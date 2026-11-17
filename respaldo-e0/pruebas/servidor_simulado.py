"""Servidor SIMULADO de Supabase (solo para pruebas locales). No es Supabase: no prueba RLS ni permisos reales."""
import json
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

WEBP = b"RIFF\x24\x00\x00\x00WEBPVP8 " 


def webp(semilla, extra=0):
    return WEBP + semilla.encode() * (3 + extra)


class Sim:
    def __init__(self, clave, objetos, tablas, tope_pagina=100):
        self.clave, self.objetos, self.tablas, self.tope = clave, dict(objetos), tablas, tope_pagina
        self.log, self.fallar_una_vez, self.no_existen = [], set(), set()
        self.quitar_tras_listar, self.truncar, self.html, self.redirigir = set(), set(), set(), {}
        self.listados, self.codigo_no_existe, self.etags = 0, 404, {}
        self.srv = ThreadingHTTPServer(("127.0.0.1", 0), self._handler())
        self.url = f"http://127.0.0.1:{self.srv.server_port}"
        threading.Thread(target=self.srv.serve_forever, daemon=True).start()

    def cerrar(self):
        self.srv.shutdown()
        self.srv.server_close()

    def _handler(self):
        sim = self

        class H(BaseHTTPRequestHandler):
            def log_message(self, *a):
                pass

            def _responder(self, codigo, cuerpo=b"", cab=None, largo=None):
                self.send_response(codigo)
                for k, v in (cab or {}).items():
                    self.send_header(k, v)
                self.send_header("Content-Length", str(len(cuerpo) if largo is None else largo))
                self.end_headers()
                self.wfile.write(cuerpo)

            def _auth(self):
                return self.headers.get("Authorization") == "Bearer " + sim.clave

            def do_POST(self):
                sim.log.append(("POST", self.path))
                n = int(self.headers.get("Content-Length", 0))
                cuerpo = json.loads(self.rfile.read(n) or b"{}")
                if not self._auth():
                    return self._responder(401, b"{}")
                if sim.llamada_fallida(self.path):
                    return self._responder(500, b"{}")
                if not self.path.startswith("/storage/v1/object/list/capturas"):
                    return self._responder(404, b"{}")
                pref, off, lim = cuerpo.get("prefix", ""), cuerpo.get("offset", 0), min(cuerpo.get("limit", 100), sim.tope)
                nombres = sorted(k for k in sim.objetos if k.startswith(pref))
                items, carpetas = [], set()
                for k in nombres:
                    resto = k[len(pref):]
                    if "/" in resto:
                        carpetas.add(resto.split("/")[0])
                    else:
                        items.append({"name": resto, "id": "id-" + k, "created_at": "2026-11-13T10:00:00Z", "metadata": {"size": len(sim.objetos[k]), **({"eTag": '"' + sim.etags[k] + '"'} if k in sim.etags else {})}})
                items += [{"name": c, "id": None, "metadata": None} for c in sorted(carpetas)]
                items.sort(key=lambda i: i["name"])
                sim.listados += 1
                self._responder(200, json.dumps(items[off:off + lim]).encode())
                if sim.listados == 1:
                    for k in sim.quitar_tras_listar:
                        sim.objetos.pop(k, None)

            def do_GET(self):
                sim.log.append(("GET", self.path))
                if not self._auth():
                    return self._responder(401, b"{}")
                if sim.llamada_fallida(self.path):
                    return self._responder(500, b"{}")
                ruta = urlparse(self.path).path
                if ruta.startswith("/storage/v1/object/authenticated/capturas/"):
                    nombre = unquote(ruta[len("/storage/v1/object/authenticated/capturas/"):])
                    if nombre in sim.redirigir:
                        return self._responder(302, b"", {"Location": sim.redirigir[nombre]})
                    if nombre in sim.no_existen or nombre not in sim.objetos:
                        return self._responder(sim.codigo_no_existe, b'{"statusCode":"404","error":"not_found"}')
                    datos = sim.objetos[nombre]
                    if nombre in sim.html:
                        return self._responder(200, b"<html>error</html>" + b"x" * (len(datos) - 18))
                    if nombre in sim.truncar:
                        return self._responder(200, datos[:10], largo=len(datos))
                    return self._responder(200, datos)
                if ruta.startswith("/rest/v1/"):
                    tabla = ruta.split("/")[-1]
                    filas = sim.tablas.get(tabla)
                    if filas is None:
                        return self._responder(404, b"{}")
                    a, b = self.headers.get("Range", "0-999").split("-")
                    a, b = int(a), int(b)
                    if a >= len(filas) and a > 0:
                        return self._responder(416, b"{}")
                    trozo = filas[a:min(b + 1, a + 1000)]
                    rango = f"{a}-{a + len(trozo) - 1}/{len(filas)}" if trozo else f"*/{len(filas)}"
                    return self._responder(206 if len(trozo) < len(filas) else 200, json.dumps(trozo).encode(), {"Content-Range": rango})
                self._responder(404, b"{}")

        return H

    def llamada_fallida(self, ruta):
        if ruta in self.fallar_una_vez:
            self.fallar_una_vez.discard(ruta)
            return True
        return False
