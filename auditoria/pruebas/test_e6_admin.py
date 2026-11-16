"""E6 — Admin MVP: control de acceso, filtros, paginación, bitácora y coincidencia con las fuentes (Postgres LOCAL)."""
import json
import unittest
import uuid

import pg_local
from test_migraciones import ESQUEMA, LEGADO, U1, U2, evento, mig

ADM = "aaaaaaaa-0000-4000-8000-00000000000a"   # administrador lector
REV = "bbbbbbbb-0000-4000-8000-00000000000b"   # administrador revisor
NAD = "cccccccc-0000-4000-8000-00000000000c"   # conductor cualquiera (authenticated, no admin)
R = "desde => '2026-11-01'::timestamptz, hasta => '2026-11-30'::timestamptz"


class E6(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{REV}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector'), ('{REV}', 'revisor');")
        evs = [evento(app="didi" if i % 2 else "uber", ts_evento=f"2026-11-1{i % 5}T10:00:01Z", ts_fotograma=f"2026-11-1{i % 5}T10:00:00Z")
               for i in range(12)]
        evs += [evento(resultado="duplicada", motivo="misma_oferta_60s", tarifa=None, km=None, min=None, nivel=None,
                       ts_evento="2026-11-12T10:00:01Z", ts_fotograma="2026-11-12T10:00:00Z") for _ in range(3)]
        evs += [evento(ts_evento="2026-10-01T10:00:01Z", ts_fotograma="2026-10-01T10:00:00Z")]  # fuera de rango
        cls.evs = evs
        for u, lote in ((U1, evs[:8]), (U2, evs[8:])):
            cls.pg.sql(f"select public.auditoria_registrar_lote('{u}', $j${json.dumps(lote)}$j$::jsonb)")
        cls.pg.sql("update public.errores_lector set creado = '2026-11-14T12:00:00Z'")
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        E6.n += 1
        self.db = f"a{E6.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def como(self, u, s):
        return self.pg.sql(s, db=self.db, rol="authenticated", claims={"role": "authenticated", "sub": u}).strip()

    def rpc(self, u, s):
        return json.loads(self.como(u, "select " + s))

    def q(self, s):
        return self.pg.sql(s, db=self.db).strip()

    # ---------- acceso ----------
    def test_anon_no_puede_llamar(self):
        for f in (f"public.admin_inicio({R})", f"public.admin_errores({R})", f"public.admin_ofertas({R})", "public.admin_bitacora()"):
            with self.subTest(f):
                with self.assertRaises(pg_local.ErrorSQL) as e:
                    self.pg.sql("select " + f, db=self.db, rol="anon")
                self.assertIn("permission denied", str(e.exception))

    def test_no_admin_es_denegado_y_queda_en_bitacora(self):
        for f in (f"public.admin_inicio({R})", f"public.admin_errores({R})", f"public.admin_ofertas({R})"):
            self.assertEqual(self.rpc(NAD, f), {"error": "no_autorizado"})
        self.assertEqual(self.q("select count(*) from auditoria.acciones_admin where resultado='denegado'"), "3")
        self.assertNotIn(NAD, self.q("select string_agg(admin, ',') from auditoria.acciones_admin"))  # seudónimo, no uuid

    def test_no_admin_no_lee_tablas_directo(self):
        for t in ("auditoria.lecturas", "auditoria.administradores", "auditoria.acciones_admin", "public.errores_lector"):
            with self.subTest(t):
                with self.assertRaises(pg_local.ErrorSQL):
                    self.como(ADM, f"select * from {t}")

    def test_lector_no_da_veredictos_revisor_si(self):
        e = self.evs[0]["id"]
        args = f"'lectura', '{e}', 'oferta', 120.5, 10.2, 25, null, 'alta', '{'d' * 64}'"
        self.assertEqual(self.rpc(ADM, f"public.admin_veredicto({args})"), {"error": "no_autorizado"})
        r = self.rpc(REV, f"public.admin_veredicto({args})")
        self.assertIn("id", r)
        self.assertEqual(self.q(f"select estado from auditoria.veredicto_vigente where lectura_id='{e}'"), "referencia_humana")
        self.assertTrue(self.q(f"select autor from auditoria.veredictos where id={r['id']}").startswith("admin_"))
        sin = self.rpc(REV, f"public.admin_veredicto('lectura', '{e}', 'oferta', 1, 1, 1)")
        self.assertEqual(sin["error"], "objetivo_o_evidencia")
        malo = self.rpc(REV, f"public.admin_veredicto('lectura', '{self.evs[1]['id']}', 'oferta', null, null, null, null, 'alta', '{'d' * 64}')")
        self.assertEqual(malo["error"], "veredicto_invalido")

    # ---------- datos = fuente ----------
    def test_inicio_cuadra_con_las_tablas(self):
        r = self.rpc(ADM, f"public.admin_inicio({R})")
        self.assertEqual(r["lecturas"], 15)  # 16 menos la de octubre
        self.assertEqual(r["por_resultado"], {"oferta": 12, "duplicada": 3})
        self.assertEqual(r["conductores"], 2)
        self.assertEqual(r["errores_lector"], int(self.q("select count(*) from public.errores_lector")))

    def test_ofertas_filtros_y_paginacion(self):
        r = self.rpc(ADM, f"public.admin_ofertas({R}, app => 'didi', resultado => 'oferta', por_pagina => 4)")
        self.assertEqual(r["total"], 6)
        self.assertEqual(len(r["filas"]), 4)
        p2 = self.rpc(ADM, f"public.admin_ofertas({R}, app => 'didi', resultado => 'oferta', pagina => 2, por_pagina => 4)")
        self.assertEqual(len(p2["filas"]), 2)
        ids = {f["id"] for f in r["filas"]} | {f["id"] for f in p2["filas"]}
        self.assertEqual(len(ids), 6)  # sin repetidos entre páginas
        f = r["filas"][0]
        self.assertNotIn("usuario", f)
        self.assertNotIn("lineas_redactadas", f)
        self.assertEqual(len(f["conductor"]), 8)
        self.assertEqual([e["etapa"] for e in f["etapas"]], ["captura", "parser"])

    def test_errores_filtros_y_sin_datos_personales(self):
        r = self.rpc(ADM, f"public.admin_errores({R}, version => '10.77', por_pagina => 100)")
        self.assertEqual(r["total"], int(self.q("select count(*) from public.errores_lector where version='10.77'")))
        texto = json.dumps(r)
        for prohibido in ("usuario", "lineas", U1):
            self.assertNotIn(prohibido, texto)
        self.assertTrue(all(f["estado_veredicto"] == "heredado_no_revisado" or f["estado_veredicto"] == "sin_veredicto" for f in r["filas"]))
        h = self.rpc(ADM, f"public.admin_errores({R}, estado_veredicto => 'heredado_no_revisado', por_pagina => 100)")
        self.assertEqual(h["total"], int(self.q("select count(*) from public.errores_lector where veredicto is not null")))

    def test_limites_de_filtros(self):
        casos = {"por_pagina": f"public.admin_ofertas({R}, por_pagina => 500)",
                 "pagina": f"public.admin_ofertas({R}, pagina => 0)",
                 "rango_mayor_a_92_dias": "public.admin_ofertas('2026-01-01', '2026-11-30')",
                 "rango": "public.admin_inicio('2026-11-30', '2026-11-01')",
                 "resultado": f"public.admin_ofertas({R}, resultado => 'x')",
                 "estado_veredicto": f"public.admin_errores({R}, estado_veredicto => 'x')"}
        for err, s in casos.items():
            with self.subTest(err):
                self.assertEqual(self.rpc(ADM, s), {"error": err})
        # Las negativas/inválidos se anotan una vez por minuto por persona y acción (A5): 3 acciones distintas.
        self.assertEqual(self.q("select count(*) from auditoria.acciones_admin where resultado='invalido'"), "3")

    def test_a5_un_conductor_no_puede_inflar_la_bitacora(self):
        for _ in range(25):
            self.assertEqual(self.rpc(NAD, f"public.admin_ofertas({R})"), {"error": "no_autorizado"})
        self.assertEqual(self.q("select count(*) from auditoria.acciones_admin"), "1")
        self.q("update pg_trigger set tgenabled = 'D' where tgname = 'acciones_admin_solo_agregar'")
        self.q("update auditoria.acciones_admin set creado = now() - interval '2 minutes'")
        self.q("update pg_trigger set tgenabled = 'O' where tgname = 'acciones_admin_solo_agregar'")
        self.rpc(NAD, f"public.admin_ofertas({R})")
        self.assertEqual(self.q("select count(*) from auditoria.acciones_admin"), "2")  # pasado el minuto vuelve a anotarse
        for _ in range(3):  # las consultas correctas se anotan siempre
            self.rpc(ADM, f"public.admin_ofertas({R})")
        self.assertEqual(self.q("select count(*) from auditoria.acciones_admin where resultado='ok'"), "3")

    def test_a3_un_solo_revisor_no_se_muestra_como_referencia(self):
        e = self.evs[2]["id"]
        self.rpc(REV, f"public.admin_veredicto('lectura', '{e}', 'oferta', 120.5, 10.2, 25, null, 'alta', '{'d' * 64}')")
        r = self.rpc(ADM, f"public.admin_ofertas({R}, estado_veredicto => 'referencia_humana')")
        self.assertEqual(r["total"], 1)
        self.assertIsNone(r["filas"][0]["referencia"])

    def test_inyeccion_en_filtros_no_hace_nada(self):
        r = self.rpc(ADM, f"public.admin_ofertas({R}, app => $$didi' or '1'='1$$)")
        self.assertEqual(r["total"], 0)

    # ---------- bitácora ----------
    def test_cada_consulta_queda_en_bitacora_y_no_se_altera(self):
        self.rpc(ADM, f"public.admin_inicio({R})")
        self.rpc(ADM, f"public.admin_ofertas({R}, por_pagina => 3)")
        b = self.rpc(ADM, "public.admin_bitacora()")["filas"]
        self.assertEqual([x["accion"] for x in b[:2]], ["ofertas", "inicio"])
        self.assertEqual(b[0]["filas"], 3)
        self.assertEqual(b[0]["filtros"]["por_pagina"], 3)
        for s in ("update auditoria.acciones_admin set accion='x'", "delete from auditoria.acciones_admin"):
            with self.assertRaises(pg_local.ErrorSQL):
                self.q(s)

    def test_reversion(self):
        self.pg.archivo(mig("004_admin", True), db=self.db)
        self.assertEqual(self.q("select count(*) from pg_proc where proname like 'admin\\_%'"), "0")
        self.assertEqual(self.q("select count(*) from auditoria.lecturas"), "16")
        self.pg.archivo(mig("004_admin"), db=self.db)


if __name__ == "__main__":
    unittest.main()
