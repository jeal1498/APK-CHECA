"""011 — Interruptores remotos de la IA y tiempos de respuesta (Postgres LOCAL)."""
import json
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, mig

REV = "bbbbbbbb-0000-4000-8000-00000000000b"
ADM = "aaaaaaaa-0000-4000-8000-00000000000a"
NAD = "cccccccc-0000-4000-8000-00000000000c"


class E11(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{REV}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin", "011_interruptores_ia"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector'), ('{REV}', 'revisor');")
        cls.pg.sql("delete from public.revisiones")
        filas = []
        # visión: 5 ok con ms_ia 1000..5000 y round trip 2000..6000; 1 con 402 (sin saldo); texto: 2 ok
        for i in range(1, 6):
            filas.append(f"('{NAD}', 'didi', '10.83', 'deepseek-flash', {i * 1000}, {(i + 1) * 1000}, 700, 30, 1, 0.0003, true, 'revision')")
        filas.append(f"('{NAD}', 'didi', '10.83', 'deepseek-flash', null, null, 0, 0, 1, 0, false, 'revision')")
        cls.pg.sql("insert into public.revisiones (usuario, app, version, modelo, ms_ia, ms_revision_anterior, tokens_entrada, tokens_salida, imagenes, costo_usd, ok, modo) values " + ",".join(filas))
        cls.pg.sql(f"update public.revisiones set error = 'http_402' where ok = false")
        cls.pg.sql(f"insert into public.revisiones (usuario, app, version, modelo, ms_ia, ms_revision_anterior, tokens_entrada, tokens_salida, imagenes, costo_usd, ok, modo) values "
                   f"('{NAD}', 'didi', '10.82', 'deepseek-flash', 600, 900, 150, 20, 0, 0.00005, true, 'revision'), ('{NAD}', 'didi', '10.82', 'deepseek-flash', 800, 1100, 150, 20, 0, 0.00005, true, 'revision'),"
                   f"('{NAD}', 'didi', '10.80', 'deepseek-flash', 800, 1100, 150, 20, 0, 0.00005, true, 'revision')")
        cls.pg.sql("update public.revisiones set creado = now() - interval '20 days' where version = '10.80'")   # fuera de los 7 días
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        E11.n += 1
        self.db = f"i{E11.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def como(self, u, s):
        return self.pg.sql(s, db=self.db, rol="authenticated", claims={"role": "authenticated", "sub": u}).strip()

    def rpc(self, u, s):
        return json.loads(self.como(u, "select " + s))

    def q(self, s):
        return self.pg.sql(s, db=self.db).strip()

    def test_por_defecto_todo_encendido(self):
        e = self.rpc(ADM, "public.admin_ia_estado()")
        self.assertEqual((e["vision"], e["texto"]), (True, True))

    def test_apagar_y_encender_cada_una_sin_afectar_a_la_otra(self):
        self.assertEqual(self.rpc(REV, "public.admin_ia_interruptor('texto', false)")["ok"], True)
        e = self.rpc(REV, "public.admin_ia_estado()")
        self.assertEqual((e["vision"], e["texto"]), (True, False))
        self.assertTrue(e["por"].startswith("admin_"))
        self.rpc(REV, "public.admin_ia_interruptor('vision', false)")
        self.rpc(REV, "public.admin_ia_interruptor('texto', true)")
        e = self.rpc(REV, "public.admin_ia_estado()")
        self.assertEqual((e["vision"], e["texto"]), (False, True))

    def test_solo_el_revisor_cambia_y_el_lector_solo_ve(self):
        self.assertEqual(self.rpc(ADM, "public.admin_ia_interruptor('vision', false)"), {"error": "no_autorizado"})
        self.assertEqual(self.rpc(NAD, "public.admin_ia_interruptor('vision', false)"), {"error": "no_autorizado"})
        self.assertEqual(self.rpc(NAD, "public.admin_ia_estado()"), {"error": "no_autorizado"})
        self.assertEqual(self.q("select vision from public.ia_config"), "t")
        self.assertIn("modos", self.rpc(ADM, "public.admin_ia_estado()"))
        for f in ("public.admin_ia_estado()", "public.admin_ia_interruptor('vision', false)"):
            with self.assertRaises(pg_local.ErrorSQL):
                self.pg.sql("select " + f, db=self.db, rol="anon")

    def test_valores_invalidos(self):
        for malo in ("'otra', true", "'vision', null", "null, true"):
            self.assertEqual(self.rpc(REV, f"public.admin_ia_interruptor({malo})"), {"error": "filtro_invalido"})

    def test_tiempos_de_visión_y_texto_por_separado_en_los_ultimos_7_dias(self):
        m = self.rpc(ADM, "public.admin_ia_estado()")["modos"]
        v, t = m["vision"], m["texto"]
        self.assertEqual((v["n"], v["ok"], v["sin_saldo"], v["errores"]), (6, 5, 1, 1))
        self.assertEqual((v["ms_ia_p50"], v["ms_ia_p90"]), (3000, 4600))          # 1000..5000, solo las ok
        self.assertEqual((v["ms_total_p50"], v["ms_total_p90"]), (4000, 5600))    # 2000..6000
        self.assertAlmostEqual(v["costo_usd"], 0.0015, places=4)
        self.assertEqual((t["n"], t["ms_ia_p50"]), (2, 700))                       # la fila de hace 20 días no cuenta

    def test_repetir_conserva_los_interruptores_y_revertir_limpia(self):
        self.rpc(REV, "public.admin_ia_interruptor('vision', false)")
        self.pg.archivo(mig("011_interruptores_ia"), db=self.db)
        self.assertEqual(self.q("select vision from public.ia_config"), "f")
        self.pg.archivo(mig("011_interruptores_ia", down=True), db=self.db)
        self.assertEqual(self.q("select to_regclass('public.ia_config') is null"), "t")
        self.assertEqual(self.q("select count(*) from public.revisiones"), "9")   # no toca revisiones


if __name__ == "__main__":
    unittest.main()
