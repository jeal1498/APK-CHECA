"""007 — Revisión humana de capturas de ofertas normales: acceso, cola, conteos, revisión, deshacer y restricciones (Postgres LOCAL)."""
import json
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, mig

ADM = "aaaaaaaa-0000-4000-8000-00000000000a"   # administrador lector
REV = "bbbbbbbb-0000-4000-8000-00000000000b"   # administrador revisor
NAD = "cccccccc-0000-4000-8000-00000000000c"   # conductor cualquiera
SHA = "a" * 64


class E8(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{REV}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin", "007_revision_ofertas"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector'), ('{REV}', 'revisor');")
        filas = []
        # id, nivel, creado; el 4 es anterior a la 10.82 y el 5 no tiene archivo en el almacén
        for i, nivel, creado in ((1, "rojo", "2026-11-18T10:00:00Z"), (2, "rojo", "2026-11-18T11:00:00Z"), (3, "verde", "2026-11-18T12:00:00Z"),
                                 (4, "amarillo", "2026-11-17T12:00:00Z"), (5, "amarillo", "2026-11-18T13:00:00Z"), (16, "rojo", "2026-11-18T14:00:00Z")):
            filas.append(f"({i}, '{NAD}', 'didi', 'MXN', '10.80', 'x', '[\"a\"]', '{{\"tarifa\":45.62,\"km\":0.761,\"min\":3}}', '{{}}', '{nivel}', '{creado}')")
        cls.pg.sql("delete from public.capturas_ok")   # el LEGADO trae filas de ejemplo
        cls.pg.sql("insert into public.capturas_ok (id, usuario, app, moneda, version, lector, lineas, local, ia, nivel, creado) values " + ",".join(filas))
        for nombre in ("ok-rojo-45.62-0.8-3-1-n1.webp", "ok-rojo-45.62-0.8-3-2-n2.webp", "ok-verde-45.62-0.8-3-3-n3.webp",
                       "ok-amarillo-45.62-0.8-3-4-n4.webp", "ok-rojo-45.62-0.8-3-16-n16.webp"):
            cls.pg.sql(f"insert into storage.objects (bucket_id, name) values ('capturas', '{nombre}')")
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        E8.n += 1
        self.db = f"r{E8.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def como(self, u, s):
        return self.pg.sql(s, db=self.db, rol="authenticated", claims={"role": "authenticated", "sub": u}).strip()

    def rpc(self, u, s):
        return json.loads(self.como(u, "select " + s))

    def q(self, s):
        return self.pg.sql(s, db=self.db).strip()

    def revisar(self, u, cid, res, **kw):
        extra = "".join(f", p_{k} => {v!r}" if not isinstance(v, str) else f", p_{k} => '{v}'" for k, v in kw.items())
        return self.rpc(u, f"public.admin_revisar_captura({cid}, '{res}'{extra})")

    # ---------- acceso ----------
    def test_anon_no_puede_llamar(self):
        for f in ("public.admin_capturas_pendientes()", "public.admin_revisar_captura(1, 'correcta')"):
            with self.assertRaises(pg_local.ErrorSQL) as e:
                self.pg.sql("select " + f, db=self.db, rol="anon")
            self.assertIn("permission denied", str(e.exception))

    def test_no_admin_es_denegado(self):
        self.assertEqual(self.rpc(NAD, "public.admin_capturas_pendientes()"), {"error": "no_autorizado"})
        self.assertEqual(self.revisar(NAD, 1, "correcta"), {"error": "no_autorizado"})

    def test_lector_ve_la_cola_pero_no_revisa(self):
        self.assertIn("items", self.rpc(ADM, "public.admin_capturas_pendientes()"))
        self.assertEqual(self.revisar(ADM, 1, "correcta"), {"error": "no_autorizado"})
        self.assertEqual(self.q("select count(*) from auditoria.revisiones_ofertas"), "0")

    def test_no_admin_no_lee_tablas_directo(self):
        for t in ("auditoria.revisiones_ofertas", "auditoria.revision_ofertas_vigente"):
            with self.assertRaises(pg_local.ErrorSQL):
                self.como(NAD, f"select * from {t}")

    # ---------- cola ----------
    def test_cola_solo_desde_la_10_82_y_con_archivo(self):
        r = self.rpc(REV, "public.admin_capturas_pendientes()")
        self.assertEqual(sorted(i["id"] for i in r["items"]), [1, 2, 3, 16])   # 4 es anterior; 5 no tiene archivo
        self.assertEqual(r["pendientes"], {"rojo": 3, "amarillo": 0, "verde": 1})
        self.assertEqual(r["revisadas"], {"rojo": 0, "amarillo": 0, "verde": 0})
        self.assertEqual(r["items"][0]["id"], 16)                              # la más reciente primero
        self.assertEqual(r["items"][0]["nombre"], "ok-rojo-45.62-0.8-3-16-n16.webp")  # n16 no se confunde con n1

    def test_filtro_por_color_y_limite(self):
        r = self.rpc(REV, "public.admin_capturas_pendientes('rojo', 2)")
        self.assertEqual([i["id"] for i in r["items"]], [16, 2])
        self.assertEqual(r["pendientes"]["rojo"], 3)                           # el conteo no depende del límite
        for malo in ("'azul'", "'rojo', 0", "'rojo', 51"):
            self.assertEqual(self.rpc(REV, f"public.admin_capturas_pendientes({malo})"), {"error": "filtro_invalido"})

    # ---------- revisión ----------
    def test_correcta_sale_de_la_cola_y_cuenta_como_revisada(self):
        self.assertIn("id", self.revisar(REV, 2, "correcta"))
        r = self.rpc(REV, "public.admin_capturas_pendientes('rojo', 10)")
        self.assertNotIn(2, [i["id"] for i in r["items"]])
        self.assertEqual((r["pendientes"]["rojo"], r["revisadas"]["rojo"]), (2, 1))
        self.assertEqual(self.q("select autor from auditoria.revisiones_ofertas").startswith("admin_"), True)

    def test_valores_mal_exige_los_tres_valores(self):
        self.assertEqual(self.revisar(REV, 1, "valores_mal", tarifa=45.62)["error"], "revision_invalida")
        self.assertIn("id", self.revisar(REV, 1, "valores_mal", tarifa=45.62, km=6.361, min=19, nota="tomó el primer tramo"))
        self.assertEqual(self.q("select km from auditoria.revisiones_ofertas"), "6.361")

    def test_falso_positivo_exige_motivo_y_no_lleva_valores(self):
        self.assertEqual(self.revisar(REV, 1, "falso_positivo")["error"], "revision_invalida")
        self.assertEqual(self.revisar(REV, 1, "falso_positivo", motivo="otro")["error"], "revision_invalida")
        self.assertEqual(self.revisar(REV, 1, "falso_positivo", motivo="vencida", tarifa=1.0)["error"], "revision_invalida")
        self.assertIn("id", self.revisar(REV, 1, "falso_positivo", motivo="vencida"))
        self.assertEqual(self.revisar(REV, 1, "correcta", motivo="vencida")["error"], "revision_invalida")

    def test_no_acepta_resultados_raros_ni_capturas_inexistentes(self):
        self.assertEqual(self.revisar(REV, 1, "quizas")["error"], "revision_invalida")
        self.assertEqual(self.revisar(REV, 999, "correcta"), {"error": "no_existe"})
        self.assertEqual(self.revisar(REV, 5, "correcta"), {"error": "no_existe"})   # sin archivo en el almacén
        self.assertEqual(self.revisar(REV, 1, "correcta", evidencia_sha256="zz")["error"], "revision_invalida")
        self.assertIn("id", self.revisar(REV, 1, "correcta", evidencia_sha256=SHA))

    def test_anular_devuelve_la_captura_a_la_cola_sin_borrar_nada(self):
        self.revisar(REV, 2, "correcta")
        self.revisar(REV, 2, "anulada")
        r = self.rpc(REV, "public.admin_capturas_pendientes('rojo', 10)")
        self.assertIn(2, [i["id"] for i in r["items"]])
        self.assertEqual(self.q("select count(*) from auditoria.revisiones_ofertas where captura_ok_id = 2"), "2")  # nada se borra

    def test_la_ultima_revision_manda(self):
        self.revisar(REV, 3, "correcta")
        self.revisar(REV, 3, "falso_positivo", motivo="no_oferta")
        self.assertEqual(self.q("select resultado from auditoria.revision_ofertas_vigente where captura_ok_id = 3"), "falso_positivo")

    def test_todo_queda_en_la_bitacora_con_seudonimo(self):
        self.rpc(REV, "public.admin_capturas_pendientes()")
        self.revisar(REV, 1, "correcta")
        acc = self.q("select string_agg(accion || ':' || resultado, ',' order by id) from auditoria.acciones_admin")
        self.assertEqual(acc, "capturas_pendientes:ok,revisar_captura:ok")
        self.assertNotIn(REV, self.q("select string_agg(admin, ',') from auditoria.acciones_admin"))

    def test_down_es_reversible_y_aditivo(self):
        self.pg.archivo(mig("007_revision_ofertas"), db=self.db)         # repetir no falla
        self.pg.archivo(mig("007_revision_ofertas", down=True), db=self.db)
        self.assertEqual(self.q("select to_regclass('auditoria.revisiones_ofertas') is null"), "t")
        self.assertEqual(self.q("select count(*) from public.capturas_ok"), "6")     # no toca la tabla original


if __name__ == "__main__":
    unittest.main()
