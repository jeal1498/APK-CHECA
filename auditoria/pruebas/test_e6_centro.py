"""E6/E7 — Centro de control (006): salud, expediente, seguimiento, versiones, dispositivos, IA, costos y configuración (Postgres LOCAL)."""
import json
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, U1, U2, evento, mig

ADM = "aaaaaaaa-0000-4000-8000-00000000000a"
REV = "bbbbbbbb-0000-4000-8000-00000000000b"
NAD = "cccccccc-0000-4000-8000-00000000000c"
R = "desde => '2026-11-01'::timestamptz, hasta => '2026-11-30'::timestamptz"
FUNCS = ["admin_salud", "admin_versiones", "admin_dispositivos", "admin_ia", "admin_costos"]


class Centro(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{REV}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin", "005_metricas", "006_centro"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector'), ('{REV}', 'revisor');")
        evs = [evento(app="didi" if i % 2 else "uber", ts_evento=f"2026-11-1{i % 5}T10:00:01Z", ts_fotograma=f"2026-11-1{i % 5}T10:00:00Z",
                      telefono="samsung SM-A15" if i % 3 else "xiaomi 23", android="14") for i in range(12)]
        cls.evs = evs
        for u, lote in ((U1, evs[:8]), (U2, evs[8:])):
            cls.pg.sql(f"select public.auditoria_registrar_lote('{u}', $j${json.dumps(lote)}$j$::jsonb)")
        cls.pg.sql("update public.errores_lector set creado = '2026-11-14T12:00:00Z'")
        cls.pg.sql("insert into public.fallas_ia (dia, tipo, n) values ('2026-11-14', 'http_402', 2)")
        cls.pg.sql("update public.revisiones set creado = '2026-11-14T12:00:00Z', modelo = 'deepseek-flash', costo_usd = 0.001, ms_ia = 800")
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        Centro.n += 1
        self.db = f"c{Centro.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def como(self, u, s):
        return self.pg.sql(s, db=self.db, rol="authenticated", claims={"role": "authenticated", "sub": u}).strip()

    def rpc(self, u, s):
        return json.loads(self.como(u, "select " + s))

    def q(self, s):
        return self.pg.sql(s, db=self.db).strip()

    def test_acceso_denegado_y_anon(self):
        for f in FUNCS:
            self.assertEqual(self.rpc(NAD, f"public.{f}({R})"), {"error": "no_autorizado"})
            with self.assertRaises(pg_local.ErrorSQL):
                self.pg.sql(f"select public.{f}({R})", db=self.db, rol="anon")
        self.assertEqual(self.rpc(NAD, "public.admin_config()"), {"error": "no_autorizado"})
        self.assertEqual(self.rpc(NAD, "public.admin_error(1)"), {"error": "no_autorizado"})

    def test_rango_invalido(self):
        self.assertEqual(self.rpc(ADM, "public.admin_salud('2026-11-30'::timestamptz, '2026-11-01'::timestamptz)"), {"error": "rango"})

    def test_salud_componentes_y_estados_honestos(self):
        s = self.rpc(ADM, f"public.admin_salud({R})")
        comp = {c["clave"]: c for c in s["componentes"]}
        self.assertEqual(comp["base_de_datos"]["estado"], "operativo")
        self.assertEqual(comp["ocr"]["estado"], "sin_datos")           # n < 30: no se pinta verde sin datos
        self.assertEqual(comp["mercado_pago"]["estado"], "sin_comprobar")
        self.assertEqual(comp["play_billing"]["estado"], "no_aplicable")
        self.assertEqual(comp["revision_ia"]["estado"], "fuera_de_servicio")   # http_402 sembrado
        self.assertEqual(comp["ingesta"]["estado"], "operativo")             # recepción reciente
        self.assertEqual(s["estado_general"], "rojo")
        ind = {i["clave"]: i for i in s["indicadores"]}
        self.assertEqual(ind["lecturas"]["valor"], 12)
        self.assertEqual(ind["tasa_errores"]["estado"], "insuficiente")
        self.assertEqual(ind["calidad_interpretacion"]["n"], 0)               # sin referencia: no se cuenta como correcto
        self.assertIsNone(ind["calidad_interpretacion"].get("valor"))
        self.assertTrue(any(a["titulo"].startswith("La revisión con IA no tiene saldo") for a in s["atencion"]))
        self.assertIsNone(s["procesamiento"]["pendientes"])

    def test_tope_diario_no_es_falla_del_servicio(self):
        """4872 de 4874 «fallas» reales eran el tope diario por conductor (límite deliberado): no cuentan como falla."""
        self.q("delete from public.fallas_ia")
        self.q("update public.revisiones set creado='2026-11-14T12:00:00Z', ok=true, coincide=true")
        self.q("insert into public.revisiones (usuario, app, version, ok, error, creado) select '%s','Uber','10.80',false,'tope_diario','2026-11-14T12:00:00Z' from generate_series(1,200)" % U1)
        s = self.rpc(ADM, f"public.admin_salud({R})")
        c = {x["clave"]: x for x in s["componentes"]}["revision_ia"]
        self.assertEqual(c["estado"], "operativo")
        self.assertEqual(c["k"], 0)
        self.assertEqual(c["tope_diario"], 200)

    def test_salud_sin_datos_no_es_verde(self):
        s = self.rpc(ADM, "public.admin_salud('2025-01-01'::timestamptz, '2025-02-01'::timestamptz)")
        self.assertNotEqual(s["estado_general"], "verde")
        self.assertEqual(s["indicadores"][0]["valor"], 0)

    def test_expediente_y_seguimiento(self):
        eid = int(self.q("select min(id) from public.errores_lector"))
        e = self.rpc(ADM, f"public.admin_error({eid})")
        self.assertEqual(e["estado_seguimiento"], "nuevo")
        self.assertNotIn('$50.00', json.dumps(e))                       # no se devuelven las líneas (direcciones)
        self.assertIn("clasificacion", e)
        self.assertEqual(self.rpc(ADM, f"public.admin_seguimiento({eid}, 'en_investigacion')"), {"error": "no_autorizado"})  # lector no cambia
        r = self.rpc(REV, f"public.admin_seguimiento({eid}, 'causa_identificada', 'ocr', 'prueba')")
        self.assertIn("id", r)
        e = self.rpc(ADM, f"public.admin_error({eid})")
        self.assertEqual(e["estado_seguimiento"], "causa_identificada")
        self.assertEqual(e["historial"][0]["causa"], "ocr")
        self.assertEqual(self.rpc(REV, f"public.admin_seguimiento({eid}, 'inventado')")["error"], "seguimiento_invalido")
        with self.assertRaises(pg_local.ErrorSQL):
            self.q("update auditoria.seguimiento_errores set estado = 'cerrado'")
        self.assertEqual(self.rpc(ADM, "public.admin_error(999999)"), {"error": "no_existe"})

    def test_enlace_linea_captura_huella(self):
        """El caso se une a su lectura por el fotograma y trae la huella de la captura que miró el lector."""
        sha = "e" * 64
        fot = self.evs[0]["fotograma_id"]
        self.q(f"update auditoria.lecturas set captura_sha256='{sha}', captura_vinculo='exacto' where fotograma_id='{fot}'")
        self.q(f"update public.errores_lector set fotograma_id='{fot}' where id=(select min(id) from public.errores_lector)")
        eid = int(self.q("select min(id) from public.errores_lector"))
        e = self.rpc(ADM, f"public.admin_error({eid})")
        self.assertEqual(e["enlace"]["captura_sha256"], sha)
        self.assertEqual(e["enlace"]["fotograma_id"], fot)
        # un caso sin fotograma (anterior a la 10.82) no inventa enlace
        otro = int(self.q("select max(id) from public.errores_lector"))
        self.assertIsNone(self.rpc(ADM, f"public.admin_error({otro})")["enlace"])
        with self.assertRaises(pg_local.ErrorSQL):
            self.como(ADM, "select * from auditoria.enlace_capturas")

    def test_versiones_dispositivos_ia_costos_config(self):
        v = self.rpc(ADM, f"public.admin_versiones({R})")
        self.assertEqual(sum(x["lecturas"] for x in v["versiones"]), 12)
        d = self.rpc(ADM, f"public.admin_dispositivos({R})")
        self.assertTrue(all(x["muestra_suficiente"] is False for x in d["dispositivos"]))
        self.assertNotIn(U1, json.dumps(d))                              # solo seudónimos
        i = self.rpc(ADM, f"public.admin_ia({R})")
        self.assertEqual(i["casos"], 40)
        self.assertEqual(i["con_referencia_humana"], 0)
        c = self.rpc(ADM, f"public.admin_costos({R})")
        self.assertEqual(c["por_modelo"][0]["modelo"], "deepseek-flash")
        cfg = self.rpc(REV, "public.admin_config()")
        self.assertEqual(cfg["yo"]["rol"], "revisor")
        self.assertEqual(cfg["esquema"], 6)
        self.assertNotIn(ADM, json.dumps(cfg))

    def test_bitacora_y_reversion(self):
        self.rpc(ADM, f"public.admin_salud({R})")
        self.assertEqual(self.q("select count(*) from auditoria.acciones_admin where accion='salud' and resultado='ok'"), "1")
        self.pg.archivo(mig("006_centro", down=True), db=self.db)
        self.assertEqual(self.q("select count(*) from pg_proc where proname in ('admin_salud','admin_error','admin_config')"), "0")


if __name__ == "__main__":
    unittest.main()
