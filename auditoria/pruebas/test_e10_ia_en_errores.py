"""010 — La sugerencia de la IA de imagen se guarda con el veredicto de un error y se mide junto al Lector y la IA de texto (Postgres LOCAL)."""
import json
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, mig

REV = "bbbbbbbb-0000-4000-8000-00000000000b"
ADM = "aaaaaaaa-0000-4000-8000-00000000000a"
NAD = "cccccccc-0000-4000-8000-00000000000c"
SHA = "a" * 64


def vis(estado="vigente", t=45.62, k=6.361, m=19):
    return json.dumps({"estado": estado, "tarifa": t, "km": k, "min": m, "confianza": "alta"})


class E10(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{REV}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin", "010_ia_en_errores"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector'), ('{REV}', 'revisor');")
        # Error 1: el Lector leyó mal los minutos y la IA de texto también; la verdad es 45.62 · 6.361 · 19.
        # Error 2: lector e IA de texto aciertan (50 · 5 · 10). Error 3: no era una oferta.
        cls.pg.sql("""insert into public.errores_lector (id, usuario, app, version, lineas, local, ia, tipo) values
          (1001, '%s', 'DiDi', '10.82', '["a"]', '{"tarifa":45.62,"km":9.5,"min":95}', '{"oferta":true,"tarifa":45.62,"km":9.5,"min":95}', 'diferencia'),
          (1002, '%s', 'DiDi', '10.82', '["a"]', '{"tarifa":50,"km":5,"min":10}', '{"oferta":true,"tarifa":50,"km":5,"min":10}', 'diferencia'),
          (1003, '%s', 'Uber', '10.82', '["a"]', '{"tarifa":30,"km":3,"min":8}', '{"oferta":false}', 'diferencia'),
          (1004, '%s', 'Uber', '10.82', '["a"]', '{"tarifa":30,"km":3,"min":8}', '{"oferta":true,"tarifa":30,"km":3,"min":8}', 'diferencia')""" % (NAD, NAD, NAD, NAD))
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        E10.n += 1
        self.db = f"e{E10.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")

    def como(self, u, s):
        return self.pg.sql(s, db=self.db, rol="authenticated", claims={"role": "authenticated", "sub": u}).strip()

    def rpc(self, u, s):
        return json.loads(self.como(u, "select " + s))

    def q(self, s):
        return self.pg.sql(s, db=self.db).strip()

    def veredicto(self, eid, clase, sug=None, **kw):
        extra = "".join(f", {k} => {v}" if not isinstance(v, str) else f", {k} => '{v}'" for k, v in kw.items())
        s = f", ia_sugerencia => $j${sug}$j$::jsonb" if sug else ""
        return self.rpc(REV, f"public.admin_veredicto('error_lector', '{eid}', '{clase}', evidencia_sha256 => '{SHA}'{extra}{s})")

    def precision(self):
        return self.rpc(REV, "public.admin_ia_precision_errores()")

    def test_guarda_la_sugerencia_y_exige_objeto(self):
        self.assertIn("id", self.veredicto(1001, "oferta", vis(), tarifa=45.62, km=6.361, min=19))
        self.assertEqual(json.loads(self.q("select ia_sugerencia from auditoria.veredictos"))["estado"], "vigente")
        self.assertEqual(self.veredicto(1002, "oferta", "[1]", tarifa=50, km=5, min=10)["error"], "veredicto_invalido")

    def test_veredicto_sin_sugerencia_sigue_funcionando_y_no_cuenta(self):
        self.assertIn("id", self.veredicto(1002, "oferta", tarifa=50, km=5, min=10))
        self.assertEqual(self.precision()["n"], 0)

    def test_compara_vision_lector_y_texto_en_los_mismos_casos(self):
        self.veredicto(1001, "oferta", vis(), tarifa=45.62, km=6.361, min=19)            # visión acierta; lector y texto fallan
        self.veredicto(1002, "oferta", vis(t=50, k=5, m=10), tarifa=50, km=5, min=10)     # los tres aciertan
        self.veredicto(1003, "no_oferta", vis("sin_oferta", None, None, None))            # visión acierta (no era oferta)
        self.veredicto(1004, "oferta", vis("vencida", None, None, None), tarifa=30, km=3, min=8)  # visión falla; lector y texto aciertan
        p = self.precision()
        self.assertEqual((p["n"], p["ofertas"]), (4, 3))
        self.assertEqual((p["vision"], p["vision_ofertas"]), (3, 2))
        self.assertEqual((p["lector_ofertas"], p["texto_ofertas"]), (2, 2))
        self.assertEqual(p["minimo"], 30)

    def test_usa_el_ultimo_veredicto_por_error(self):
        self.assertIn("id", self.veredicto(1001, "oferta", vis(k=9.5, m=95), tarifa=45.62, km=9.5, min=95))
        self.assertIn("id", self.veredicto(1001, "oferta", vis(), tarifa=45.62, km=6.361, min=19))   # corrige: sustituye al anterior
        self.assertEqual(self.q("select count(*) from auditoria.veredictos where fuente = 'humano' and sustituye is not null"), "1")
        self.assertEqual(self.q("select clase || ':' || km from auditoria.veredicto_vigente where error_lector_id = 1001"), "oferta:6.36")   # la vista redondea a 2 decimales
        p = self.precision()
        self.assertEqual((p["n"], p["vision"], p["lector_ofertas"]), (1, 1, 0))

    def test_acceso(self):
        self.assertEqual(self.rpc(NAD, "public.admin_ia_precision_errores()"), {"error": "no_autorizado"})
        self.assertEqual(self.precision()["n"], 0)
        with self.assertRaises(pg_local.ErrorSQL):
            self.pg.sql("select public.admin_ia_precision_errores()", db=self.db, rol="anon")
        self.assertEqual(self.rpc(ADM, "public.admin_veredicto('error_lector', '1001', 'oferta', evidencia_sha256 => '%s')" % SHA), {"error": "no_autorizado"})

    def test_repetir_y_revertir(self):
        self.pg.archivo(mig("010_ia_en_errores"), db=self.db)
        self.assertIn("id", self.veredicto(1001, "oferta", vis(), tarifa=45.62, km=6.361, min=19))
        self.pg.archivo(mig("010_ia_en_errores", down=True), db=self.db)
        self.assertEqual(self.q("select count(*) from information_schema.columns where table_name='veredictos' and column_name='ia_sugerencia'"), "0")
        r = self.rpc(REV, "public.admin_veredicto('error_lector', '1002', 'oferta', 50, 5, 10, null, 'media', '%s')" % SHA)   # vuelve la firma de 004
        self.assertIn("id", r)
        self.assertEqual(self.q("select count(*) from auditoria.veredictos where fuente = 'humano'"), "2")   # no se perdió ninguno


if __name__ == "__main__":
    unittest.main()
