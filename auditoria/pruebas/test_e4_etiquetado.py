"""E4 — herramientas de etiquetado humano y métricas (datos sintéticos; sin capturas reales)."""
import contextlib
import csv
import io
import json
import os
import shutil
import sys
import tempfile
import unittest

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
sys.path.insert(0, os.path.join(RAIZ, "auditoria", "etiquetado"))
import etiquetado  # noqa: E402
import metricas  # noqa: E402
import origen_banco  # noqa: E402
import pg_local  # noqa: E402
from test_migraciones import ESQUEMA, LEGADO, mig  # noqa: E402


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="e4-")
        self.addCleanup(shutil.rmtree, self.tmp, True)
        self.casos = origen_banco.clasificar()
        # Manifiesto sintético: capturas para los casos de id par (como si el resto ya hubiera expirado).
        self.man = os.path.join(self.tmp, "manifiesto.csv")
        with open(self.man, "w", newline="") as f:
            w = csv.writer(f, lineterminator="\n")
            w.writerow(["tipo", "nombre", "bytes", "sha256", "origen", "id_enlace", "version", "filas"])
            for c in self.casos:
                i = int(c["caso"][:4])
                if i % 2 == 0:
                    w.writerow(["captura", f"capturas/{i}.webp", 10, f"{i:064x}", "dudosa", i, "10.77", ""])

    def correr(self, *a):
        with contextlib.redirect_stdout(io.StringIO()):
            return etiquetado.main(list(a))

    def hoja(self, n=40, semilla=7):
        d = os.path.join(self.tmp, f"h{semilla}")
        self.correr("hoja", "--manifiesto", self.man, "--n", str(n), "--semilla", str(semilla), "--salida", d)
        return d

    def llenar(self, d, rev, f):
        filas = list(csv.DictReader(open(os.path.join(d, f"hoja-{rev}.csv"))))
        for r in filas:
            r.update(f(r))
        ruta = os.path.join(self.tmp, f"lleno-{rev}.csv")
        with open(ruta, "w", newline="") as g:
            w = csv.DictWriter(g, fieldnames=etiquetado.CAMPOS_HOJA, lineterminator="\n")
            w.writeheader(); w.writerows(filas)
        return ruta


class TestOrigen(Base):
    def test_banco_completo_sin_verdad_humana(self):
        self.assertEqual(len(self.casos), 316)
        self.assertTrue(all(c["humano_verificado"] == "no" for c in self.casos))
        self.assertEqual(sum(c["circular"] == "si" for c in self.casos), 45)
        self.assertNotIn("desconocido", {c["origen"] for c in self.casos})

    def test_csv_versionado_coincide(self):
        en_repo = list(csv.DictReader(open(os.path.join(RAIZ, "auditoria", "etiquetado", "banco_origen.csv"))))
        self.assertEqual(en_repo, self.casos)


class TestHoja(Base):
    def test_sin_respaldo_e0_basta_una_carpeta_de_capturas(self):
        """Sin respaldo E0 la hoja y la importación funcionan con la carpeta de capturas de la revisión."""
        cap = os.path.join(self.tmp, "capturas"); os.makedirs(cap)
        ids = [int(c["caso"][:4]) for c in self.casos][:40]
        for i in ids:
            open(os.path.join(cap, f"{i}.webp"), "wb").write(b"RIFF\x00\x00\x00\x00WEBP" + str(i).encode())
        open(os.path.join(cap, "ok-verde-1-2-3-4.webp"), "wb").write(b"x")   # las normales no entran
        os.symlink(os.path.join(cap, f"{ids[0]}.webp"), os.path.join(cap, "99999.webp"))  # un enlace no cuenta
        caps = etiquetado.capturas_de_manifiesto(cap)
        self.assertEqual(sorted(caps), sorted(ids))
        import hashlib
        self.assertEqual(caps[ids[0]][1], hashlib.sha256(open(os.path.join(cap, f"{ids[0]}.webp"), "rb").read()).hexdigest())
        d = os.path.join(self.tmp, "hc")
        self.correr("hoja", "--manifiesto", cap, "--n", "20", "--salida", d)
        filas = list(csv.DictReader(open(os.path.join(d, "hoja-revisor_a.csv"))))
        self.assertTrue(filas and all(int(r["caso"][:4]) in ids for r in filas))
        llenar = lambda rev: self.llenar(d, rev, lambda r: {"clase": "no_oferta"})
        out = os.path.join(self.tmp, "oc")
        self.correr("importar", "--manifiesto", cap, "--hoja", llenar("revisor_a"), "--hoja", llenar("revisor_b"), "--salida", out)
        self.assertEqual(json.load(open(os.path.join(out, "acuerdo.json")))["casos_comunes"], len(filas))
        # Si la captura cambia después de etiquetar, la fila deja de valer.
        open(os.path.join(cap, f"{filas[0]['caso'][:4].lstrip('0')}.webp"), "ab").write(b"!")
        out2 = os.path.join(self.tmp, "oc2")
        self.correr("importar", "--manifiesto", cap, "--hoja", llenar("revisor_a"), "--hoja", llenar("revisor_b"), "--salida", out2)
        ac = json.load(open(os.path.join(out2, "acuerdo.json")))
        self.assertEqual(ac["casos_comunes"], len(filas) - 1)
        self.assertIn("captura_no_coincide_con_manifiesto", {e["error"] for e in ac["errores_de_hoja"]})

    def test_hoja_ciega_determinista_y_solo_con_captura(self):
        d1, d2 = self.hoja(semilla=7), self.hoja(semilla=7)
        a = open(os.path.join(d1, "hoja-revisor_a.csv")).read()
        self.assertEqual(a, open(os.path.join(d2, "hoja-revisor_a.csv")).read())
        filas = list(csv.DictReader(io.StringIO(a)))
        self.assertGreaterEqual(len(filas), 30)
        self.assertTrue(all(int(r["caso"][:4]) % 2 == 0 for r in filas))
        self.assertTrue(all(r["clase"] == r["tarifa"] == r["km"] == r["min"] == "" for r in filas))  # ciega
        self.assertEqual(list(filas[0].keys()), etiquetado.CAMPOS_HOJA)  # sin columnas del lector ni de la IA
        info = json.load(open(os.path.join(d1, "muestra.json")))
        self.assertGreater(info["casos_sin_captura_excluidos"], 0)
        self.assertNotEqual(a, open(os.path.join(self.hoja(semilla=8), "hoja-revisor_a.csv")).read())

    def test_revisor_con_nombre_o_correo_se_rechaza(self):
        with self.assertRaises(SystemExit):
            self.correr("hoja", "--manifiesto", self.man, "--revisor", "Juan Pérez", "--salida", os.path.join(self.tmp, "x"))


class TestImportar(Base):
    def _par(self, cambio_b=lambda r: {}):
        d = self.hoja()
        base = lambda r: {"clase": "oferta", "tarifa": "50.00", "km": "5.0", "min": "12", "confianza": "alta"}
        a = self.llenar(d, "revisor_a", base)
        b = self.llenar(d, "revisor_b", lambda r: {**base(r), **cambio_b(r)})
        out = os.path.join(self.tmp, "out")
        self.correr("importar", "--manifiesto", self.man, "--hoja", a, "--hoja", b, "--salida", out)
        return out, json.load(open(os.path.join(out, "acuerdo.json")))

    def test_acuerdo_total(self):
        out, ac = self._par()
        self.assertEqual(ac["acuerdo_total"]["valor"], 1.0)
        self.assertEqual(ac["kappa_clase"], 1.0)
        self.assertEqual(len(open(os.path.join(out, "conflictos.csv")).read().splitlines()), 1)

    def test_desacuerdos_se_separan_no_se_promedian(self):
        out, ac = self._par(lambda r: {"tarifa": "60.00"} if int(r["caso"][:4]) % 4 == 0 else {})
        conf = list(csv.DictReader(open(os.path.join(out, "conflictos.csv"))))
        ref = list(csv.DictReader(open(os.path.join(out, "referencia_humana.csv"))))
        self.assertTrue(conf and ref)
        self.assertEqual(len(conf) + len(ref), ac["casos_comunes"])
        self.assertTrue(all(int(r["caso"][:4]) % 4 == 0 for r in conf))
        self.assertLess(ac["acuerdo_por_campo"]["tarifa"]["valor"], 1.0)
        self.assertEqual(ac["acuerdo_por_campo"]["km"]["valor"], 1.0)

    def _hojas(self, cambio_b=lambda r: {}, sha_b=None):
        d = self.hoja()
        base = lambda r: {"clase": "oferta", "tarifa": "50.00", "km": "5.0", "min": "12", "confianza": "alta"}
        a = self.llenar(d, "revisor_a", base)
        b = self.llenar(d, "revisor_b", lambda r: {**base(r), **cambio_b(r)})
        return d, a, b

    def test_app_distinta_entre_revisores_es_desacuerdo_no_casos_distintos(self):
        """Antes se emparejaba por «id-app»: si uno marcaba DiDi y otro Uber, no se comparaban."""
        d, a, b = self._hojas()
        # cambia el sufijo de app en el caso del revisor B (así lo exporta la página según la app elegida)
        filas = list(csv.DictReader(open(b)))
        for r in filas:
            r["caso"] = r["caso"][:4] + ("-didi" if r["caso"].endswith("uber") else "-uber")   # la app contraria
        with open(b, "w", newline="") as g:
            w = csv.DictWriter(g, fieldnames=etiquetado.CAMPOS_HOJA, lineterminator="\n"); w.writeheader(); w.writerows(filas)
        out = os.path.join(self.tmp, "oapp")
        self.correr("importar", "--manifiesto", self.man, "--hoja", a, "--hoja", b, "--salida", out)
        ac = json.load(open(os.path.join(out, "acuerdo.json")))
        self.assertEqual(ac["solo_uno"], 0)
        self.assertEqual(ac["casos_comunes"], len(filas))          # se compararon todos
        self.assertEqual(ac["acuerdo_total"]["k"], 0)               # y todos discrepan en la app
        self.assertEqual(len(list(csv.DictReader(open(os.path.join(out, "conflictos.csv"))))), len(filas))

    def test_importar_sin_carpeta_de_capturas(self):
        d, a, b = self._hojas()
        out = os.path.join(self.tmp, "osin")
        self.correr("importar", "--sin-capturas", "--hoja", a, "--hoja", b, "--salida", out)
        ac = json.load(open(os.path.join(out, "acuerdo.json")))
        self.assertEqual(ac["acuerdo_total"]["valor"], 1.0)
        self.assertEqual(ac["errores_de_hoja"], [])
        with self.assertRaises(SystemExit):   # sin carpeta y sin la opción: se niega
            self.correr("importar", "--hoja", a, "--hoja", b, "--salida", os.path.join(self.tmp, "ox"))

    def test_sin_carpeta_exige_el_mismo_sha_entre_revisores_y_forma_valida(self):
        d, a, b = self._hojas()
        filas = list(csv.DictReader(open(b)))
        filas[0]["captura_sha256"] = "f" * 64            # B vio «otra» imagen con el mismo nombre
        filas[1]["captura_sha256"] = "xyz"               # sha con forma inválida
        filas[2]["captura"] = "9999.webp"                # nombre que no corresponde al id
        with open(b, "w", newline="") as g:
            w = csv.DictWriter(g, fieldnames=etiquetado.CAMPOS_HOJA, lineterminator="\n"); w.writeheader(); w.writerows(filas)
        out = os.path.join(self.tmp, "osha")
        self.correr("importar", "--sin-capturas", "--hoja", a, "--hoja", b, "--salida", out)
        ac = json.load(open(os.path.join(out, "acuerdo.json")))
        errs = {e["error"] for e in ac["errores_de_hoja"]}
        self.assertIn("captura_distinta_entre_revisores", errs)
        self.assertEqual(sum(e["error"] == "captura_invalida" for e in ac["errores_de_hoja"]), 2)
        self.assertEqual(ac["casos_comunes"], len(filas) - 3)

    def test_filas_invalidas_se_reportan(self):
        d = self.hoja()
        a = self.llenar(d, "revisor_a", lambda r: {"clase": "oferta", "tarifa": "50", "km": "", "min": "12"})
        b = self.llenar(d, "revisor_b", lambda r: {"clase": "no_oferta", "captura_sha256": "0" * 64})
        out = os.path.join(self.tmp, "o2")
        with self.assertRaises(SystemExit):   # ninguna fila válida → no hay 2 revisores
            self.correr("importar", "--manifiesto", self.man, "--hoja", a, "--hoja", b, "--salida", out)

    def test_un_solo_revisor_no_basta(self):
        d = self.hoja()
        a = self.llenar(d, "revisor_a", lambda r: {"clase": "no_oferta"})
        with self.assertRaises(SystemExit):
            self.correr("importar", "--manifiesto", self.man, "--hoja", a, "--salida", os.path.join(self.tmp, "o3"))

    def test_sql_generado_entra_a_la_base_con_precedencia(self):
        out, _ = self._par(lambda r: {"tarifa": "60.00"} if int(r["caso"][:4]) % 4 == 0 else {})
        pg = pg_local.Pg()
        self.addCleanup(pg.cerrar)
        pg.archivo(ESQUEMA); pg.sql(LEGADO)
        for n in ("001_auditoria", "002_registrar", "003_veredictos"):
            pg.archivo(mig(n))
        pg.archivo(os.path.join(out, "veredictos_humanos.sql"))
        est = dict(l.split("|")[::-1] for l in pg.sql(
            "select estado, count(*) from auditoria.veredicto_vigente where error_lector_id is not null and estado <> 'heredado_no_revisado' group by 1").split())
        ref = len(list(csv.DictReader(open(os.path.join(out, "referencia_humana.csv")))))
        conf = len(list(csv.DictReader(open(os.path.join(out, "conflictos.csv")))))
        self.assertEqual({k: int(v) for k, v in {v: k for k, v in est.items()}.items()},
                         {"referencia_doble": ref, "conflicto_humano": conf})


class TestMetricas(unittest.TestCase):
    def test_categorias(self):
        of = {"clase": "oferta", "tarifa": 50, "km": 5, "min": 12}
        no = {"clase": "no_oferta"}
        C = metricas.categoria
        self.assertEqual(C({"resultado": "oferta", "tarifa": 50, "km": 5.04, "min": 12.4}, of)[0], "acierto")
        self.assertEqual(C({"resultado": "oferta", "tarifa": 50, "km": 6, "min": 12}, of), ("error_datos", ["km"]))
        self.assertEqual(C({"resultado": "oferta", "tarifa": 1, "km": 1, "min": 1}, no)[0], "falso_positivo")
        self.assertEqual(C({"resultado": "nada"}, of)[0], "falso_negativo")
        self.assertEqual(C({"resultado": "nada"}, no)[0], "verdadero_negativo")
        self.assertEqual(C({"resultado": "varias"}, of)[0], "abstencion")
        self.assertEqual(C({"resultado": "oferta"}, {"clase": "ilegible"})[0], "sin_referencia")
        self.assertEqual(C({"resultado": "oferta"}, None)[0], "sin_referencia")

    def test_wilson_y_suficiencia(self):
        self.assertEqual(metricas.wilson(8, 10), [0.4902, 0.9433])
        self.assertIsNone(metricas.wilson(0, 0))
        self.assertFalse(metricas.proporcion(8, 10)["suficiente"])
        self.assertTrue(metricas.proporcion(30, 30)["suficiente"])

    def test_resumen_denominadores(self):
        of = {"clase": "oferta", "tarifa": 50, "km": 5, "min": 12}
        pares = [("a", "uber", {"resultado": "oferta", "tarifa": 50, "km": 5, "min": 12}, of),
                 ("b", "uber", {"resultado": "oferta", "tarifa": 50, "km": 9, "min": 12}, of),
                 ("c", "didi", {"resultado": "oferta", "tarifa": 5, "km": 1, "min": 1}, {"clase": "no_oferta"}),
                 ("d", "didi", {"resultado": "nada"}, of),
                 ("e", "didi", {"resultado": "varias"}, of),
                 ("f", "didi", {"resultado": "nada"}, {"clase": "ilegible"})]
        r = metricas.resumir(pares)["total"]
        self.assertEqual(r["evaluables"], 5)
        self.assertEqual((r["precision_ofertas"]["k"], r["precision_ofertas"]["n"]), (1, 3))
        self.assertEqual((r["tasa_fn"]["k"], r["tasa_fn"]["n"]), (1, 3))
        self.assertEqual((r["tasa_fp"]["k"], r["tasa_fp"]["n"]), (1, 1))
        self.assertEqual((r["abstenciones"]["k"], r["abstenciones"]["n"]), (1, 5))
        self.assertEqual(set(metricas.resumir(pares)["por_app"]), {"uber", "didi"})

    def test_kappa(self):
        self.assertEqual(metricas.kappa(["a", "a", "b", "b"], ["a", "a", "b", "b"]), 1.0)
        self.assertEqual(metricas.kappa(["a", "b", "a", "b"], ["a", "a", "b", "b"]), 0.0)
        self.assertIsNone(metricas.kappa([], []))


if __name__ == "__main__":
    unittest.main()
