"""008 — Sugerencia de la IA con la revisión y su precisión contra el veredicto humano (Postgres LOCAL)."""
import json
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, mig

REV = "bbbbbbbb-0000-4000-8000-00000000000b"
ADM = "aaaaaaaa-0000-4000-8000-00000000000a"
NAD = "cccccccc-0000-4000-8000-00000000000c"


def ia(estado="vigente", t=45.62, k=0.8, m=3):
    return json.dumps({"estado": estado, "tarifa": t, "km": k, "min": m, "confianza": "alta"})


class E9(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{REV}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin", "007_revision_ofertas", "008_ia_sugerencia", "009_cola_por_version"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector'), ('{REV}', 'revisor');")
        cls.pg.sql("delete from public.capturas_ok")
        for i in range(1, 8):
            cls.pg.sql(f"insert into public.capturas_ok (id, usuario, app, moneda, version, lector, lineas, local, ia, nivel, creado) values "
                       f"({i}, '{NAD}', 'didi', 'MXN', '10.80', 'x', '[\"a\"]', '{{}}', '{{}}', 'rojo', '2026-11-18T1{i}:00:00Z')")
            cls.pg.sql(f"insert into storage.objects (bucket_id, name) values ('capturas', 'ok-rojo-45.62-0.8-3-{i}-n{i}.webp')")
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        E9.n += 1
        self.db = f"s{E9.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def como(self, u, s):
        return self.pg.sql(s, db=self.db, rol="authenticated", claims={"role": "authenticated", "sub": u}).strip()

    def rpc(self, u, s):
        return json.loads(self.como(u, "select " + s))

    def q(self, s):
        return self.pg.sql(s, db=self.db).strip()

    def revisar(self, cid, res, sug=None, **kw):
        extra = "".join(f", p_{k} => {v}" if not isinstance(v, str) else f", p_{k} => '{v}'" for k, v in kw.items())
        s = f", p_ia_sugerencia => $j${sug}$j$::jsonb" if sug else ""
        return self.rpc(REV, f"public.admin_revisar_captura({cid}, '{res}'{extra}{s})")

    def precision(self):
        return self.rpc(REV, "public.admin_ia_precision()")

    def test_guarda_la_sugerencia_y_exige_que_sea_objeto(self):
        self.assertIn("id", self.revisar(1, "correcta", ia()))
        self.assertEqual(json.loads(self.q("select ia_sugerencia from auditoria.revisiones_ofertas"))["estado"], "vigente")
        self.assertEqual(self.revisar(2, "correcta", "[1]")["error"], "revision_invalida")

    def test_sin_sugerencia_no_cuenta_en_la_precision(self):
        self.revisar(1, "correcta")
        self.assertEqual(self.precision()["n"], 0)

    def test_acierto_cuando_la_ia_coincide_con_el_pop_up_en_una_correcta(self):
        self.revisar(1, "correcta", ia(k=0.76, m=3))            # km dentro de ±0.15 del 0.8 del archivo
        p = self.precision()
        self.assertEqual((p["n"], p["aciertos"], p["tarifa_ok"], p["km_ok"], p["min_ok"]), (1, 1, 1, 1, 1))

    def test_falla_cuando_difiere_y_cuenta_por_campo(self):
        self.revisar(1, "correcta", ia(k=6.361, m=19))
        p = self.precision()
        self.assertEqual((p["aciertos"], p["tarifa_ok"], p["km_ok"], p["min_ok"]), (0, 1, 0, 0))

    def test_valores_mal_usa_los_valores_de_la_persona(self):
        self.revisar(1, "valores_mal", ia(k=6.361, m=19), tarifa=45.62, km=6.361, min=19)
        self.assertEqual(self.precision()["aciertos"], 1)

    def test_falso_positivo_se_detecta_si_la_ia_no_dice_vigente(self):
        self.revisar(1, "falso_positivo", ia("vencida", None, None, None), motivo="vencida")
        self.revisar(2, "falso_positivo", ia("vigente"), motivo="no_oferta")
        p = self.precision()
        self.assertEqual((p["n"], p["falsos_positivos"], p["falsos_positivos_detectados"], p["aciertos"], p["ofertas"]), (2, 2, 1, 1, 0))

    def test_usa_solo_la_revision_vigente(self):
        self.revisar(1, "correcta", ia())
        self.revisar(1, "anulada")
        self.assertEqual(self.precision()["n"], 0)
        self.revisar(1, "falso_positivo", ia("sin_oferta", None, None, None), motivo="no_oferta")
        self.assertEqual((self.precision()["n"], self.precision()["aciertos"]), (1, 1))

    def test_acceso(self):
        self.assertEqual(self.rpc(NAD, "public.admin_ia_precision()"), {"error": "no_autorizado"})
        self.assertEqual(self.precision()["minimo"], 30)
        with self.assertRaises(pg_local.ErrorSQL):
            self.pg.sql("select public.admin_ia_precision()", db=self.db, rol="anon")

    def test_007_sigue_funcionando_y_down_restaura(self):
        self.pg.archivo(mig("008_ia_sugerencia"), db=self.db)           # repetir no falla
        self.assertIn("id", self.revisar(3, "correcta"))                # la firma nueva acepta llamadas sin sugerencia
        self.pg.archivo(mig("008_ia_sugerencia", down=True), db=self.db)
        self.assertEqual(self.q("select count(*) from information_schema.columns where table_name='revisiones_ofertas' and column_name='ia_sugerencia'"), "0")
        self.assertIn("id", self.rpc(REV, "public.admin_revisar_captura(4, 'correcta')"))   # vuelve la firma de 007
        self.assertEqual(self.q("select count(*) from auditoria.revisiones_ofertas"), "2")   # no se borró ninguna revisión


if __name__ == "__main__":
    unittest.main()
