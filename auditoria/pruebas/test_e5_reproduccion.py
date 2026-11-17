"""E5 — protocolo ciego, regresión por caso y reproducción (usa kotlinc del caché de docs/banco/calificar.sh)."""
import contextlib
import io
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
REP = os.path.join(RAIZ, "auditoria", "reproduccion")
sys.path.insert(0, REP)
import evaluar  # noqa: E402

KOTLINC = os.path.expanduser("~/.cache/checa-banco/kotlinc/bin/kotlinc")


def correr(*a):
    with contextlib.redirect_stdout(io.StringIO()):
        return evaluar.main(list(a))


class Protocolo(unittest.TestCase):
    def setUp(self):
        self.t = tempfile.mkdtemp(prefix="e5-")
        self.addCleanup(shutil.rmtree, self.t, True)
        self.et = os.path.join(self.t, "etiquetas.jsonl")
        self.pr = os.path.join(self.t, "pred.jsonl")
        self.reg = os.path.join(self.t, "registro.json")
        self.cor = os.path.join(self.t, "corrida.json")
        with open(self.et, "w") as f:
            f.write(json.dumps({"caso": "a", "app": "uber", "esperado": {"clase": "oferta", "tarifa": 50, "km": 5, "min": 12},
                                "origen": "x", "circular": False, "fuente": "heredado"}) + "\n")
            f.write(json.dumps({"caso": "b", "app": "didi", "esperado": {"clase": "no_oferta"}, "origen": "x", "circular": True,
                                "fuente": "heredado"}) + "\n")
        self.escribir_pred([{"caso": "a", "resultado": "oferta", "tarifa": 50.5, "km": 5.1, "min": 12}, {"caso": "b", "resultado": "nada"}])

    def escribir_pred(self, l, ruta=None):
        ruta = ruta or self.pr
        with open(ruta, "w") as f:
            for x in l:
                f.write(json.dumps(x) + "\n")
        with open(self.cor, "w") as f:
            json.dump({"predicciones_sha256": evaluar.sha(ruta)}, f)
        return ruta

    def test_orden_completo(self):
        self.assertEqual(correr("sellar", "--etiquetas", self.et, "--registro", self.reg), 0)
        self.assertEqual(correr("comprometer", "--predicciones", self.pr, "--corrida", self.cor, "--registro", self.reg), 0)
        inf = os.path.join(self.t, "inf.json")
        self.assertEqual(correr("revelar", "--etiquetas", self.et, "--predicciones", self.pr, "--registro", self.reg, "--salida", inf), 0)
        i = json.load(open(inf))
        self.assertEqual(i["total"]["acuerdo_banco"]["k"], 2)
        self.assertEqual(i["sin_circulares"]["n"], 1)
        self.assertIn("no humanas", i["aviso"])

    def test_revelar_sin_comprometer_falla(self):
        correr("sellar", "--etiquetas", self.et, "--registro", self.reg)
        with self.assertRaises(SystemExit):
            correr("revelar", "--etiquetas", self.et, "--predicciones", self.pr, "--registro", self.reg, "--salida", os.path.join(self.t, "i"))

    def test_comprometer_sin_sellar_falla(self):
        with self.assertRaises(SystemExit):
            correr("comprometer", "--predicciones", self.pr, "--corrida", self.cor, "--registro", self.reg)

    def test_etiquetas_cambiadas_despues_de_sellar(self):
        correr("sellar", "--etiquetas", self.et, "--registro", self.reg)
        correr("comprometer", "--predicciones", self.pr, "--corrida", self.cor, "--registro", self.reg)
        with open(self.et, "a") as f:
            f.write("\n")
        with self.assertRaises(SystemExit):
            correr("revelar", "--etiquetas", self.et, "--predicciones", self.pr, "--registro", self.reg, "--salida", os.path.join(self.t, "i"))

    def test_predicciones_cambiadas_despues_de_comprometer(self):
        correr("sellar", "--etiquetas", self.et, "--registro", self.reg)
        correr("comprometer", "--predicciones", self.pr, "--corrida", self.cor, "--registro", self.reg)
        with open(self.pr, "a") as f:
            f.write(json.dumps({"caso": "c", "resultado": "nada"}) + "\n")
        with self.assertRaises(SystemExit):
            correr("revelar", "--etiquetas", self.et, "--predicciones", self.pr, "--registro", self.reg, "--salida", os.path.join(self.t, "i"))

    def test_registro_no_se_reutiliza(self):
        correr("sellar", "--etiquetas", self.et, "--registro", self.reg)
        with self.assertRaises(SystemExit):
            correr("sellar", "--etiquetas", self.et, "--registro", self.reg)

    def _corrida(self, pred, nombre, **k):
        c = {"entradas_sha256": "e" * 64, "n_entradas": 2, "java": "21", "kotlinc": "1.9.24", "predicciones_sha256": evaluar.sha(pred), "ref": nombre}
        c.update(k)
        ruta = os.path.join(self.t, nombre + ".corrida.json")
        json.dump(c, open(ruta, "w"))
        return ruta

    def test_comparar_detecta_regresion_y_mejora(self):
        cand = self.escribir_pred([{"caso": "a", "resultado": "nada"}, {"caso": "b", "resultado": "nada"}], os.path.join(self.t, "c.jsonl"))
        cb, cc = self._corrida(self.pr, "base"), self._corrida(cand, "cand")
        sal = os.path.join(self.t, "cmp.json")
        self.assertEqual(correr("comparar", "--base", self.pr, "--candidata", cand, "--etiquetas", self.et, "--salida", sal,
                                "--corrida-base", cb, "--corrida-candidata", cc), 1)
        d = json.load(open(sal))
        self.assertEqual(d["regresiones"], ["a"])
        self.assertEqual(d["condiciones"]["entradas_sha256"], "e" * 64)
        self.assertEqual(correr("comparar", "--base", cand, "--candidata", self.pr, "--etiquetas", self.et, "--salida", sal,
                                "--corrida-base", cc, "--corrida-candidata", cb), 0)
        self.assertEqual(json.load(open(sal))["mejoras"], ["a"])

    def test_a4_comparar_exige_mismo_corpus_y_condiciones(self):
        cand = self.escribir_pred([{"caso": "a", "resultado": "nada"}, {"caso": "b", "resultado": "nada"}], os.path.join(self.t, "c.jsonl"))
        cb = self._corrida(self.pr, "base")
        with self.assertRaises(SystemExit):   # sin corridas no se compara
            correr("comparar", "--base", self.pr, "--candidata", cand, "--etiquetas", self.et)
        for nombre, k in (("otro_corpus", {"entradas_sha256": "f" * 64}), ("otra_jvm", {"java": "17"}), ("otro_kotlin", {"kotlinc": "2.0"}),
                          ("otras_predicciones", {"predicciones_sha256": "0" * 64}), ("menos_casos", {"n_entradas": 1})):
            with self.subTest(nombre):
                with self.assertRaises(SystemExit) as e:
                    correr("comparar", "--base", self.pr, "--candidata", cand, "--etiquetas", self.et,
                           "--corrida-base", cb, "--corrida-candidata", self._corrida(cand, nombre, **k))
                self.assertIn("NO válida", str(e.exception))

    def test_tolerancias_del_banco(self):
        e = {"clase": "oferta", "tarifa": 50, "km": 5, "min": 12, "recKm": 1}
        self.assertTrue(evaluar.acierta_banco({"resultado": "oferta", "tarifa": 50.9, "km": 5.1, "min": 13, "recKm": 1.1}, e))
        self.assertFalse(evaluar.acierta_banco({"resultado": "oferta", "tarifa": 50, "km": 5, "min": 12, "recKm": 1.3}, e))
        self.assertFalse(evaluar.acierta_banco({"resultado": "nada"}, e))
        self.assertTrue(evaluar.acierta_banco({"resultado": "nada"}, {"clase": "no_oferta"}))


@unittest.skipUnless(os.path.exists(KOTLINC), "sin kotlinc (corre docs/banco/calificar.sh)")
class Reproduccion(unittest.TestCase):
    def test_determinista_y_rechaza_entradas_con_respuesta(self):
        t = tempfile.mkdtemp(prefix="e5r-")
        self.addCleanup(shutil.rmtree, t, True)
        with contextlib.redirect_stdout(io.StringIO()):
            evaluar.preparar([("sint", os.path.join(REP, "sinteticos"), "sintetico_regla")], os.path.join(t, "c"))
        sh = os.path.join(REP, "reproducir.sh")
        for n in ("r1", "r2"):
            subprocess.run([sh, "actual", os.path.join(t, "c", "entradas"), os.path.join(t, n)], check=True, capture_output=True)
        a, b = (json.load(open(os.path.join(t, n, "corrida.json"))) for n in ("r1", "r2"))
        self.assertEqual(a["predicciones_sha256"], b["predicciones_sha256"])
        preds = evaluar.leer_jsonl(os.path.join(t, "r1", "predicciones.jsonl"))
        etq = evaluar.leer_jsonl(os.path.join(t, "c", "etiquetas.jsonl"))
        self.assertEqual(evaluar.calificar(preds, etq)["total"]["acuerdo_banco"]["k"], 4)
        # Una entrada que trae «---» (respuesta) no es ciega: el corredor la rechaza.
        mala = os.path.join(t, "mala"); os.makedirs(mala)
        open(os.path.join(mala, "x.txt"), "w").write("$1.00\n---\nnada\n")
        p = subprocess.run([sh, "actual", mala, os.path.join(t, "r3")], capture_output=True, text=True)
        self.assertNotEqual(p.returncode, 0)

    def test_evidencia_versionada_reconcilia_con_banco(self):
        """El informe versionado de la evaluación ciega coincide con lo que da Banco.kt (271 de 316)."""
        i = json.load(open(os.path.join(RAIZ, "evidencias", "e5", "ciega-10.81", "informe.json")))
        # La evidencia sellada es la de la 10.81 (271); el piso del banco solo sube con arreglos del lector (272 desde el 17-nov).
        self.assertLessEqual(i["por_corpus"]["banco"]["acuerdo_banco"]["k"], int(open(os.path.join(RAIZ, "app", "src", "test", "resources", "banco", "minimo.txt")).read()))
        self.assertEqual([p["paso"] for p in i["registro"]["pasos"]], ["sellar", "comprometer"])
        reg = json.load(open(os.path.join(RAIZ, "evidencias", "e5", "ciega-10.81", "registro.json")))
        import hashlib
        self.assertEqual(reg["pasos"][2]["informe_sha256"], hashlib.sha256(json.dumps(i, sort_keys=True, ensure_ascii=False).encode()).hexdigest())


if __name__ == "__main__":
    unittest.main()
