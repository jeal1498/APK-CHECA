"""E4 — veredictos humano / IA / heredado en Postgres LOCAL (no Supabase)."""
import json
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, U1, evento, huella_publica, mig

H = "c" * 64
EXTRA = f"""
insert into public.errores_lector (id, usuario, app, version, lineas, local, ia, tipo, veredicto) values
 (9001, '{U1}', 'DiDi', '10.77', '[]', '{{"tarifa":50,"km":5,"min":12}}', '{{"tarifa":55,"km":5,"min":12}}', 'diferencia', 'ia_bien'),
 (9002, '{U1}', 'Uber', '10.77', '[]', '{{"tarifa":40,"km":3,"min":9}}',  '{{"tarifa":44,"km":3,"min":9}}',  'diferencia', 'ia_mal'),
 (9003, '{U1}', 'Uber', '10.77', '[]', '{{"tarifa":40,"km":3,"min":9}}',  '{{"oferta":false}}',              'diferencia', 'ia_bien'),
 (9004, '{U1}', 'Uber', '10.77', '[]', '{{"tarifa":40,"km":3,"min":9}}',  '{{"tarifa":41}}',                 'diferencia', 'dudoso');
"""


class E4(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + EXTRA)
        cls.n = 0

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def setUp(self):
        E4.n += 1
        self.db = f"v{E4.n}"
        self.pg.sql(f"create database {self.db} template prueba", db="postgres")
        for n in ("001_auditoria", "002_registrar", "003_veredictos"):
            self.pg.archivo(mig(n), db=self.db)

    def q(self, s, **k):
        return self.pg.sql(s, db=self.db, **k).strip()

    def humano(self, objetivo, autor, clase="oferta", tarifa=50, km=5, min_=12, estado="validado", ev=H, guia="1.0", **k):
        cols = {"caso_banco": None, "error_lector_id": None, "lectura_id": None}
        cols.update(objetivo)
        k = {x: y for x, y in k.items() if y is not None}
        datos = dict(cols, fuente="humano", autor=autor, clase=clase, tarifa=tarifa if clase == "oferta" else None,
                     km=km if clase == "oferta" else None, min=min_ if clase == "oferta" else None, estado=estado,
                     evidencia_sha256=ev, guia_version=guia, **k)
        c = ", ".join(datos)
        v = ", ".join("null" if x is None else (str(x) if isinstance(x, (int, float)) else f"'{x}'") for x in datos.values())
        return self.q(f"insert into auditoria.veredictos ({c}) values ({v}) returning id")

    def vigente(self, where):
        return self.q(f"select estado, n_humanos, coalesce(clase,'-'), coalesce(tarifa::text,'-') from auditoria.veredicto_vigente where {where}")

    # ---------- importación heredada ----------
    def test_importacion_marca_heredado_y_no_toca_el_original(self):
        n = self.q("select count(*) from public.errores_lector where veredicto is not null")
        self.assertEqual(self.q("select count(*) from auditoria.veredictos where fuente='heredado' and origen_migracion='003'"), n)
        self.assertEqual(self.q("select count(*) from auditoria.veredictos where fuente <> 'heredado'"), "0")
        self.assertEqual(self.q("select count(*) from auditoria.veredictos where estado = 'validado'"), "0")
        self.assertEqual(self.vigente("error_lector_id = 9001"), "heredado_no_revisado|0|-|-")

    def test_importacion_traduce_segun_el_veredicto(self):
        f = lambda i: self.q(f"select clase, coalesce(tarifa::text,'-') from auditoria.veredictos where error_lector_id={i}")
        self.assertEqual(f(9001), "oferta|55.00")      # ia_bien → datos de la IA
        self.assertEqual(f(9002), "oferta|40.00")      # ia_mal → datos del lector
        self.assertEqual(f(9003), "no_oferta|-")
        self.assertEqual(f(9004), "ilegible|-")        # dudoso no se fuerza a una clase

    def test_importacion_idempotente_y_reversible(self):
        antes = huella_publica(self.pg, self.db)
        n = self.q("select count(*) from auditoria.veredictos")
        self.pg.archivo(mig("003_veredictos"), db=self.db)
        self.assertEqual(self.q("select count(*) from auditoria.veredictos"), n)
        self.pg.archivo(mig("003_veredictos", True), db=self.db)
        self.assertEqual(self.q("select count(*) from pg_tables where schemaname='auditoria' and tablename='veredictos'"), "0")
        self.assertEqual(huella_publica(self.pg, self.db), antes)
        self.pg.archivo(mig("003_veredictos"), db=self.db)
        self.assertEqual(self.q("select count(*) from auditoria.veredictos"), n)

    # ---------- reglas ----------
    def test_ia_y_heredado_no_pueden_validar(self):
        for fuente in ("ia", "heredado"):
            with self.subTest(fuente):
                with self.assertRaises(pg_local.ErrorSQL):
                    self.q(f"insert into auditoria.veredictos (caso_banco, fuente, autor, clase, estado, origen_migracion) "
                           f"values ('0002-uber', '{fuente}', 'x', 'no_oferta', 'validado', 'x')")

    def test_humano_valida_solo_con_captura_y_guia(self):
        with self.assertRaises(pg_local.ErrorSQL):
            self.humano({"caso_banco": "0002-uber"}, "rev1", ev=None)
        with self.assertRaises(pg_local.ErrorSQL):
            self.humano({"caso_banco": "0002-uber"}, "rev1", guia=None)
        self.humano({"caso_banco": "0002-uber"}, "rev1", ev=None, estado="propuesto")  # propuesto sí

    def test_un_solo_objetivo_y_datos_coherentes(self):
        with self.assertRaises(pg_local.ErrorSQL):
            self.humano({"caso_banco": "0002-uber", "error_lector_id": 9001}, "rev1")
        with self.assertRaises(pg_local.ErrorSQL):
            self.q("insert into auditoria.veredictos (caso_banco, fuente, autor, clase) values ('0002-uber','ia','m','oferta')")
        with self.assertRaises(pg_local.ErrorSQL):
            self.q("insert into auditoria.veredictos (caso_banco, fuente, autor, clase, tarifa) values ('0002-uber','ia','m','varias', 10)")

    def test_no_se_editan_ni_se_borran(self):
        i = self.humano({"caso_banco": "0002-uber"}, "rev1")
        for s in (f"update auditoria.veredictos set clase='no_oferta' where id={i}", f"delete from auditoria.veredictos where id={i}"):
            with self.subTest(s):
                with self.assertRaises(pg_local.ErrorSQL) as e:
                    self.q(s)
                self.assertIn("no se", str(e.exception))

    def test_cascada_desde_la_lectura_si_borra(self):
        e = evento()
        self.q(f"select public.auditoria_registrar_lote('{U1}', $j${json.dumps([e])}$j$::jsonb)")
        self.humano({"lectura_id": e["id"]}, "rev1")
        self.q(f"delete from auth.users where id = '{U1}'")
        self.assertEqual(self.q(f"select count(*) from auditoria.veredictos where lectura_id = '{e['id']}'"), "0")

    def test_mismo_revisor_no_repite_sin_sustituir(self):
        i = self.humano({"caso_banco": "0002-uber"}, "rev1")
        with self.assertRaises(pg_local.ErrorSQL):
            self.humano({"caso_banco": "0002-uber"}, "rev1")
        self.humano({"caso_banco": "0002-uber"}, "rev1", tarifa=51, sustituye=i)
        self.assertEqual(self.vigente("caso_banco='0002-uber'"), "referencia_humana|1|oferta|51.00")

    # ---------- precedencia ----------
    def test_precedencia_humano_sobre_ia(self):
        self.q("insert into auditoria.veredictos (caso_banco, fuente, autor, revisor_version, clase) values ('0004-uber','ia','claude-haiku-5-5','prompt-v77','no_oferta')")
        self.assertEqual(self.vigente("caso_banco='0004-uber'"), "solo_ia|0|-|-")
        self.humano({"caso_banco": "0004-uber"}, "rev1", tarifa=48.73)
        self.assertEqual(self.vigente("caso_banco='0004-uber'"), "referencia_humana|1|oferta|48.73")
        self.humano({"caso_banco": "0004-uber"}, "rev2", tarifa=48.73)
        self.assertEqual(self.vigente("caso_banco='0004-uber'"), "referencia_doble|2|oferta|48.73")

    def test_desacuerdo_humano_no_se_fuerza(self):
        self.humano({"caso_banco": "0006-uber"}, "rev1", tarifa=50)
        self.humano({"caso_banco": "0006-uber"}, "rev2", tarifa=60)
        self.assertEqual(self.vigente("caso_banco='0006-uber'"), "conflicto_humano|2|-|-")

    def test_a3_un_mismo_revisor_no_suma_dos_votos(self):
        i = self.humano({"caso_banco": "0012-uber"}, "rev1")
        self.humano({"caso_banco": "0012-uber"}, "rev1", sustituye=i)
        self.humano({"caso_banco": "0012-uber"}, "rev1", sustituye=i)  # dos sustitutos del mismo veredicto
        self.assertEqual(self.vigente("caso_banco='0012-uber'").split("|")[:2], ["referencia_humana", "1"])

    def test_a3_tolerancias_de_la_guia(self):
        self.humano({"caso_banco": "0013-uber"}, "rev1", km=5.00, min_=12.0)
        self.humano({"caso_banco": "0013-uber"}, "rev2", km=5.04, min_=12.4)
        self.assertEqual(self.vigente("caso_banco='0013-uber'"), "referencia_doble|2|oferta|50.00")
        self.humano({"caso_banco": "0015-uber"}, "rev1", km=5.00)
        self.humano({"caso_banco": "0015-uber"}, "rev2", km=5.10)
        self.assertEqual(self.vigente("caso_banco='0015-uber'"), "conflicto_humano|2|-|-")
        self.humano({"caso_banco": "0017-uber"}, "rev1", tarjeta=1)
        self.humano({"caso_banco": "0017-uber"}, "rev2", tarjeta=2)
        self.assertEqual(self.vigente("caso_banco='0017-uber'").split("|")[0], "conflicto_humano")
        self.humano({"caso_banco": "0020-uber"}, "rev1", clase="varias")
        self.humano({"caso_banco": "0020-uber"}, "rev2", clase="no_oferta")
        self.assertEqual(self.vigente("caso_banco='0020-uber'").split("|")[0], "conflicto_humano")

    def test_adjudicacion_resuelve_un_conflicto_y_no_es_doble_ciega(self):
        self.humano({"caso_banco": "0022-uber"}, "rev1", km=5.0)
        self.humano({"caso_banco": "0022-uber"}, "rev2", km=6.0)
        self.assertEqual(self.vigente("caso_banco='0022-uber'").split("|")[0], "conflicto_humano")
        self.humano({"caso_banco": "0022-uber"}, "consenso", km=5.5, revisor_version="adjudicacion:consenso")
        self.assertEqual(self.vigente("caso_banco='0022-uber'"), "referencia_adjudicada|2|oferta|50.00")
        self.assertEqual(self.q("select km from auditoria.veredicto_vigente where caso_banco='0022-uber'"), "5.50")   # valen los datos de la adjudicación

    def test_adjudicacion_sin_validar_o_de_ia_no_cuenta(self):
        self.humano({"caso_banco": "0024-uber"}, "rev1", km=5.0)
        self.humano({"caso_banco": "0024-uber"}, "rev2", km=6.0)
        self.humano({"caso_banco": "0024-uber"}, "consenso", km=5.5, estado="propuesto", ev=None, revisor_version="adjudicacion:consenso")
        self.assertEqual(self.vigente("caso_banco='0024-uber'").split("|")[0], "conflicto_humano")
        with self.assertRaises(pg_local.ErrorSQL):   # la IA nunca queda validada, tampoco como adjudicadora
            self.q("insert into auditoria.veredictos (caso_banco, fuente, autor, revisor_version, clase, estado) values ('0024-uber','ia','m','adjudicacion:x','no_oferta','validado')")

    def test_adjudicacion_sin_conflicto_previo_tambien_es_referencia(self):
        self.humano({"caso_banco": "0026-uber"}, "consenso", revisor_version="adjudicacion:tercera_persona")
        self.assertEqual(self.vigente("caso_banco='0026-uber'").split("|")[0], "referencia_adjudicada")

    def test_propuesto_o_descartado_no_es_referencia(self):
        self.humano({"caso_banco": "0010-uber"}, "rev1", estado="propuesto")
        self.assertEqual(self.vigente("caso_banco='0010-uber'").split("|")[0], "heredado_no_revisado")

    def test_permisos(self):
        for rol in ("anon", "authenticated"):
            with self.subTest(rol):
                with self.assertRaises(pg_local.ErrorSQL):
                    self.q("select * from auditoria.veredicto_vigente", rol=rol, claims={"role": rol, "sub": U1})


if __name__ == "__main__":
    unittest.main()
