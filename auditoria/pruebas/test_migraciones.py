"""E1/E2 — Pruebas de las migraciones de auditoría en Postgres LOCAL (no Supabase; ver pg_local.py).
Ejecutar: python3 -m unittest discover -s auditoria/pruebas -p 'test_*.py' -v"""
import json
import os
import unittest
import uuid

import pg_local

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SQL = os.path.join(RAIZ, "auditoria", "sql")
ESQUEMA = os.path.join(RAIZ, "supabase", "esquema.sql")
U1 = "11111111-1111-4111-8111-111111111111"
U2 = "22222222-2222-4222-8222-222222222222"
SVC = {"role": "service_role"}

LEGADO = f"""
insert into auth.users (id) values ('{U1}'), ('{U2}');
insert into public.errores_lector (usuario, app, version, lineas, local, ia, tipo, veredicto)
select '{U1}', 'DiDi', '10.77', '["$50.00","5 min (2 km)"]', '{{"tarifa":50}}', '{{"tarifa":50}}', 'diferencia',
       (array['ia_bien','ia_mal','dudoso',null])[1 + g % 4]
from generate_series(1, 40) g;
insert into public.revisiones (usuario, app, version, ok, coincide) select '{U1}', 'Uber', '10.79', true, g % 3 > 0 from generate_series(1, 30) g;
insert into public.capturas_ok (usuario, app, version, lineas, local, ia) values ('{U2}', 'Uber', '10.80', '[]', '{{}}', '{{}}');
"""


def mig(n, down=False):
    return os.path.join(SQL, f"{n}.down.sql" if down else f"{n}.sql")


def huella_publica(pg, db):
    """md5 del contenido de todas las tablas de public (orden estable)."""
    tablas = pg.sql("select tablename from pg_tables where schemaname='public' order by 1", db=db).split()
    return {t: pg.sql(f"select md5(coalesce(string_agg(x::text, '|' order by x::text), '')) from public.{t} x", db=db).strip()
            for t in tablas}


def evento(**k):
    e = {"id": str(uuid.uuid4()), "contrato": 1, "app": "didi", "version_app": "10.81", "version_parser": "10.79",
         "lector": "ocr", "fotograma_id": str(uuid.uuid4()), "captura_vinculo": "sin_captura",
         "ts_fotograma": "2026-11-15T10:00:00Z", "ts_evento": "2026-11-15T10:00:01Z", "resultado": "oferta",
         "tarifa": 120.5, "km": 10.2, "min": 25, "rec_km": 1.5, "rec_min": 5, "moneda": "MXN", "nivel": "verde",
         "tarjetas": 1, "ms_lectura": 420, "ms_popup": 650, "lineas_sha256": "a" * 64,
         "lineas_redactadas": ["$120.50", "5 min (1.5 km)"], "orientacion": "vertical",
         "etapas": [{"etapa": "captura", "estado": "ok", "ms": 30}, {"etapa": "parser", "estado": "ok", "ms": 2}]}
    e.update(k)
    return {x: v for x, v in e.items() if v is not None}


class Migraciones(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO)
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        Migraciones.n += 1
        self.db = f"t{Migraciones.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def q(self, s, **k):
        return self.pg.sql(s, db=self.db, **k)

    def subir(self, *ns):
        for n in ns:
            self.pg.archivo(mig(n), db=self.db)

    def registrar(self, eventos, u=U1, rol="service_role"):
        out = self.q(f"select public.auditoria_registrar_lote('{u}', $j${json.dumps(eventos)}$j$::jsonb)", rol=rol, claims=SVC)
        return json.loads(out.strip())

    # ---------- E1: idempotencia, compatibilidad, datos previos intactos ----------
    def test_migraciones_repetidas_no_fallan_ni_cambian_datos_previos(self):
        antes = huella_publica(self.pg, self.db)
        self.subir("001_auditoria", "002_registrar", "001_auditoria", "002_registrar")
        self.assertEqual(huella_publica(self.pg, self.db), antes)
        self.assertEqual(self.q("select string_agg(version::text, ',' order by version) from auditoria.version_esquema").strip(), "1,2")

    def test_esquema_sql_se_puede_reaplicar_despues(self):
        # supabase.yml vuelve a ejecutar esquema.sql completo en cada despliegue: no debe chocar con auditoria.
        self.subir("001_auditoria", "002_registrar")
        self.registrar([evento()])
        self.pg.archivo(ESQUEMA, db=self.db)
        self.assertEqual(self.q("select count(*) from auditoria.lecturas").strip(), "1")

    def test_reversion_y_vuelta_a_aplicar(self):
        antes = huella_publica(self.pg, self.db)
        self.subir("001_auditoria", "002_registrar")
        self.registrar([evento()])
        self.pg.archivo(mig("002_registrar", True), db=self.db)
        self.assertEqual(self.q("select count(*) from auditoria.lecturas").strip(), "1")  # 002 down conserva datos
        self.pg.archivo(mig("001_auditoria", True), db=self.db)
        self.assertEqual(self.q("select count(*) from pg_namespace where nspname='auditoria'").strip(), "0")
        self.assertEqual(huella_publica(self.pg, self.db), antes)
        self.subir("001_auditoria", "002_registrar")
        self.assertEqual(self.registrar([evento()])[0]["estado"], "guardado")

    def test_indices_esperados(self):
        self.subir("001_auditoria")
        idx = set(self.q("select indexname from pg_indexes where schemaname='auditoria'").split())
        for i in ("lecturas_pkey", "lecturas_usuario_ts", "lecturas_ts", "lecturas_version_app", "lecturas_expira",
                  "lecturas_fotograma_resultado", "etapas_pkey", "recepciones_pkey"):
            self.assertIn(i, idx)

    # ---------- E1: restricciones ----------
    def _insertar(self, **k):
        self.subir("001_auditoria", "002_registrar")
        return self.registrar([evento(**k)])[0]

    def test_oferta_valida_se_guarda(self):
        r = self._insertar()
        self.assertEqual(r["estado"], "guardado")
        self.assertEqual(self.q("select count(*) from auditoria.etapas").strip(), "2")

    def test_restricciones_rechazan_datos_inconsistentes(self):
        casos = {
            "oferta sin semáforo": dict(nivel=None),
            "oferta sin tarifa": dict(tarifa=None),
            "oferta con km 0": dict(km=0),
            "semáforo sin oferta": dict(resultado="no_oferta", tarifa=None, km=None, min=None),
            "varias con datos": dict(resultado="varias", nivel=None),
            "vínculo exacto sin sha": dict(captura_vinculo="exacto"),
            "ocr sin fotograma": dict(fotograma_id=None),
            "app desconocida": dict(app="cabify"),
            "sha mal formado": dict(lineas_sha256="xyz"),
            "contrato futuro": dict(contrato=2),
            "tarifa negativa": dict(tarifa=-5),
            "demasiadas líneas": dict(lineas_redactadas=["$1"] * 31),
            "fotograma posterior al evento": dict(ts_fotograma="2026-11-15T11:00:00Z"),
            "etapa desconocida": dict(etapas=[{"etapa": "magia", "estado": "ok"}]),
        }
        self.subir("001_auditoria", "002_registrar")
        for nombre, k in casos.items():
            with self.subTest(nombre):
                r = self.registrar([evento(**k)])[0]
                self.assertEqual(r["estado"], "invalido", nombre)
        self.assertEqual(self.q("select count(*) from auditoria.lecturas").strip(), "0")
        self.assertEqual(self.q("select count(*) from auditoria.etapas").strip(), "0")  # nada parcial

    def test_varias_y_abstencion_sin_semaforo_si_se_guardan(self):
        self.subir("001_auditoria", "002_registrar")
        r = self.registrar([evento(resultado="varias", tarifa=None, km=None, min=None, nivel=None, motivo="tarjeta_ambigua"),
                            evento(resultado="abstencion", tarifa=None, km=None, min=None, nivel=None, motivo="datos_insuficientes")])
        self.assertEqual([x["estado"] for x in r], ["guardado", "guardado"])

    def test_cascada_al_borrar_usuario(self):
        self._insertar()
        self.q(f"delete from auth.users where id = '{U1}'")
        for t in ("lecturas", "etapas", "recepciones"):
            self.assertEqual(self.q(f"select count(*) from auditoria.{t}").strip(), "0", t)

    # ---------- E1: permisos / RLS ----------
    def test_anon_y_authenticated_no_acceden(self):
        self._insertar()
        for rol in ("anon", "authenticated"):
            claims = {"role": rol, "sub": U1}
            for s in ("select * from auditoria.lecturas", "select * from auditoria.etapas",
                      "insert into auditoria.lecturas (id) values (gen_random_uuid())",
                      f"select public.auditoria_registrar_lote('{U1}', '[]'::jsonb)", "select auditoria.purgar_vencidas()"):
                with self.subTest(rol=rol, sql=s):
                    with self.assertRaises(pg_local.ErrorSQL) as e:
                        self.q(s, rol=rol, claims=claims)
                    self.assertIn("permission denied", str(e.exception))

    def test_service_role_no_lee_tablas_directamente_solo_por_funcion(self):
        self.subir("001_auditoria", "002_registrar")
        with self.assertRaises(pg_local.ErrorSQL):
            self.q("insert into auditoria.lecturas (id) values (gen_random_uuid())", rol="service_role", claims=SVC)

    # ---------- E1: retención ----------
    def test_purga_solo_lo_vencido(self):
        self.subir("001_auditoria", "002_registrar")
        a, b = evento(), evento()
        self.registrar([a, b])
        self.q(f"update auditoria.lecturas set expira = now() - interval '1 day' where id = '{a['id']}'")
        self.assertEqual(self.q("select auditoria.purgar_vencidas()").strip(), "1")
        self.assertEqual(self.q("select id from auditoria.lecturas").strip(), b["id"])
        self.assertEqual(self.q("select count(*) from auditoria.recepciones").strip(), "1")

    def test_expira_a_los_30_dias(self):
        self._insertar()
        d = self.q("select round(extract(epoch from (expira - recibido)) / 86400) from auditoria.lecturas").strip()
        self.assertEqual(d, "30")

    def test_purga_no_esta_programada(self):
        self.subir("001_auditoria", "002_registrar")
        self.assertEqual(self.q("select count(*) from pg_extension where extname = 'pg_cron'").strip(), "0")
        texto = "".join(open(mig(n), encoding="utf-8").read() for n in ("001_auditoria", "002_registrar"))
        self.assertNotIn("cron.schedule", texto)

    # ---------- E2 (SQL): idempotencia y conflictos ----------
    def test_reintento_identico_es_repetido_y_no_duplica(self):
        self.subir("001_auditoria", "002_registrar")
        e = evento()
        self.assertEqual(self.registrar([e])[0]["estado"], "guardado")
        self.assertEqual(self.registrar([e])[0]["estado"], "repetido")
        self.assertEqual(self.q("select count(*), max(intentos) from auditoria.lecturas, auditoria.recepciones").strip(), "1|2")

    def test_mismo_id_con_otro_contenido_es_conflicto_y_no_sobrescribe(self):
        self.subir("001_auditoria", "002_registrar")
        e = evento()
        self.registrar([e])
        e2 = dict(e, tarifa=999)
        self.assertEqual(self.registrar([e2])[0]["estado"], "conflicto")
        self.assertEqual(self.q("select tarifa from auditoria.lecturas").strip(), "120.50")

    def test_mismo_id_de_otro_usuario_es_conflicto(self):
        self.subir("001_auditoria", "002_registrar")
        e = evento()
        self.registrar([e], u=U1)
        self.assertEqual(self.registrar([e], u=U2)[0]["estado"], "conflicto")
        self.assertEqual(self.q("select usuario from auditoria.lecturas").strip(), U1)

    def test_lote_mixto_guarda_lo_valido(self):
        self.subir("001_auditoria", "002_registrar")
        r = self.registrar([evento(), evento(app="x"), evento()])
        self.assertEqual([x["estado"] for x in r], ["guardado", "invalido", "guardado"])

    def test_lote_vacio_o_grande_se_rechaza(self):
        self.subir("001_auditoria", "002_registrar")
        for ev in ([], [evento() for _ in range(51)]):
            with self.assertRaises(pg_local.ErrorSQL):
                self.registrar(ev)

    def test_usuario_nulo_se_rechaza(self):
        self.subir("001_auditoria", "002_registrar")
        with self.assertRaises(pg_local.ErrorSQL):
            self.q(f"select public.auditoria_registrar_lote(null, $j${json.dumps([evento()])}$j$::jsonb)", rol="service_role", claims=SVC)


    # ---------- E2: el contrato TypeScript y la base de datos dicen lo mismo ----------
    def test_contrato_ts_y_base_coinciden_en_cada_caso(self):
        import subprocess
        out = subprocess.run(["node", "--experimental-strip-types", "--no-warnings", os.path.join(RAIZ, "auditoria", "pruebas", "e2", "exportar_casos.ts")],
                             capture_output=True, text=True, check=True).stdout
        casos = json.loads(out)
        self.assertGreaterEqual(len(casos), 20)
        self.subir("001_auditoria", "002_registrar")
        for c in casos:
            with self.subTest(c["nombre"]):
                self.assertEqual(c["ts"], c["esperado"], "contrato TS")
                r = self.registrar([c["evento"]])[0]
                self.assertEqual(r["estado"] == "guardado", c["esperado"], f"base: {r}")


    # ---------- E3: lo que serializa Traza.kt lo aceptan el contrato TS y la base ----------
    def test_muestras_de_android_pasan_contrato_y_base(self):
        import subprocess
        ruta = os.path.join(RAIZ, "evidencias", "e3", "muestras_kotlin.json")
        out = subprocess.run(["node", "--experimental-strip-types", "--no-warnings", os.path.join(RAIZ, "auditoria", "pruebas", "e2", "validar_archivo.ts"), ruta],
                             capture_output=True, text=True, check=True).stdout
        self.assertEqual([x for x in json.loads(out) if x["error"]], [])
        muestras = json.load(open(ruta, encoding="utf-8"))
        self.subir("001_auditoria", "002_registrar")
        r = self.registrar(muestras)
        self.assertEqual([x["estado"] for x in r], ["guardado"] * len(muestras))
        self.assertEqual(self.q("select count(*) from auditoria.lecturas where captura_vinculo = 'exacto'").strip(), "1")
        self.assertEqual(self.q("select count(*) from auditoria.lecturas where resultado = 'varias' and tarifa is null").strip(), "1")


if __name__ == "__main__":
    unittest.main()
