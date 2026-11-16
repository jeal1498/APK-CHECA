"""E3 — privacidad de la redacción sobre el corpus real (banco/ + ofertas/): lo que saldría del teléfono.
`evidencias/e3/redaccion_corpus.json` lo genera Redaccion.kt (herramientas/jvm/probar.sh --corpus …)."""
import json
import os
import re
import subprocess
import tempfile
import unittest

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CORPUS = os.path.join(RAIZ, "evidencias", "cierre", "e3", "redaccion_corpus.json")
KOTLINC = os.path.expanduser("~/.cache/checa-banco/kotlinc/bin/kotlinc")
PERMITIDAS = set("min|km|m|h|est|viaje|a|de|por|inicio|incluido|adicionales|adicional|aceptar|total|tarifa|x".split("|"))


class Redaccion(unittest.TestCase):
    def setUp(self):
        self.d = json.load(open(CORPUS, encoding="utf-8"))

    def test_cubre_todo_el_corpus(self):
        n = sum(1 for c in ("banco", "ofertas") for f in os.listdir(os.path.join(RAIZ, "app", "src", "test", "resources", c))
                if f.endswith(".txt") and f != "minimo.txt")
        self.assertEqual(len(self.d), n)
        self.assertEqual(n, 361)

    def test_ninguna_linea_redactada_trae_palabras_ajenas_ni_numeros_sueltos(self):
        for c in self.d:
            for l in c["salen"]:
                with self.subTest(c["caso"]):
                    self.assertLessEqual(len(l), 60)
                    self.assertEqual([w for w in re.findall(r"[^\W\d_]+", l) if w.lower() not in PERMITIDAS], [], l)
                    resto = re.sub(r"[+-]?[$€£] ?\d[\d.,]*|\d+(?:[.,]\d+)? ?(?:min|km|m|h|x)", "", l, flags=re.I)
                    self.assertFalse(any(ch.isdigit() for ch in resto), l)

    def test_las_direcciones_del_corpus_no_sobreviven(self):
        """El corpus original SÍ trae direcciones pegadas al tramo; en lo redactado no queda ninguna."""
        con_direccion = 0
        for c in ("banco", "ofertas"):
            d = os.path.join(RAIZ, "app", "src", "test", "resources", c)
            for f in os.listdir(d):
                if f.endswith(".txt") and re.search(r"\(\d+ ?min [\d.]+ ?km\) \S{3,}", open(os.path.join(d, f), encoding="utf-8").read()):
                    con_direccion += 1
        self.assertGreater(con_direccion, 50)   # el problema existe en los datos de origen
        todo = " ".join(l for c in self.d for l in c["salen"]).lower()
        for palabra in ("calle", "avenida", "centro", "plaza", "hospital", "col"):
            self.assertNotRegex(todo, rf"(?<![^\W\d_]){palabra}(?![^\W\d_])")

    def test_el_servidor_acepta_todas_las_lineas_redactadas(self):
        out = subprocess.run(["node", "--experimental-strip-types", "--no-warnings",
                              os.path.join(RAIZ, "auditoria", "pruebas", "e2", "validar_archivo.ts"), CORPUS, "--lineas"],
                             capture_output=True, text=True, check=True).stdout
        self.assertEqual(json.loads(out), [])

    @unittest.skipUnless(os.path.exists(KOTLINC), "sin kotlinc")
    def test_la_evidencia_versionada_es_la_que_produce_el_codigo(self):
        with tempfile.TemporaryDirectory() as t:
            nuevo = os.path.join(t, "c.json")
            p = subprocess.run([os.path.join(RAIZ, "herramientas", "jvm", "probar.sh"), "--corpus", nuevo], capture_output=True, text=True)
            self.assertEqual(p.returncode, 0, p.stdout[-600:] + p.stderr[-600:])
            self.assertEqual(json.load(open(nuevo, encoding="utf-8")), self.d)


if __name__ == "__main__":
    unittest.main()
