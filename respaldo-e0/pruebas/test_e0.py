"""Pruebas del procedimiento E0.1 contra un servidor SIMULADO. Ejecutar: python3 -m unittest discover -s pruebas -v
(desde respaldo-e0/). No tocan Supabase ni usan llaves reales."""
import base64
import contextlib
import io
import json
import os
import shutil
import sys
import tempfile
import unittest

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(AQUI))
sys.path.insert(0, AQUI)
import comun  # noqa: E402
import evidencia_copias  # noqa: E402
import subprocess  # noqa: E402
import respaldar  # noqa: E402
import verificar  # noqa: E402
import verificar_entrega  # noqa: E402
from servidor_simulado import Sim, webp  # noqa: E402


def cargar(f):
    with f:
        return json.load(f)


def jwt(rol):
    b = lambda d: base64.urlsafe_b64encode(json.dumps(d).encode()).rstrip(b"=").decode()
    return f"{b({'alg':'HS256'})}.{b({'role':rol})}.firmaFALSAfirmaFALSA"


CLAVE = jwt("service_role")
REQ = list(range(101, 113))  # 12 ids requeridos de prueba


def datos_base():
    obj, er, ok = {}, [], []
    for i in REQ:
        obj[f"{i}.webp"] = webp(str(i)); er.append({"id": i, "version": "10.77", "usuario": "aaaaaaaa-0000-4000-8000-000000000001", "lineas": ["$50.00", "correo@x.com"]})
    for i in range(1000, 1130):  # 130 más: obliga a paginar (>100)
        obj[f"{i}.webp"] = webp(str(i), i % 5); er.append({"id": i, "version": "10.79", "usuario": "aaaaaaaa-0000-4000-8000-000000000002", "lineas": []})
    for i in range(1, 6):
        obj[f"ok-verde-50.00-5.0-12-1030-n{i}.webp"] = webp("n" + str(i)); ok.append({"id": i, "version": "10.80", "lineas": ["$50.00"]})
    obj["ok-rojo-20.00-9.0-30-1100.webp"] = webp("sinn")
    obj["sub/carpeta.webp"] = webp("sub")
    return obj, {"errores_lector": sorted(er, key=lambda r: r["id"]), "capturas_ok": ok}


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="e0test-")
        self.ids = os.path.join(self.tmp, "ids.txt")
        with open(self.ids, "w") as f:
            f.write("".join(f"{i}\n" for i in REQ))
        obj, tablas = datos_base()
        self.sim = Sim(CLAVE, obj, tablas)
        self.addCleanup(self.sim.cerrar)
        self.addCleanup(shutil.rmtree, self.tmp, True)
        os.environ["SUPABASE_SERVICE_ROLE_KEY"] = CLAVE

    def correr(self, *extra, salida="resp", url=None):
        err = io.StringIO()
        with contextlib.redirect_stderr(err), contextlib.redirect_stdout(io.StringIO()):
            cod = respaldar.main(["--url", url or self.sim.url, "--permitir-local", "--espera", "0.01", "--ids-requeridos", self.ids,
                                  *(["--salida", os.path.join(self.tmp, salida)] if salida else []), *extra])
        return cod, err.getvalue()

    def _refs(self, tabla_extra=None, quitar=(), tam=None):
        objs = {k: len(v) for k, v in self.sim.objetos.items() if k not in quitar}
        if tam:
            objs[tam] += 1
        ro = os.path.join(self.tmp, "ref_objetos.csv")
        open(ro, "w").write("name,bytes\n" + "".join(f"{k},{v}\n" for k, v in sorted(objs.items())))
        er = self.sim.tablas["errores_lector"]
        cols = {"objetos": len(objs), "errores_lector": len(er), "errores_lector_10_77": sum(1 for r in er if r["version"] == "10.77"),
                "capturas_ok": len(self.sim.tablas["capturas_ok"]),
                "huella_ids_errores_lector": comun.huella_ids(r["id"] for r in er),
                "huella_ids_errores_lector_10_77": comun.huella_ids(r["id"] for r in er if r["version"] == "10.77"),
                "huella_ids_capturas_ok": comun.huella_ids(r["id"] for r in self.sim.tablas["capturas_ok"]),
                "huella_listado": comun.huella_listado(list(objs.items())), "consultado_utc": "2026-11-15T12:00:00Z"}
        cols.update(tabla_extra or {})
        rt = os.path.join(self.tmp, "ref_tablas.csv")
        open(rt, "w").write(",".join(cols) + "\n" + ",".join("" if v is None else str(v) for v in cols.values()) + "\n")
        return ro, rt

    def dir(self, n="resp"):
        return os.path.join(self.tmp, n)

    def verif(self, n="resp", **k):
        return verificar.verificar(self.dir(n), **k)


class TestRespaldo(Base):
    def test_camino_feliz(self):
        cod, err = self.correr()
        self.assertEqual(cod, 0, err)
        res = cargar(open(os.path.join(self.dir(), "resumen.json")))
        self.assertEqual(res["resultado_respaldo"], "COMPLETO")
        self.assertEqual(res["capturas"]["descargadas"], 12 + 130 + 5 + 1 + 1)  # incluye la de la subcarpeta
        self.assertEqual(res["requeridos"]["presentes"], 12)
        self.assertEqual(res["tablas"]["errores_lector"]["filas"], 142)
        self.assertTrue(os.path.isfile(os.path.join(self.dir(), "capturas", "sub", "carpeta.webp")))
        v = self.verif()
        self.assertEqual(v["resultado"], "APROBADO_CON_ADVERTENCIAS", [c for c in v["checks"] if c["estado"] != "ok"])
        self.assertEqual({c["id"] for c in v["checks"] if c["estado"] == "advertencia"}, {"R00"})

    def test_solo_lectura_y_rutas_permitidas(self):
        self.correr()
        for metodo, ruta in self.sim.log:
            self.assertIn(metodo, ("GET", "POST"))
            self.assertTrue(any(ruta.startswith(p) for _, p in respaldar.PERMITIDO), ruta)
            if metodo == "POST":
                self.assertTrue(ruta.startswith("/storage/v1/object/list/"))
        with open(os.path.join(os.path.dirname(AQUI), "respaldar.py")) as f:
            fuente = f.read()
        for prohibido in ('"DELETE"', '"PUT"', '"PATCH"', "'DELETE'", "'PUT'"):
            self.assertNotIn(prohibido, fuente)

    def test_la_llave_no_aparece_en_ninguna_salida(self):
        _, err = self.correr()
        self.assertNotIn(CLAVE, err)
        for raiz, _, archivos in os.walk(self.dir()):
            for a in archivos:
                with open(os.path.join(raiz, a), "rb") as f:
                    self.assertNotIn(CLAVE.encode(), f.read())

    def test_paginacion_y_carpetas(self):
        self.sim.tope = 7  # el servidor devuelve menos de lo pedido
        cod, err = self.correr()
        self.assertEqual(cod, 0, err)
        self.assertEqual(self.verif()["conteos"]["capturas"], 149)

    def test_reintenta_ante_500(self):
        self.sim.fallar_una_vez.add("/storage/v1/object/authenticated/capturas/101.webp")
        cod, err = self.correr()
        self.assertEqual(cod, 0, err)

    def test_requerido_ausente_es_incompleto(self):
        del self.sim.objetos["105.webp"]
        cod, err = self.correr()
        self.assertEqual(cod, 2)
        res = cargar(open(os.path.join(self.dir(), "resumen.json")))
        self.assertEqual(res["resultado_respaldo"], "INCOMPLETO")
        self.assertEqual(res["requeridos"]["faltantes"], [105])
        self.assertEqual(self.verif()["resultado"], "FALLO")

    def test_requerido_que_desaparece_durante_la_descarga(self):
        self.sim.quitar_tras_listar.add("107.webp")
        cod, _ = self.correr()
        self.assertEqual(cod, 2)
        res = cargar(open(os.path.join(self.dir(), "resumen.json")))
        self.assertIn("107.webp", res["capturas"]["desaparecidas_antes_de_descargar"])
        self.assertIn("107.webp", res["listado"]["quitados_durante"])

    def test_objeto_inexistente_con_http_400_como_hace_supabase(self):
        self.sim.codigo_no_existe = 400
        self.sim.quitar_tras_listar.add("1050.webp")
        cod, _ = self.correr()
        self.assertEqual(cod, 0)
        res = cargar(open(os.path.join(self.dir(), "resumen.json")))
        self.assertEqual(res["capturas"]["desaparecidas_antes_de_descargar"], ["1050.webp"])

    def test_etag_md5_coincide_o_se_avisa(self):
        import hashlib
        for k, v in self.sim.objetos.items():
            self.sim.etags[k] = hashlib.md5(v).hexdigest()
        self.sim.etags["104.webp"] = "0" * 32  # el servidor dice otra cosa
        self.assertEqual(self.correr()[0], 0)
        res = cargar(open(os.path.join(self.dir(), "resumen.json")))
        self.assertEqual(res["capturas"]["etag_md5"]["discrepancias"], ["104.webp"])
        self.assertEqual(res["capturas"]["etag_md5"]["comprobados"], 148)
        self.assertIn("V10c", {c["id"] for c in self.verif()["checks"] if c["estado"] == "advertencia"})

    def test_no_requerido_que_desaparece_es_advertencia(self):
        self.sim.quitar_tras_listar.add("1050.webp")
        cod, _ = self.correr()
        self.assertEqual(cod, 0)
        v = self.verif()
        self.assertEqual(v["resultado"], "APROBADO_CON_ADVERTENCIAS")
        self.assertIn("V10", {c["id"] for c in v["checks"] if c["estado"] == "advertencia"})

    def test_descarga_truncada_aborta_y_no_deja_resumen_falso(self):
        self.sim.truncar.add("1001.webp")
        cod, err = self.correr()
        self.assertEqual(cod, 1)
        self.assertIn("Sin respuesta tras 4 intentos", err)
        self.assertFalse(os.path.exists(os.path.join(self.dir(), "capturas", "1001.webp")))
        self.assertFalse(os.path.exists(os.path.join(self.dir(), "resumen.json")))  # no hay un resumen que parezca completo

    def test_html_con_200_se_rechaza(self):
        self.sim.html.add("102.webp")
        cod, _ = self.correr()
        self.assertEqual(cod, 2)
        res = cargar(open(os.path.join(self.dir(), "resumen.json")))
        self.assertEqual(res["capturas"]["fallidas"][0]["nombre"], "102.webp")
        self.assertFalse(os.path.exists(os.path.join(self.dir(), "capturas", "102.webp")))

    def test_redireccion_no_reenvia_la_llave(self):
        otro = Sim("otra", {}, {})
        self.addCleanup(otro.cerrar)
        self.sim.redirigir["103.webp"] = otro.url + "/robo"
        cod, err = self.correr()
        self.assertEqual(otro.log, [])  # nunca se llamó al otro host
        self.assertEqual(cod, 1)
        self.assertIn("HTTP 302", err)

    def test_rechaza_url_ajena_clave_anon_y_sin_clave(self):
        for url in ("https://ejemplo.supabase.co", "https://evil.com", "http://gqsadclvftkzwrihfipz.supabase.co"):
            err = io.StringIO()
            with contextlib.redirect_stderr(err):
                cod = respaldar.main(["--url", url, "--salida", self.dir("x"), "--ids-requeridos", self.ids])
            self.assertEqual(cod, 1, url)
            self.assertFalse(os.path.exists(self.dir("x")))
        os.environ["SUPABASE_SERVICE_ROLE_KEY"] = jwt("anon")
        self.assertEqual(self.correr()[0], 1)
        os.environ["SUPABASE_SERVICE_ROLE_KEY"] = ""
        self.assertEqual(self.correr()[0], 1)
        self.assertEqual(self.sim.log, [])

    def test_clave_incorrecta_401(self):
        os.environ["SUPABASE_SERVICE_ROLE_KEY"] = jwt("service_role") + "x"
        cod, err = self.correr()
        self.assertEqual(cod, 1)
        self.assertNotIn(os.environ["SUPABASE_SERVICE_ROLE_KEY"], err)

    def test_salida_dentro_de_un_repo_o_no_vacia_se_rechaza(self):
        repo = os.path.join(self.tmp, "repo")
        os.makedirs(os.path.join(repo, ".git"))
        e = io.StringIO()
        with contextlib.redirect_stderr(e):
            c1 = respaldar.main(["--url", self.sim.url, "--permitir-local", "--salida", os.path.join(repo, "r"), "--ids-requeridos", self.ids])
        self.assertEqual(c1, 1)
        self.assertIn("repositorio", e.getvalue())
        os.makedirs(self.dir("llena"))
        open(os.path.join(self.dir("llena"), "a"), "w").close()
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(respaldar.main(["--url", self.sim.url, "--permitir-local", "--salida", self.dir("llena"), "--ids-requeridos", self.ids]), 1)

    def test_solo_listar_no_escribe_nada(self):
        cod, _ = self.correr("--solo-listar", salida=None)
        self.assertEqual(cod, 0)
        self.assertEqual(os.listdir(self.tmp).count("resp"), 0)

    def test_nombre_de_objeto_con_ruta_peligrosa(self):
        self.sim.objetos["../fuera.webp"] = webp("x")
        cod, err = self.correr()
        self.assertEqual(cod, 1)
        self.assertFalse(os.path.exists(os.path.join(self.tmp, "fuera.webp")))


class TestMutaciones(Base):
    """Cada alteración del respaldo debe hacer fallar verificar.py."""

    def setUp(self):
        super().setUp()
        self.assertEqual(self.correr()[0], 0)

    def assertFalla(self, id_):
        v = self.verif()
        self.assertEqual(v["resultado"], "FALLO")
        self.assertIn(id_, {c["id"] for c in v["checks"] if c["estado"] == "fallo"})

    def test_byte_alterado(self):
        p = os.path.join(self.dir(), "capturas", "104.webp")
        d = bytearray(open(p, "rb").read()); d[-1] ^= 1; open(p, "wb").write(d)
        self.assertFalla("V03")

    def test_archivo_borrado(self):
        os.remove(os.path.join(self.dir(), "capturas", "104.webp"))
        self.assertFalla("V03")

    def test_archivo_extra(self):
        open(os.path.join(self.dir(), "capturas", "999.webp"), "wb").write(b"RIFFxxxxWEBP")
        self.assertFalla("V04")

    def test_contenido_reemplazado_por_html(self):
        p = os.path.join(self.dir(), "capturas", "104.webp")
        open(p, "wb").write(b"<html>")
        self.assertFalla("V03")

    def test_manifiesto_editado(self):
        p = os.path.join(self.dir(), "manifiesto.csv")
        t = open(p).read()
        self.assertIn("capturas/104.webp", t)
        open(p, "w").write(t.replace("capturas/104.webp", "capturas/104.webp ", 1))
        self.assertFalla("V01")

    def test_tabla_alterada(self):
        p = os.path.join(self.dir(), "tablas", "errores_lector.jsonl")
        open(p, "a").write('{"id": 5000, "version": "10.77"}\n')
        self.assertFalla("V03")

    def test_lista_de_ids_alterada(self):
        p = os.path.join(self.dir(), "ids_requeridos.txt")
        open(p, "a").write("113\n")
        self.assertFalla("V03")

    def test_resumen_con_huella_cambiada(self):
        p = os.path.join(self.dir(), "resumen.json")
        r = cargar(open(p)); r["capturas"]["huella_objetos"] = "0" * 64; json.dump(r, open(p, "w"))
        v = self.verif()
        self.assertEqual(v["resultado"], "FALLO")

    def test_version_requerida_cambiada_en_la_tabla_y_todo_reajustado(self):
        """Un atacante coherente (reescribe tabla, manifiesto y resumen) aún falla por la versión 10.77."""
        d = self.dir()
        p = os.path.join(d, "tablas", "errores_lector.jsonl")
        filas = [json.loads(l) for l in open(p)]
        for f in filas:
            if f["id"] == 101:
                f["version"] = "10.99"
        datos = "".join(json.dumps(f, ensure_ascii=False, sort_keys=True) + "\n" for f in filas).encode()
        open(p, "wb").write(datos)
        m = comun.leer_manifiesto(os.path.join(d, "manifiesto.csv"))
        for r in m:
            if r["nombre"] == "tablas/errores_lector.jsonl":
                r["sha256"] = comun.sha256_bytes(datos)
        open(os.path.join(d, "manifiesto.csv"), "wb").write(comun.escribir_manifiesto(m))
        res = cargar(open(os.path.join(d, "resumen.json")))
        res["manifiesto_sha256"] = comun.sha256_archivo(os.path.join(d, "manifiesto.csv"))
        res["tablas"]["errores_lector"]["sha256_jsonl"] = comun.sha256_bytes(datos)
        json.dump(res, open(os.path.join(d, "resumen.json"), "w"))
        self.assertFalla("V08c")


class TestReferencia(Base):
    def test_referencia_coincide(self):
        self.correr()
        ro, rt = self._refs()
        v = self.verif(ref_objetos=ro, ref_tablas=rt)
        self.assertEqual(v["resultado"], "APROBADO", [c for c in v["checks"] if c["estado"] != "ok"])

    def test_objeto_de_la_referencia_ausente_en_el_respaldo(self):
        self.correr()
        ro, rt = self._refs()
        open(ro, "a").write("999999.webp,100\n")
        v = self.verif(ref_objetos=ro, ref_tablas=rt)
        self.assertEqual(v["resultado"], "FALLO")
        self.assertIn("R01", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})

    def test_tamano_distinto_y_huella_distinta(self):
        self.correr()
        ro, rt = self._refs(tam="104.webp")
        v = self.verif(ref_objetos=ro, ref_tablas=rt)
        self.assertIn("R02", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})

    def test_mas_filas_en_referencia_que_en_respaldo_falla(self):
        self.correr()
        ro, rt = self._refs({"errores_lector": 143})
        v = self.verif(ref_objetos=ro, ref_tablas=rt)
        self.assertIn("R05-errores_lector", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})

    def test_huella_de_ids_distinta_con_mismo_conteo_falla(self):
        self.correr()
        ro, rt = self._refs({"huella_ids_errores_lector_10_77": "0" * 64})
        v = self.verif(ref_objetos=ro, ref_tablas=rt)
        self.assertIn("R08", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})

    def test_actividad_posterior_es_advertencia_no_fallo(self):
        self.correr()
        ro, rt = self._refs()
        self.sim.objetos["nuevo.webp"] = webp("n")
        ro2, rt2 = self._refs()
        v = self.verif(ref_objetos=ro2, ref_tablas=rt2)
        self.assertIn("R01", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})  # existía en la referencia y no en el respaldo


class TestEntrega(Base):
    def test_artefactos_sanitizados_contienen_solo_lo_permitido(self):
        self.correr()
        v = self.verif()
        open(os.path.join(self.dir(), "VERIFICACION.json"), "wb").write(comun.json_estable(v))
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            texto = open(os.path.join(self.dir(), n), encoding="utf-8").read()
            self.assertTrue(comun.sin_trazas_de_secretos(texto), n)
        # y el respaldo completo SÍ contiene datos personales (por eso no se comparte)
        self.assertFalse(comun.sin_trazas_de_secretos(open(os.path.join(self.dir(), "tablas", "errores_lector.jsonl")).read()))

    def test_entrega_sin_referencia_no_cierra(self):
        self.correr()
        d = self.dir()
        open(os.path.join(d, "VERIFICACION.json"), "wb").write(comun.json_estable(self.verif()))
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(d, n), entrega)
        r = verificar_entrega.verificar_entrega(entrega, self.ids)
        self.assertEqual([c["id"] for c in r["checks"] if c["estado"] == "fallo"], ["E10"])

    def test_verificar_entrega_acepta_y_detecta_manipulacion(self):
        self.correr()
        d = self.dir()
        ro, rt = self._refs()
        v = self.verif(ref_objetos=ro, ref_tablas=rt)
        open(os.path.join(d, "VERIFICACION.json"), "wb").write(comun.json_estable(v))
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(d, n), entrega)
        r = verificar_entrega.verificar_entrega(entrega, self.ids)
        self.assertFalse([c for c in r["checks"] if c["estado"] == "fallo"], r["checks"])
        # manipulación: borrar una fila requerida del manifiesto
        p = os.path.join(entrega, "manifiesto.csv")
        lineas = open(p).read().split("\n")
        open(p, "w").write("\n".join(l for l in lineas if "capturas/105.webp" not in l))
        r2 = verificar_entrega.verificar_entrega(entrega, self.ids)
        self.assertIn("E02", {c["id"] for c in r2["checks"] if c["estado"] == "fallo"})

    def test_lista_de_ids_del_repo_con_un_id_que_el_respaldo_no_tiene(self):
        self.correr()
        d = self.dir()
        ro, rt = self._refs()
        with open(os.path.join(d, "VERIFICACION.json"), "wb") as f:
            f.write(comun.json_estable(self.verif(ref_objetos=ro, ref_tablas=rt)))
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(d, n), entrega)
        mas = os.path.join(self.tmp, "ids_mas.txt")
        with open(mas, "w") as f:
            f.write("".join(f"{i}\n" for i in REQ + [113]))
        r = verificar_entrega.verificar_entrega(entrega, mas)
        self.assertEqual({c["id"] for c in r["checks"] if c["estado"] == "fallo"}, {"E06a", "E06b"})

    def test_minimo_de_objetos(self):
        self.correr()
        d = self.dir()
        ro, rt = self._refs()
        with open(os.path.join(d, "VERIFICACION.json"), "wb") as f:
            f.write(comun.json_estable(self.verif(ref_objetos=ro, ref_tablas=rt)))
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(d, n), entrega)
        r = verificar_entrega.verificar_entrega(entrega, self.ids, minimo_objetos=738)
        self.assertEqual({c["id"] for c in r["checks"] if c["estado"] == "fallo"}, {"E05b"})


class MontajeCopias(Base):
    """Montaje común: ciclo completo con age (clave) y gpg (frase en archivo); todo con datos simulados."""

    def setUp(self):
        super().setUp()
        self.assertEqual(self.correr()[0], 0)
        self.ro, self.rt = self._refs()
        with open(os.path.join(self.dir(), "VERIFICACION.json"), "wb") as f:
            f.write(comun.json_estable(self.verif(ref_objetos=self.ro, ref_tablas=self.rt)))
        self.tar = os.path.join(self.tmp, "r.tar")
        self.ev = lambda *a: evidencia_copias.main(list(a))
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(self.ev("empaquetar", "--dir", self.dir(), "--salida", self.tar), 0)
        self.emp = self.tar + ".empaquetado.json"
        self.ident = os.path.join(self.tmp, "id.txt")
        out = subprocess.run(["age-keygen", "-o", self.ident], capture_output=True, text=True).stderr
        self.pub = [l for l in out.splitlines() if l.startswith("Public key")][0].split(": ")[1]

    def cifrar(self, destino):
        subprocess.run(["age", "-r", self.pub, "-o", destino, self.tar], check=True)
        return destino

    def comprobar(self, copia, et="A", emp=None):
        salida = os.path.join(self.tmp, f"COMPROBACION-{et}.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("comprobar", "--copia", copia, "--etiqueta", et, "--empaquetado", emp or self.emp, "--identidad", self.ident, "--salida", salida)
        return cod, salida

    def restaurar(self, copia, et="B", destino=None, emp=None):
        destino = destino or os.path.join(self.tmp, "rest" + et)
        salida = os.path.join(self.tmp, f"RESTAURACION-{et}.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("restaurar", "--copia", copia, "--etiqueta", et, "--destino", destino, "--empaquetado", emp or self.emp,
                          "--identidad", self.ident, "--ref-objetos", self.ro, "--ref-tablas", self.rt, "--salida", salida)
        return cod, salida

class TestCopias(MontajeCopias):
    def test_tar_determinista(self):
        otro = os.path.join(self.tmp, "r2.tar")
        with contextlib.redirect_stderr(io.StringIO()):
            self.ev("empaquetar", "--dir", self.dir(), "--salida", otro)
        self.assertEqual(comun.sha256_archivo(self.tar), comun.sha256_archivo(otro))

    def test_ciclo_completo_y_revision_de_entrega(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        self.assertNotEqual(comun.sha256_archivo(a), comun.sha256_archivo(b))  # cifrados por separado
        cod, rb = self.restaurar(b)
        self.assertEqual(cod, 0)
        cod, ca = self.comprobar(a)
        self.assertEqual(cod, 0)
        ev_json = os.path.join(self.tmp, "entrega", "EVIDENCIA-COPIAS.json")
        os.makedirs(os.path.dirname(ev_json))
        with contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "disco USB casa", "--medio-b", "nube cuenta aparte",
                                     "--restauracion-b", rb, "--comprobacion-a", ca, "--confirmo-conservar", "--salida", ev_json), 0)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(self.dir(), n), os.path.dirname(ev_json))
        r = verificar_entrega.verificar_entrega(os.path.dirname(ev_json), self.ids)
        self.assertEqual([c for c in r["checks"] if c["estado"] == "fallo"], [])
        self.assertEqual(r["resultado"], "APROBADO")
        self.assertIn("C05", {c["id"] for c in r["checks"]})

    def test_sin_evidencia_queda_pendiente(self):
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(self.dir(), n), entrega)
        r = verificar_entrega.verificar_entrega(entrega, self.ids)
        self.assertEqual(r["resultado"], "APROBADO_CON_ADVERTENCIAS")
        self.assertIn("C00", {c["id"] for c in r["checks"] if c["estado"] == "advertencia"})

    def test_copia_dañada_no_restaura(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        d = bytearray(open(b, "rb").read()); d[len(d) // 2] ^= 0xFF; open(b, "wb").write(d)
        with self.assertRaises(SystemExit):
            self.restaurar(b)

    def test_tar_distinto_al_empaquetado_se_detecta(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        info = json.load(open(self.emp)); info["tar_sha256"] = "0" * 64
        otro = os.path.join(self.tmp, "falso.json"); json.dump(info, open(otro, "w"))
        cod, rb = self.restaurar(b, emp=otro)
        self.assertEqual(cod, 1)
        self.assertFalse(json.load(open(rb))["tar_coincide"])

    def test_archivo_sin_cifrar_se_rechaza(self):
        with self.assertRaises(SystemExit):
            self.restaurar(self.tar)
        self.assertEqual(evidencia_copias.formato_cifrado(self.tar), "desconocido")

    def test_registrar_incompleta_si_medios_iguales_o_sin_confirmar(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b)
        for extra, medio_b in (([], "disco USB casa"), (["--confirmo-conservar"], "disco USB casa"), ([], "otro")):
            with contextlib.redirect_stderr(io.StringIO()):
                cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "disco USB casa", "--medio-b", medio_b,
                              "--restauracion-b", rb, "--salida", os.path.join(self.tmp, "ev.json"), *extra)
            self.assertEqual(cod, 1)

    def test_restauracion_de_otra_copia_no_vale_para_b(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, ra = self.restaurar(a, "A")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "x", "--medio-b", "y", "--restauracion-b", ra,
                          "--confirmo-conservar", "--salida", os.path.join(self.tmp, "ev.json"))
        self.assertEqual(cod, 1)

    def test_restauracion_etiquetada_B_pero_hecha_con_otro_archivo(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, r = self.restaurar(a, "B")  # se restauró el archivo A y se etiquetó como B
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "x", "--medio-b", "y", "--restauracion-b", r,
                          "--confirmo-conservar", "--salida", os.path.join(self.tmp, "ev.json"))
        self.assertEqual(cod, 1)
        self.assertFalse(json.load(open(os.path.join(self.tmp, "ev.json")))["restauracion_b_corresponde_a_copia_b"])

    def test_gpg_con_frase_en_archivo(self):
        frase = os.path.join(self.tmp, "frase.txt"); open(frase, "w").write("frase-de-prueba\n")
        gdir = os.path.join(self.tmp, "gnupg"); os.makedirs(gdir, mode=0o700)
        env = dict(os.environ, GNUPGHOME=gdir)
        b = os.path.join(self.tmp, "B.gpg")
        subprocess.run(["gpg", "--batch", "--quiet", "--pinentry-mode", "loopback", "--passphrase-file", frase, "-c", "-o", b, self.tar], check=True, env=env)
        self.assertEqual(evidencia_copias.formato_cifrado(b), "gpg")
        os.environ["GNUPGHOME"] = gdir
        self.addCleanup(os.environ.pop, "GNUPGHOME", None)
        salida = os.path.join(self.tmp, "RB.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("restaurar", "--copia", b, "--etiqueta", "B", "--destino", os.path.join(self.tmp, "rg"), "--empaquetado", self.emp,
                          "--frase-archivo", frase, "--salida", salida)
        self.assertEqual(cod, 0)

    def test_tar_malicioso_no_escribe_fuera(self):
        import tarfile
        mal = os.path.join(self.tmp, "mal.tar")
        with tarfile.open(mal, "w") as t:
            ti = tarfile.TarInfo("../../escapa.txt"); ti.size = 1; t.addfile(ti, io.BytesIO(b"x"))
        with self.assertRaises(SystemExit):
            evidencia_copias._extraer_seguro(mal, os.path.join(self.tmp, "dest"))
        self.assertFalse(os.path.exists(os.path.join(self.tmp, "escapa.txt")))


class TestDefectosE01(MontajeCopias):
    """Regresiones de los defectos D1–D5 detectados en la revisión de E0.1."""

    def _registrar(self, a, b, rb, salida=None, con_a=True):
        salida = salida or os.path.join(self.tmp, "ev.json")
        extra = []
        if con_a:
            try:
                extra = ["--comprobacion-a", self.comprobar(a)[1]]
            except SystemExit:
                extra = []
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "disco USB casa", "--medio-b", "nube cuenta aparte",
                          "--restauracion-b", rb, "--confirmo-conservar", "--salida", salida, *extra)
        return cod, json.load(open(salida))

    # D1 ------------------------------------------------------------------
    def test_d1_misma_copia_copiada_byte_a_byte_es_incompleta(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        a = os.path.join(self.tmp, "A.age"); shutil.copy(b, a)  # mismo cifrado copiado: no son dos cifrados
        _, rb = self.restaurar(b)
        cod, ev = self._registrar(a, b, rb)
        self.assertEqual(cod, 1)
        self.assertFalse(ev["cifrados_distintos"])
        self.assertEqual(ev["resultado"], "INCOMPLETA")

    def test_d1_mismo_archivo_dos_veces_es_incompleta(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b)
        cod, ev = self._registrar(b, b, rb)
        self.assertEqual(cod, 1)
        self.assertFalse(ev["archivos_distintos"])
        self.assertEqual(ev["resultado"], "INCOMPLETA")

    def test_d1_enlace_duro_no_cuenta_como_otra_copia(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        a = os.path.join(self.tmp, "A.age"); os.link(b, a)
        _, rb = self.restaurar(b)
        cod, ev = self._registrar(a, b, rb)
        self.assertEqual(cod, 1)
        self.assertFalse(ev["archivos_distintos"])

    def test_d1_verificar_entrega_falla_si_la_evidencia_dice_completa_con_cifrados_iguales(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b)
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        cod, ev = self._registrar(a, b, rb, os.path.join(entrega, "EVIDENCIA-COPIAS.json"))
        self.assertEqual(cod, 0)
        for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(self.dir(), n), entrega)
        ev["copias"]["A"]["sha256"] = ev["copias"]["B"]["sha256"]  # manipulada a mano para que diga COMPLETA
        ev["cifrados_distintos"] = False
        open(os.path.join(entrega, "EVIDENCIA-COPIAS.json"), "wb").write(comun.json_estable(ev))
        r = verificar_entrega.verificar_entrega(entrega, self.ids)
        self.assertEqual(r["resultado"], "FALLO")
        self.assertIn("C08", {c["id"] for c in r["checks"] if c["estado"] == "fallo"})

    # D2 ------------------------------------------------------------------
    def test_d2_requerido_sin_fila_en_errores_lector_es_incompleto(self):
        self.sim.tablas["errores_lector"] = [r for r in self.sim.tablas["errores_lector"] if r["id"] != REQ[0]]
        cod, err = self.correr(salida="d2a")
        self.assertEqual(cod, 2)
        res = json.load(open(os.path.join(self.dir("d2a"), "resumen.json")))
        self.assertEqual(res["resultado_respaldo"], "INCOMPLETO")
        self.assertEqual(res["requeridos"]["sin_fila_en_errores_lector"], [REQ[0]])
        self.assertTrue(any("sin fila" in m for m in res["motivos_incompleto"]))

    def test_d2_requerido_con_version_distinta_es_incompleto(self):
        for r in self.sim.tablas["errores_lector"]:
            if r["id"] == REQ[1]:
                r["version"] = "10.79"
        cod, _ = self.correr(salida="d2b")
        self.assertEqual(cod, 2)
        res = json.load(open(os.path.join(self.dir("d2b"), "resumen.json")))
        self.assertEqual(res["resultado_respaldo"], "INCOMPLETO")
        self.assertEqual(res["requeridos"]["version_distinta_de_10_77"], [REQ[1]])
        self.assertEqual(verificar.verificar(self.dir("d2b"))["resultado"], "FALLO")

    # D3 ------------------------------------------------------------------
    def _copia_scripts(self):
        d = os.path.join(self.tmp, "scripts")
        shutil.copytree(os.path.dirname(AQUI), d, ignore=shutil.ignore_patterns("__pycache__", "pruebas"))
        return d

    def test_d3_huellas_fijadas_coinciden_con_el_repo(self):
        actual, dif = comun.comprobar_scripts(os.path.dirname(AQUI))
        self.assertEqual(dif, [])
        self.assertEqual(set(actual), set(comun.SCRIPTS))

    def test_d3_script_alterado_se_detecta(self):
        d = self._copia_scripts()
        with open(os.path.join(d, "verificar.py"), "a") as f:
            f.write("\n# alterado\n")
        self.assertEqual(comun.comprobar_scripts(d)[1], ["verificar.py"])

    def test_d3_script_faltante_es_error_no_se_omite(self):
        d = self._copia_scripts()
        os.remove(os.path.join(d, "referencia.sql"))
        with self.assertRaises(comun.ScriptsAlterados):
            comun.hash_scripts(d)

    def test_d3_huellas_fijadas_incompletas_o_mal_formadas(self):
        d = self._copia_scripts()
        p = os.path.join(d, comun.ARCHIVO_HUELLAS)
        lineas = open(p).read().splitlines()
        open(p, "w").write("\n".join(lineas[1:]) + "\n")
        with self.assertRaises(comun.ScriptsAlterados):
            comun.leer_huellas_fijadas(d)
        open(p, "w").write("no-es-hash  comun.py\n")
        with self.assertRaises(comun.ScriptsAlterados):
            comun.leer_huellas_fijadas(d)

    def test_d3_respaldar_no_corre_con_scripts_alterados(self):
        d = self._copia_scripts()
        with open(os.path.join(d, "respaldar.py"), "a") as f:
            f.write("\n# alterado\n")
        env = dict(os.environ, SUPABASE_SERVICE_ROLE_KEY=CLAVE)
        n0 = len(self.sim.log)
        p = subprocess.run([sys.executable, os.path.join(d, "respaldar.py"), "--url", self.sim.url, "--permitir-local",
                            "--solo-listar", "--ids-requeridos", self.ids], capture_output=True, text=True, env=env)
        self.assertEqual(p.returncode, 1)
        self.assertIn("Integridad de scripts", p.stderr)
        self.assertEqual(len(self.sim.log), n0)  # no llegó a hacer ninguna petición

    def test_d3_entrega_con_huellas_de_scripts_distintas_falla(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b)
        entrega = os.path.join(self.tmp, "entrega"); os.makedirs(entrega)
        self._registrar(a, b, rb, os.path.join(entrega, "EVIDENCIA-COPIAS.json"))
        for n in ("manifiesto.csv", "VERIFICACION.json"):
            shutil.copy(os.path.join(self.dir(), n), entrega)
        res = json.load(open(os.path.join(self.dir(), "resumen.json")))
        res["scripts_sha256"].pop("verificar.py")  # artefacto sin una huella: antes se ignoraba
        open(os.path.join(entrega, "resumen.json"), "wb").write(comun.json_estable(res))
        r = verificar_entrega.verificar_entrega(entrega, self.ids)
        self.assertIn("E12b", {c["id"] for c in r["checks"] if c["estado"] == "fallo"})

    def test_d3_verificar_falla_si_el_resumen_trae_otra_huella(self):
        res_p = os.path.join(self.dir(), "resumen.json")
        res = json.load(open(res_p))
        res["scripts_sha256"]["respaldar.py"] = "0" * 64
        crudo = comun.json_estable(res)
        open(res_p, "wb").write(crudo)
        v = self.verif()
        self.assertIn("V11b", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})

    # D4 ------------------------------------------------------------------
    def test_d4_readme_declara_el_numero_real_de_pruebas(self):
        import re as _re
        total = unittest.defaultTestLoader.discover(AQUI).countTestCases()
        txt = open(os.path.join(os.path.dirname(AQUI), "README.md"), encoding="utf-8").read()
        m = _re.search(r"— (\d+) pruebas contra un servidor", txt)
        self.assertIsNotNone(m)
        self.assertEqual(int(m.group(1)), total)

    # D5 ------------------------------------------------------------------
    def test_d5_revisor_de_mensajes_detecta_url_de_sesion(self):
        h = os.path.join(os.path.dirname(os.path.dirname(AQUI)), "herramientas")
        if not os.path.isfile(os.path.join(h, "revisar_mensajes.py")):
            self.skipTest("copia aislada sin herramientas/ (p. ej. en pruebas de mutación)")
        sys.path.insert(0, h)
        import revisar_mensajes
        self.assertTrue(revisar_mensajes.hallazgos("x\nClaude-Session: https://claude.ai/code/session_01ABCdef\n"))
        self.assertTrue(revisar_mensajes.hallazgos("token eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoic2VydmljZV9yb2xlIn0.x"))
        self.assertEqual(revisar_mensajes.hallazgos("E0: corrige D1\n\nCo-Authored-By: Claude <noreply@anthropic.com>\n"), [])


class TestAuditoriaCierre(MontajeCopias):
    """Regresiones de los hallazgos de la auditoría de cierre (H1–H8)."""

    def _manifiesto_con(self, fila, quitar=None):
        """Agrega una fila al manifiesto y reajusta el sha256 en resumen.json (como haría quien lo manipula)."""
        man_p, res_p = os.path.join(self.dir(), "manifiesto.csv"), os.path.join(self.dir(), "resumen.json")
        filas = comun.leer_manifiesto(man_p) + [fila]
        datos = comun.escribir_manifiesto(filas)
        open(man_p, "wb").write(datos)
        res = json.load(open(res_p)); res["manifiesto_sha256"] = comun.sha256_bytes(datos)
        open(res_p, "wb").write(comun.json_estable(res))

    def _secreto_fuera(self):
        p = os.path.join(self.tmp, "secreto.txt")
        open(p, "wb").write(b"NO-DEBE-SALIR")
        return p

    # H1: verificar no lee nada fuera del respaldo
    def test_h1_manifiesto_con_ruta_fuera_no_se_lee(self):
        fuera = self._secreto_fuera()
        for nombre in ("../secreto.txt", "capturas/../../secreto.txt", "/etc/hostname", "capturas\\..\\x.webp", "capturas/./x.webp", "capturas//x.webp", "C:/x.webp", "capturas/nul.webp"):
            with self.subTest(nombre):
                shutil.copy(os.path.join(self.dir(), "manifiesto.csv"), os.path.join(self.tmp, "man.bak"))
                shutil.copy(os.path.join(self.dir(), "resumen.json"), os.path.join(self.tmp, "res.bak"))
                self._manifiesto_con({"tipo": "captura", "nombre": nombre, "bytes": os.path.getsize(fuera), "sha256": comun.sha256_archivo(fuera), "origen": "otro"})
                v = self.verif()
                self.assertEqual(v["resultado"], "FALLO")
                self.assertIn("V02b", {c["id"] for c in v["checks"] if c["estado"] == "fallo"})
                self.assertNotIn("V03", {c["id"] for c in v["checks"]})  # se detuvo antes de leer archivos
                shutil.copy(os.path.join(self.tmp, "man.bak"), os.path.join(self.dir(), "manifiesto.csv"))
                shutil.copy(os.path.join(self.tmp, "res.bak"), os.path.join(self.dir(), "resumen.json"))

    def test_h1_enlace_simbolico_dentro_del_respaldo_se_rechaza(self):
        fuera = self._secreto_fuera()
        os.symlink(fuera, os.path.join(self.dir(), "capturas", "enlace.webp"))
        self._manifiesto_con({"tipo": "captura", "nombre": "capturas/enlace.webp", "bytes": os.path.getsize(fuera), "sha256": comun.sha256_archivo(fuera), "origen": "otro"})
        self.assertIn("V02b", {c["id"] for c in self.verif()["checks"] if c["estado"] == "fallo"})

    def test_h1_enlace_a_otro_archivo_del_mismo_respaldo_tambien_se_rechaza(self):
        os.symlink(os.path.join(self.dir(), "capturas", "101.webp"), os.path.join(self.dir(), "capturas", "dentro.webp"))
        with self.assertRaises(comun.RutaInsegura):
            comun.ruta_en(self.dir(), "capturas/dentro.webp", "captura")
        os.makedirs(os.path.join(self.tmp, "otra"))
        os.symlink(os.path.join(self.tmp, "otra"), os.path.join(self.dir(), "capturas", "dir"))  # carpeta enlazada
        with self.assertRaises(comun.RutaInsegura):
            comun.ruta_en(self.dir(), "capturas/dir/x.webp", "captura")
        self.assertTrue(comun.ruta_en(self.dir(), "capturas/101.webp", "captura").endswith("101.webp"))

    def test_h1_tipo_y_ubicacion_coherentes(self):
        for tipo, nombre in (("tabla", "tablas/otra.jsonl"), ("captura", "tablas/errores_lector.jsonl2"), ("lista", "otra.txt"), ("raro", "capturas/x.webp")):
            with self.assertRaises(comun.RutaInsegura):
                comun.nombre_seguro(nombre, tipo)
        self.assertEqual(comun.nombre_seguro("capturas/sub/ok-verde-50.00-5.0-12-1030-n1.webp", "captura"), "capturas/sub/ok-verde-50.00-5.0-12-1030-n1.webp")

    # H2: empaquetar no mete archivos de fuera ni empaqueta algo que no verifica
    def test_h2_empaquetar_rechaza_manifiesto_con_ruta_fuera(self):
        fuera = self._secreto_fuera()
        self._manifiesto_con({"tipo": "captura", "nombre": "../secreto.txt", "bytes": os.path.getsize(fuera), "sha256": comun.sha256_archivo(fuera), "origen": "otro"})
        otro = os.path.join(self.tmp, "x.tar")
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(io.StringIO()):
            self.ev("empaquetar", "--dir", self.dir(), "--salida", otro)
        self.assertFalse(os.path.exists(otro))

    def test_h2_empaquetar_rechaza_respaldo_alterado(self):
        cap = os.path.join(self.dir(), "capturas", "101.webp")
        open(cap, "ab").write(b"x")
        otro = os.path.join(self.tmp, "x.tar")
        with self.assertRaises(SystemExit), contextlib.redirect_stderr(io.StringIO()):
            self.ev("empaquetar", "--dir", self.dir(), "--salida", otro)
        self.assertFalse(os.path.exists(otro))

    def test_h2_tar_no_contiene_rutas_ni_nombre_de_carpeta(self):
        import tarfile
        with tarfile.open(self.tar) as t:
            nombres = t.getnames()
        self.assertTrue(all(n.startswith("respaldo/") and ".." not in n.split("/") for n in nombres))
        self.assertNotIn(os.path.basename(self.dir()) + "/", "".join(nombres))

    def test_h2_tar_con_entrada_repetida_se_rechaza(self):
        import tarfile
        mal = os.path.join(self.tmp, "rep.tar")
        with tarfile.open(mal, "w") as t:
            for _ in range(2):
                ti = tarfile.TarInfo("respaldo/a.txt"); ti.size = 1; t.addfile(ti, io.BytesIO(b"x"))
        with self.assertRaises(SystemExit):
            evidencia_copias._extraer_seguro(mal, os.path.join(self.tmp, "dest2"))

    # H3: la copia A también se demuestra
    def test_h3_sin_comprobar_la_copia_a_es_incompleta(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b)
        ev = os.path.join(self.tmp, "ev.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "x", "--medio-b", "y", "--restauracion-b", rb, "--confirmo-conservar", "--salida", ev)
        self.assertEqual(cod, 1)
        self.assertFalse(json.load(open(ev))["copia_a_comprobada"])

    def test_h3_copia_a_con_otro_contenido_es_incompleta(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        basura = os.path.join(self.tmp, "basura.bin"); open(basura, "wb").write(b"no es el respaldo" * 100)
        a = os.path.join(self.tmp, "A.age")
        subprocess.run(["age", "-r", self.pub, "-o", a, basura], check=True)
        _, rb = self.restaurar(b)
        cod, ca = self.comprobar(a)
        self.assertEqual(cod, 1)
        ev = os.path.join(self.tmp, "ev.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "x", "--medio-b", "y", "--restauracion-b", rb, "--comprobacion-a", ca, "--confirmo-conservar", "--salida", ev)
        self.assertEqual(cod, 1)
        self.assertEqual(json.load(open(ev))["resultado"], "INCOMPLETA")

    def test_h3_comprobacion_de_otro_archivo_no_vale_para_a(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b)
        _, cb = self.comprobar(b, "A")  # se comprobó el archivo B y se etiquetó como A
        ev = os.path.join(self.tmp, "ev.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "x", "--medio-b", "y", "--restauracion-b", rb, "--comprobacion-a", cb, "--confirmo-conservar", "--salida", ev)
        self.assertEqual(cod, 1)

    def test_h3_comprobar_no_deja_nada_en_claro(self):
        a = self.cifrar(os.path.join(self.tmp, "A.age"))
        antes = set(os.listdir(self.tmp)) | set(os.listdir(tempfile.gettempdir()))
        self.assertEqual(self.comprobar(a)[0], 0)
        nuevos = (set(os.listdir(self.tmp)) | set(os.listdir(tempfile.gettempdir()))) - antes
        self.assertEqual(nuevos, {"COMPROBACION-A.json"})

    # H4: el tar descifrado temporal no va al temporal del sistema
    def test_h4_restaurar_no_usa_el_temporal_del_sistema(self):
        b = self.cifrar(os.path.join(self.tmp, "B.age"))
        visto = {}
        original = evidencia_copias._extraer_seguro
        def espia(tar_ruta, destino):
            visto["tar"] = tar_ruta
            return original(tar_ruta, destino)
        evidencia_copias._extraer_seguro = espia
        self.addCleanup(setattr, evidencia_copias, "_extraer_seguro", original)
        dest = os.path.join(self.tmp, "privado", "restB"); os.makedirs(os.path.dirname(dest))
        self.assertEqual(self.restaurar(b, destino=dest)[0], 0)
        self.assertEqual(os.path.dirname(os.path.dirname(visto["tar"])), os.path.dirname(dest))
        self.assertFalse(os.path.exists(visto["tar"]))  # se quitó al terminar
        self.assertEqual(os.listdir(os.path.dirname(dest)), ["restB"])

    # H5: el README no promete borrado seguro
    def test_h5_readme_no_promete_borrado_seguro(self):
        txt = open(os.path.join(os.path.dirname(AQUI), "README.md"), encoding="utf-8").read()
        self.assertIn("no garantiza", txt)
        self.assertIn("volumen cifrado", txt)
        for orden in ("rm -P", "cipher /w"):
            self.assertNotIn(orden, txt)

    # H6: inventario — capturas 10.77 fuera de la lista se informan, no se ocultan
    def test_h6_capturas_10_77_no_listadas_se_informan(self):
        self.sim.objetos["2001.webp"] = webp("2001")
        self.sim.tablas["errores_lector"].append({"id": 2001, "version": "10.77", "usuario": "aaaaaaaa-0000-4000-8000-000000000003", "lineas": []})
        self.assertEqual(self.correr(salida="h6")[0], 0)
        v = verificar.verificar(self.dir("h6"))
        self.assertEqual(v["ids_10_77_no_listados"], [2001])
        self.assertIn("V08d", {c["id"] for c in v["checks"] if c["estado"] == "advertencia"})
        self.assertEqual(v["resultado"], "APROBADO_CON_ADVERTENCIAS")
        self.assertEqual(self.verif()["ids_10_77_no_listados"], [])

    # H7: versiones coherentes
    def test_h7_todos_los_artefactos_llevan_la_misma_version(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b); _, ca = self.comprobar(a)
        for ruta in (os.path.join(self.dir(), "resumen.json"), os.path.join(self.dir(), "VERIFICACION.json"), self.emp, rb, ca):
            self.assertEqual(json.load(open(ruta))["procedimiento"], comun.VERSION_PROCEDIMIENTO, ruta)
        self.assertIn(comun.VERSION_PROCEDIMIENTO, open(os.path.join(os.path.dirname(AQUI), "README.md"), encoding="utf-8").read().splitlines()[0])

    def test_h7_respaldo_de_otra_version_no_verifica(self):
        res_p = os.path.join(self.dir(), "resumen.json")
        res = json.load(open(res_p)); res["procedimiento"] = "E0.1"
        open(res_p, "wb").write(comun.json_estable(res))
        self.assertIn("V02c", {c["id"] for c in self.verif()["checks"] if c["estado"] == "fallo"})

    def test_h7_restauracion_de_otra_version_es_incompleta(self):
        a, b = self.cifrar(os.path.join(self.tmp, "A.age")), self.cifrar(os.path.join(self.tmp, "B.age"))
        _, rb = self.restaurar(b); _, ca = self.comprobar(a)
        d = json.load(open(rb)); d["procedimiento"] = "E0.2"; json.dump(d, open(rb, "w"))
        ev = os.path.join(self.tmp, "ev.json")
        with contextlib.redirect_stderr(io.StringIO()):
            cod = self.ev("registrar", "--copia-a", a, "--copia-b", b, "--medio-a", "x", "--medio-b", "y", "--restauracion-b", rb, "--comprobacion-a", ca, "--confirmo-conservar", "--salida", ev)
        self.assertEqual(cod, 1)
        self.assertFalse(json.load(open(ev))["versiones_coherentes"])

    # H8: solo-listar avisa de nombres que detendrían el respaldo; archivo suelto no se tolera por prefijo
    def test_h8_solo_listar_avisa_nombres_no_permitidos(self):
        self.sim.objetos["raro nombre?.webp"] = webp("r")
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
            cod = respaldar.main(["--url", self.sim.url, "--permitir-local", "--espera", "0.01", "--ids-requeridos", self.ids, "--solo-listar"])
        self.assertEqual(cod, 2)
        self.assertEqual(json.loads(out.getvalue())["nombres_no_permitidos"], 1)

    def test_h8_archivo_extra_con_prefijo_verificacion_ya_no_se_tolera(self):
        open(os.path.join(self.dir(), "VERIFICACION-copia.json"), "w").write("{}")
        self.assertIn("V04", {c["id"] for c in self.verif()["checks"] if c["estado"] == "fallo"})


if __name__ == "__main__":
    unittest.main()
