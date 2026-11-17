"""10.82: verificación del enlace línea ↔ captura por huella (herramientas/verificar_enlace_capturas.py)."""
import hashlib, os, sys, tempfile, unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "herramientas"))
import verificar_enlace_capturas as v


class Huella(unittest.TestCase):
    def test_coincide_distinta_sin_imagen_sin_huella(self):
        with tempfile.TemporaryDirectory() as t:
            open(os.path.join(t, "1.webp"), "wb").write(b"imagen-uno")
            open(os.path.join(t, "2.webp"), "wb").write(b"otra-imagen")
            h = hashlib.sha256(b"imagen-uno").hexdigest()
            r = v.verificar([{"caso_id": 1, "captura_sha256": h}, {"caso_id": 2, "captura_sha256": h},
                             {"caso_id": 3, "captura_sha256": h}, {"caso_id": 4, "captura_sha256": None}], t)
            self.assertEqual([x["resultado"] for x in r], ["coincide", "distinta", "sin_imagen", "sin_huella"])


if __name__ == "__main__":
    unittest.main()
