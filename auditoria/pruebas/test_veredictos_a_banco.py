"""veredictos_a_banco.py: solo referencia humana, formato del banco, sin sobrescribir y compatible con origen_banco.py."""
import importlib.util, os, tempfile, unittest

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def _cargar(nombre, ruta):
    spec = importlib.util.spec_from_file_location(nombre, os.path.join(RAIZ, ruta))
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


vb = _cargar("veredictos_a_banco", "auditoria/etiquetado/veredictos_a_banco.py")
ob = _cargar("origen_banco", "auditoria/etiquetado/origen_banco.py")


def fila(**kw):
    base = dict(id=2165, app="didi", version="10.82", tipo="diferencia", lineas=["$45.62", "3 min (761 m)", "16 min (5.6 km)"],
                estado="referencia_humana", clase="oferta", tarifa=45.62, km=6.361, min=19)
    base.update(kw)
    return base


class VeredictosABanco(unittest.TestCase):
    def test_oferta_se_escribe_en_el_formato_del_banco(self):
        n, t = vb.caso(fila())
        self.assertEqual(n, "2165-didi.txt")
        self.assertEqual(t.split("\n---\n")[-1].strip(), "tarifa=45.62 km=6.361 min=19")
        self.assertIn("según veredicto humano", t.split("\n---\n")[0])
        self.assertIn("16 min (5.6 km)", t)

    def test_no_oferta_es_nada(self):
        n, t = vb.caso(fila(clase="no_oferta", tarifa=None, km=None, min=None))
        self.assertEqual(t.split("\n---\n")[-1].strip(), "nada")

    def test_solo_referencia_humana_y_clases_aptas(self):
        for kw in (dict(estado="solo_ia"), dict(estado="heredado_no_revisado"), dict(estado="conflicto_humano"),
                   dict(clase="ilegible"), dict(clase="varias"), dict(clase="cambio_pantalla"),
                   dict(tarifa=None), dict(km=0), dict(lineas=[]), dict(lineas="[]")):
            self.assertIsNone(vb.caso(fila(**kw))[0], kw)

    def test_no_sobrescribe_y_reemplaza_a_peticion(self):
        with tempfile.TemporaryDirectory() as d:
            r = vb.convertir([fila()], d)
            self.assertEqual(r["nuevos"], ["2165-didi.txt"])
            with open(os.path.join(d, "2165-didi.txt"), "a") as h:
                h.write("# editado a mano\n")
            r = vb.convertir([fila()], d)
            self.assertEqual(r["existentes"], ["2165-didi.txt"])
            self.assertIn("# editado a mano", open(os.path.join(d, "2165-didi.txt")).read())
            r = vb.convertir([fila()], d, reemplazar=True)
            self.assertEqual(r["reemplazados"], ["2165-didi.txt"])
            self.assertNotIn("# editado a mano", open(os.path.join(d, "2165-didi.txt")).read())

    def test_simular_no_escribe(self):
        with tempfile.TemporaryDirectory() as d:
            vb.convertir([fila()], d, simular=True)
            self.assertEqual(os.listdir(d), [])

    def test_origen_banco_lo_reconoce_como_humano_verificado(self):
        with tempfile.TemporaryDirectory() as d:
            vb.convertir([fila()], d)
            filas = ob.clasificar(d)
            self.assertEqual(len(filas), 1)
            self.assertEqual(filas[0]["origen"], "veredicto_humano")
            self.assertEqual(filas[0]["humano_verificado"], "si")
            self.assertEqual(filas[0]["circular"], "no")


if __name__ == "__main__":
    unittest.main()
