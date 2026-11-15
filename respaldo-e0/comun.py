"""Funciones comunes del respaldo E0.1 (solo biblioteca estándar de Python 3.9+).

Define los formatos y las huellas para que `respaldar.py`, `verificar.py`,
`evidencia_copias.py` y `verificar_entrega.py` calculen exactamente lo mismo.
No hace red. No lee llaves.
"""
import csv
import hashlib
import io
import json
import os
import re

ESQUEMA = 1
BUCKET = "capturas"
PROYECTO_REF = "gqsadclvftkzwrihfipz"
VERSION_BANCO = "10.77"
TABLAS = ("errores_lector", "capturas_ok")
CAMPOS_MANIFIESTO = ["tipo", "nombre", "bytes", "sha256", "origen", "id_enlace", "version", "filas"]

RE_DUDOSA = re.compile(r"^(\d+)\.webp$")
RE_NORMAL_N = re.compile(r"^ok-(verde|amarillo|rojo)-.*-n(\d+)\.webp$")
RE_NORMAL = re.compile(r"^ok-(verde|amarillo|rojo)-.*\.webp$")
RE_SECRETOS = re.compile(
    r"(eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}|sb_secret_|sb_publishable_|-----BEGIN [A-Z ]*PRIVATE KEY|"
    r"AGE-SECRET-KEY-|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}|"
    r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})"
)


def sha256_bytes(b):
    return hashlib.sha256(b).hexdigest()


def sha256_archivo(ruta):
    h = hashlib.sha256()
    with open(ruta, "rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def es_webp(cabecera):
    return len(cabecera) >= 12 and cabecera[:4] == b"RIFF" and cabecera[8:12] == b"WEBP"


def clasificar(nombre):
    """Devuelve (origen, id_enlace|None). origen: dudosa | normal_n | normal_sin_n | otro."""
    m = RE_DUDOSA.match(nombre)
    if m:
        return "dudosa", int(m.group(1))
    m = RE_NORMAL_N.match(nombre)
    if m:
        return "normal_n", int(m.group(2))
    if RE_NORMAL.match(nombre):
        return "normal_sin_n", None
    return "otro", None


def _hash_lineas(lineas):
    return hashlib.sha256(("".join(l + "\n" for l in lineas)).encode("utf-8")).hexdigest()


def huella_listado(pares):
    """pares = [(nombre, bytes)]. Mismo cálculo que `referencia.sql` (huella_listado)."""
    return _hash_lineas(f"{n}\t{b}" for n, b in sorted(pares, key=lambda p: p[0].encode("utf-8")))


def huella_objetos(trios):
    """trios = [(nombre, bytes, sha256)]. Huella del contenido real de lo respaldado."""
    return _hash_lineas(f"{n}\t{b}\t{s}" for n, b, s in sorted(trios, key=lambda t: t[0].encode("utf-8")))


def huella_ids(ids):
    """Mismo cálculo que `referencia.sql`: sha256 de 'id,id,id' en orden numérico; vacío = None."""
    ids = sorted(int(i) for i in ids)
    if not ids:
        return None
    return sha256_bytes(",".join(str(i) for i in ids).encode("utf-8"))


def escribir_manifiesto(filas):
    """filas: lista de dicts con CAMPOS_MANIFIESTO. Orden estable y saltos de línea \\n."""
    filas = sorted(filas, key=lambda f: (f["tipo"], f["nombre"].encode("utf-8")))
    s = io.StringIO(newline="")
    w = csv.DictWriter(s, fieldnames=CAMPOS_MANIFIESTO, lineterminator="\n")
    w.writeheader()
    for f in filas:
        w.writerow({k: f.get(k, "") for k in CAMPOS_MANIFIESTO})
    return s.getvalue().encode("utf-8")


def leer_manifiesto(ruta):
    with open(ruta, "r", encoding="utf-8", newline="") as f:
        r = csv.DictReader(f)
        if r.fieldnames != CAMPOS_MANIFIESTO:
            raise ValueError(f"encabezado inesperado: {r.fieldnames}")
        return list(r)


def leer_ids(ruta):
    ids = []
    with open(ruta, "r", encoding="utf-8") as f:
        for n, linea in enumerate(f, 1):
            t = linea.strip()
            if not t or t.startswith("#"):
                continue
            if not t.isdigit():
                raise ValueError(f"línea {n} no es un id: {t!r}")
            ids.append(int(t))
    if len(ids) != len(set(ids)):
        raise ValueError("ids repetidos en la lista")
    return sorted(ids)


def contar_lineas_jsonl(ruta):
    """Devuelve la lista de objetos de un .jsonl (una fila por línea)."""
    filas = []
    with open(ruta, "r", encoding="utf-8") as f:
        for linea in f:
            if linea.strip():
                filas.append(json.loads(linea))
    return filas


def json_estable(obj):
    return (json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode("utf-8")


SCRIPTS = ("comun.py", "respaldar.py", "verificar.py", "evidencia_copias.py", "verificar_entrega.py", "referencia.sql",
           "ids_requeridos_10.77.txt")
ARCHIVO_HUELLAS = "SCRIPTS.sha256"


class ScriptsAlterados(Exception):
    pass


def hash_scripts(carpeta):
    """sha256 de TODOS los scripts del procedimiento. Si falta uno, error (antes se omitía en silencio: D3)."""
    res = {}
    for n in SCRIPTS:
        p = os.path.join(carpeta, n)
        if not os.path.isfile(p):
            raise ScriptsAlterados(f"falta el script {n}")
        res[n] = sha256_archivo(p)
    return res


def leer_huellas_fijadas(carpeta):
    """Lee SCRIPTS.sha256 (formato de sha256sum: «<hash>  <archivo>»). Debe cubrir exactamente SCRIPTS."""
    p = os.path.join(carpeta, ARCHIVO_HUELLAS)
    if not os.path.isfile(p):
        raise ScriptsAlterados(f"falta {ARCHIVO_HUELLAS}")
    fij = {}
    with open(p, encoding="utf-8") as f:
        for linea in f:
            t = linea.strip()
            if not t or t.startswith("#"):
                continue
            m = re.fullmatch(r"([0-9a-f]{64}) [ *]?(\S+)", t)
            if not m:
                raise ScriptsAlterados(f"línea inválida en {ARCHIVO_HUELLAS}: {t!r}")
            if m.group(2) in fij:
                raise ScriptsAlterados(f"{m.group(2)} repetido en {ARCHIVO_HUELLAS}")
            fij[m.group(2)] = m.group(1)
    if set(fij) != set(SCRIPTS):
        raise ScriptsAlterados(f"{ARCHIVO_HUELLAS} no cubre exactamente los scripts: {sorted(set(fij) ^ set(SCRIPTS))}")
    return fij


def comprobar_scripts(carpeta):
    """Devuelve (huellas_actuales, lista_de_diferencias) contra SCRIPTS.sha256. Lanza ScriptsAlterados si faltan archivos."""
    actual, fij = hash_scripts(carpeta), leer_huellas_fijadas(carpeta)
    return actual, sorted(n for n in SCRIPTS if actual[n] != fij[n])


def huellas_registradas_validas(registradas, carpeta):
    """Compara las huellas que trae un artefacto (resumen/verificación/evidencia) con las fijadas.
    Devuelve la lista de problemas (vacía = ok). Exige que el artefacto traiga todos los scripts."""
    fij = leer_huellas_fijadas(carpeta)
    if not isinstance(registradas, dict):
        return ["el artefacto no trae scripts_sha256"]
    faltan = [n for n in SCRIPTS if n not in registradas]
    dif = [n for n in SCRIPTS if n in registradas and registradas[n] != fij[n]]
    return [f"sin huella: {n}" for n in faltan] + [f"distinto del fijado: {n}" for n in dif]


def sin_trazas_de_secretos(texto):
    """True si el texto no contiene patrones de llaves, correos ni UUID (usuarios)."""
    return RE_SECRETOS.search(texto) is None
