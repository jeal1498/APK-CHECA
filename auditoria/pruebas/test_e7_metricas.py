"""E7 — métricas: cada indicador se recalcula en Python desde los datos fuente y debe coincidir con el SQL."""
import json
import random
import statistics
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, U1, U2, evento, mig

ADM = "aaaaaaaa-0000-4000-8000-00000000000a"
NAD = "cccccccc-0000-4000-8000-00000000000c"
D, H = "2026-11-01T00:00:00Z", "2026-11-30T00:00:00Z"


def redondear(x):
    """round() de Postgres para numeric: mitad hacia arriba (Python redondea al par)."""
    from decimal import Decimal, ROUND_HALF_UP
    return int(Decimal(str(x)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def pct(datos, p):
    """percentile_cont de Postgres (interpolación lineal)."""
    x = sorted(datos)
    if not x:
        return None
    k = (len(x) - 1) * p
    f = int(k)
    return x[f] + (x[min(f + 1, len(x) - 1)] - x[f]) * (k - f)


class E7(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        rnd = random.Random(7)
        cls.pg = pg_local.Pg()
        cls.pg.archivo(ESQUEMA)
        cls.pg.sql(LEGADO + f"insert into auth.users (id) values ('{ADM}'), ('{NAD}');")
        for n in ("001_auditoria", "002_registrar", "003_veredictos", "004_admin", "005_metricas"):
            cls.pg.archivo(mig(n))
        cls.pg.sql(f"insert into auditoria.administradores (usuario, rol) values ('{ADM}', 'lector');")
        evs = []
        for i in range(140):
            app = rnd.choice(["uber", "didi"])
            dia = 2 + i % 25
            ts = f"2026-11-{dia:02d}T1{i % 10}:00:01Z"
            r = rnd.random()
            if r < 0.7:
                e = evento(app=app, ts_evento=ts, ts_fotograma=ts.replace(":01Z", ":00Z"), tarifa=round(rnd.uniform(40, 200), 2),
                           km=round(rnd.uniform(1, 20), 2), min=round(rnd.uniform(5, 50), 1), ms_popup=rnd.randint(200, 2000),
                           captura_vinculo="exacto" if rnd.random() < 0.5 else "sin_captura",
                           telefono=rnd.choice(["samsung A15", "xiaomi 13", "motorola g54"]))
                if e["captura_vinculo"] == "exacto":
                    e["captura_sha256"] = f"{i:064x}"
            elif r < 0.85:
                e = evento(app=app, ts_evento=ts, ts_fotograma=ts.replace(":01Z", ":00Z"), resultado="duplicada", motivo="misma_oferta_60s",
                           tarifa=None, km=None, min=None, nivel=None)
            else:
                e = evento(app=app, ts_evento=ts, ts_fotograma=ts.replace(":01Z", ":00Z"), resultado=rnd.choice(["varias", "abstencion"]),
                           motivo="tarjeta_ambigua", tarifa=None, km=None, min=None, nivel=None)
            evs.append(e)
        evs.append(evento(ts_evento="2026-10-05T10:00:01Z", ts_fotograma="2026-10-05T10:00:00Z"))  # fuera de ventana
        cls.evs = evs
        for j in range(0, len(evs), 50):
            cls.pg.sql(f"select public.auditoria_registrar_lote('{U1 if j % 100 == 0 else U2}', $j${json.dumps(evs[j:j + 50])}$j$::jsonb)")
        # Reintentos: 5 eventos se reenvían una vez más.
        cls.pg.sql(f"select public.auditoria_registrar_lote('{U1}', $j${json.dumps(evs[:5])}$j$::jsonb)")
        # Veredictos humanos sobre 40 ofertas: 2 revisores; algunos discrepan, algunos dicen no_oferta / varias.
        cls.refs = {}
        ofertas = [e for e in evs[:-1] if e["resultado"] == "oferta"]
        sql = []
        for k, e in enumerate(ofertas[:40]):
            if k % 10 == 0:
                a = b = ("no_oferta", None, None, None)
            elif k % 10 == 1:
                a = b = ("varias", None, None, None)
            elif k % 10 == 2:
                a = ("oferta", e["tarifa"], e["km"], e["min"]); b = ("oferta", e["tarifa"] + 5, e["km"], e["min"])  # conflicto
            elif k % 10 == 3:
                a = b = ("oferta", e["tarifa"], e["km"] + 1, e["min"])  # lector se equivocó en km
            else:
                a = b = ("oferta", e["tarifa"], e["km"], e["min"])
            for autor, v in (("rev_a", a), ("rev_b", b)):
                vals = ", ".join("null" if x is None else repr(x) for x in v[1:])
                sql.append(f"insert into auditoria.veredictos (lectura_id, fuente, autor, guia_version, clase, tarifa, km, min, estado, evidencia_sha256)"
                           f" values ('{e['id']}', 'humano', '{autor}', '1.0', '{v[0]}', {vals}, 'validado', '{'e' * 64}');")
            cls.refs[e["id"]] = (a, b)
        cls.pg.sql("\n".join(sql))
        e1 = ofertas[46]   # un solo revisor: no es referencia, no entra en la precisión
        cls.uno = e1["id"]
        cls.pg.sql("insert into auditoria.veredictos (lectura_id, fuente, autor, guia_version, clase, estado, evidencia_sha256) values "
                   f"('{e1['id']}', 'humano', 'rev_a', '1.0', 'no_oferta', 'validado', '{'e' * 64}');")
        cls.pg.sql("insert into auditoria.veredictos (lectura_id, fuente, autor, clase) values "
                   f"('{ofertas[45]['id']}', 'ia', 'claude-haiku-5-5', 'no_oferta');")  # IA: no cuenta como referencia

    @classmethod
    def tearDownClass(cls):
        cls.pg.cerrar()

    def metricas(self, app=None, quien=ADM):
        a = "null" if app is None else f"'{app}'"
        out = self.pg.sql(f"select public.admin_metricas('{D}', '{H}', {a})", rol="authenticated",
                          claims={"role": "authenticated", "sub": quien}).strip()
        return json.loads(out)

    def esperado(self, app=None):
        ev = [e for e in self.evs if e["ts_evento"] >= D and e["ts_evento"] < H and (app is None or e["app"] == app)]
        ofe = [e for e in ev if e["resultado"] == "oferta"]
        dup = sum(e["resultado"] == "duplicada" for e in ev)
        absn = sum(e["resultado"] in ("varias", "abstencion") for e in ev)
        lat = [e["ms_popup"] for e in ofe if "ms_popup" in e]
        conref = ok = fp = mez = conf = 0
        for e in ofe:
            if e["id"] not in self.refs:
                continue
            a, b = self.refs[e["id"]]
            if a != b:
                conf += 1
                continue
            conref += 1
            if a[0] == "no_oferta":
                fp += 1
            elif a[0] == "varias":
                mez += 1
            elif abs(e["tarifa"] - a[1]) <= 0.01 and abs(e["km"] - a[2]) <= 0.05 and abs(e["min"] - a[3]) <= 0.5:
                ok += 1
        rei = sum(1 for e in self.evs[:5] if e in ev)
        return {"lecturas": (len(ev), len(ev)), "ofertas_mostradas": (len(ofe), len(ev)), "duplicadas": (dup, len(ofe) + dup),
                "abstenciones": (absn, len(ofe) + absn), "captura_exacta": (sum(e["captura_vinculo"] == "exacto" for e in ofe), len(ofe)),
                "ms_popup_p50": (None, len(lat), redondear(pct(lat, 0.5))), "ms_popup_p90": (None, len(lat), redondear(pct(lat, 0.9))),
                "reintentos": (rei, len(ev)), "con_referencia_humana": (conref, len(ofe)),
                "con_un_solo_revisor": (sum(e["id"] == self.uno for e in ofe), len(ofe)), "precision_humana": (ok, conref),
                "falsos_positivos_humana": (fp, conref), "mezcla_tarjetas_humana": (mez, conref), "conflictos_humanos": (conf, len(ofe))}

    def comprobar(self, app=None):
        m = {x["clave"]: x for x in self.metricas(app)["metricas"]}
        for clave, esp in self.esperado(app).items():
            with self.subTest(app=app, clave=clave):
                self.assertEqual(m[clave]["n"], esp[1], "n")
                if esp[0] is not None:
                    self.assertEqual(m[clave]["k"], esp[0], "k")
                    if clave not in ("lecturas", "ofertas_mostradas", "reintentos", "conflictos_humanos"):
                        self.assertAlmostEqual(float(m[clave]["valor"]), round(esp[0] / esp[1], 4), places=4)
                else:
                    self.assertEqual(float(m[clave]["valor"]), esp[2])
                self.assertEqual(m[clave]["estado"], "sin_datos" if not esp[1] else ("insuficiente" if esp[1] < 30 else "suficiente"))
        return m

    def test_reconciliacion_total_y_por_app(self):
        self.comprobar()
        self.comprobar("uber")
        self.comprobar("didi")

    def test_recall_no_medible_y_acuerdo_ia_separado(self):
        m = {x["clave"]: x for x in self.metricas()["metricas"]}
        self.assertEqual(m["recall"]["estado"], "no_medible")
        self.assertIsNone(m["recall"]["valor"])
        self.assertIn("NO es exactitud", m["acuerdo_ia_lector"]["nombre"])
        self.assertEqual(m["acuerdo_ia_lector"]["grupo"], "acuerdo_ia")
        self.assertIn("NO humanas", m["banco_heredado"]["nombre"])
        self.assertEqual((m["banco_heredado"]["k"], m["banco_heredado"]["n"]), (271, 316))

    def test_ia_no_cuenta_como_referencia(self):
        m = {x["clave"]: x for x in self.metricas()["metricas"]}
        conref = self.esperado()["con_referencia_humana"][0]
        self.assertEqual(m["con_referencia_humana"]["k"], conref)  # la fila de IA no sumó

    def test_segmentos_pequenos_marcados(self):
        r = self.metricas()
        seg = r["segmentos"]
        self.assertTrue(seg)
        self.assertEqual(sum(s["n"] for s in seg), self.esperado()["lecturas"][0])
        self.assertTrue(all(s["suficiente"] == (s["n"] >= 30) for s in seg))
        self.assertTrue(any(not s["suficiente"] for s in seg))
        self.assertEqual(sum(t["lecturas"] for t in r["tendencia"]), self.esperado()["lecturas"][0])

    def test_definiciones_completas(self):
        for x in self.metricas()["metricas"]:
            for campo in ("formula", "poblacion", "fuente", "nombre", "grupo"):
                self.assertTrue(x[campo], (x["clave"], campo))

    def test_acceso_y_bitacora(self):
        self.assertEqual(self.metricas(quien=NAD), {"error": "no_autorizado"})
        with self.assertRaises(pg_local.ErrorSQL):
            self.pg.sql(f"select public.admin_metricas('{D}', '{H}')", rol="anon")
        with self.assertRaises(pg_local.ErrorSQL):
            self.pg.sql(f"select auditoria.metricas('{D}', '{H}')", rol="authenticated", claims={"role": "authenticated", "sub": ADM})
        n = int(self.pg.sql("select count(*) from auditoria.acciones_admin where accion='metricas'").strip())
        self.metricas()
        self.assertEqual(int(self.pg.sql("select count(*) from auditoria.acciones_admin where accion='metricas'").strip()), n + 1)

    def test_sin_datos_no_inventa_valores(self):
        out = json.loads(self.pg.sql("select public.admin_metricas('2025-01-01', '2025-02-01')", rol="authenticated",
                                     claims={"role": "authenticated", "sub": ADM}).strip())
        m = {x["clave"]: x for x in out["metricas"]}
        for c in ("duplicadas", "precision_humana", "captura_exacta", "ms_popup_p50"):
            self.assertEqual(m[c]["estado"], "sin_datos", c)
            self.assertIsNone(m[c]["valor"], c)

    def test_reversion(self):
        self.pg.sql("create database rev template prueba", db="postgres")
        self.pg.archivo(mig("005_metricas", True), db="rev")
        self.assertEqual(self.pg.sql("select count(*) from pg_proc where proname = 'admin_metricas'", db="rev").strip(), "0")
        self.pg.archivo(mig("005_metricas"), db="rev")


if __name__ == "__main__":
    unittest.main()
