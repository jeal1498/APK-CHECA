"""E1–E7 — la cadena completa de migraciones: 001→005 dos veces, 005→001 en reversa, y vuelta a subir."""
import unittest

import pg_local
from test_migraciones import ESQUEMA, LEGADO, huella_publica, mig

ORDEN = ["001_auditoria", "002_registrar", "003_veredictos", "004_admin", "005_metricas"]


class Cadena(unittest.TestCase):
    def test_subir_bajar_subir(self):
        pg = pg_local.Pg()
        self.addCleanup(pg.cerrar)
        pg.archivo(ESQUEMA)
        pg.sql(LEGADO)
        antes = huella_publica(pg, "prueba")
        for _ in range(2):
            for n in ORDEN:
                pg.archivo(mig(n))
        self.assertEqual(pg.sql("select string_agg(version::text, ',' order by version) from auditoria.version_esquema").strip(), "1,2,3,4,5")
        self.assertEqual(huella_publica(pg, "prueba"), antes)
        for n in reversed(ORDEN):
            pg.archivo(mig(n, True))
        self.assertEqual(pg.sql("select count(*) from pg_namespace where nspname = 'auditoria'").strip(), "0")
        self.assertEqual(pg.sql("select count(*) from pg_proc where proname like 'admin\\_%' or proname like 'auditoria\\_%'").strip(), "0")
        self.assertEqual(huella_publica(pg, "prueba"), antes)
        for n in ORDEN:
            pg.archivo(mig(n))
        # supabase.yml vuelve a ejecutar esquema.sql en cada despliegue: no rompe la cadena.
        pg.archivo(ESQUEMA)
        self.assertEqual(pg.sql("select count(*) from auditoria.version_esquema").strip(), "5")

    def test_funciones_security_definer_fijan_search_path(self):
        pg = pg_local.Pg()
        self.addCleanup(pg.cerrar)
        pg.archivo(ESQUEMA)
        for n in ORDEN:
            pg.archivo(mig(n))
        sin = pg.sql("""select p.oid::regprocedure from pg_proc p join pg_namespace n on n.oid = p.pronamespace
                        where p.prosecdef and n.nspname in ('auditoria', 'public')
                          and (p.proname like 'admin\\_%' or p.proname like 'auditoria\\_%' or n.nspname = 'auditoria')
                          and not exists (select 1 from unnest(coalesce(p.proconfig, '{}')) c where c like 'search_path=%')""").strip()
        self.assertEqual(sin, "")


if __name__ == "__main__":
    unittest.main()
