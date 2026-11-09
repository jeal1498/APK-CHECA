"""Compilador del estilo A3 con loop (único estilo vigente, narrador.md §0).

a3.json (datos del video) + tiempos.json (segundo de cada palabra) -> <proyecto>/a3/index.html y plan.
Todo lo que en a3.json es una palabra se convierte aquí a segundos absolutos; la página solo anima.

Especificación de tiempo («spec») en a3.json:
  "palabra"          segundo en que empieza esa palabra (la exacta; si no hay, la primera que empiece así) dentro de su línea
  "palabra#1"        la segunda coincidencia (cuenta desde 0)
  "palabra@-0.05"    esa palabra con un desplazamiento en segundos (también en «palabra#1@+0.3»)
  0.5                segundos desde el inicio de la línea
  "fin"              fin de la última palabra de la línea
"""
import json, os, re

AQUI = os.path.dirname(os.path.abspath(__file__))
NIVELES = ("gris", "verde", "amarillo", "rojo")


def norm(s):
    return re.sub(r"[^\w]", "", s.lower())


class Compilador:
    def __init__(self, T, A):
        self.T, self.A = T, A
        self.sc = T["scenes"]
        if len(A["lineas"]) != len(self.sc):
            raise SystemExit(f"a3.json tiene {len(A['lineas'])} líneas y tiempos.json {len(self.sc)}")
        self.plan = []  # (segundo, línea, texto)

    # ---------- tiempo ----------
    def t(self, i, spec, defecto=None):
        """spec → segundo absoluto, resuelto dentro de la línea i."""
        if spec is None:
            return defecto
        sc = self.sc[i]
        if isinstance(spec, (int, float)):
            return round(sc["start"] + spec, 3)
        off = 0.0
        if "@" in spec:
            spec, o = spec.split("@")
            off = float(o)
        if spec == "fin":
            return round(sc["start"] + sc["words"][-1][2] + off, 3)
        n = 0
        if "#" in spec:
            spec, k = spec.split("#")
            n = int(k)
        w = norm(spec)
        hits = [x for x in sc["words"] if norm(x[0]) == w] or [x for x in sc["words"] if norm(x[0]).startswith(w)]  # la palabra exacta manda sobre el prefijo
        if len(hits) <= n:
            raise SystemExit(f"línea {sc['n']}: no encuentro «{spec}»#{n} en la voz: {[x[0] for x in sc['words']]}")
        return round(sc["start"] + hits[n][1] + off, 3)

    def nota(self, t, i, txt):
        self.plan.append((round(t, 2), self.sc[i]["n"], txt))

    # ---------- compilar ----------
    def compilar(self):
        A, T = self.A, self.T
        fin_voz = max(s["start"] + s["words"][-1][2] for s in self.sc if s.get("words"))
        total = A.get("total") or round(fin_voz + 1.65, 1)
        D = {"total": total, "hook": A["hook"], "lines": [], "golpes": [], "lavado": [], "sacude": [], "ml": [], "scan": [], "cursor": [],
             "stamp": [], "ring": [], "ojo": [], "altavoz": [], "chipPulso": [], "chipBrillo": [], "chipRuns": [], "din": None}
        n = len(A["lineas"])
        previa_pop = previa_chip = False
        run = None
        pop_entra = None
        cta_i = None
        for i, a in enumerate(A["lineas"]):
            sc = self.sc[i]
            vista = a["vista"]
            fin = self.sc[i + 1]["start"] if i + 1 < n else total
            ln = {"start": sc["start"], "end": fin, "vista": vista, "zoom": a.get("zoom", 1.0)}
            # teléfono
            if "tel" in a:
                tl = a["tel"]
                o = tl.get("oferta")
                ln["tel"] = {"oferta": {"precio": o["precio"], "l1": o["l1"], "l2": o["l2"]} if o else None,
                             "cae": self.t(i, tl.get("cae")), "ruta": [self.t(i, tl["ruta"][0]), self.t(i, tl["ruta"][1])] if tl.get("ruta") else None,
                             "zoom": [self.t(i, tl["zoom"][0]), self.t(i, tl["zoom"][1])] if tl.get("zoom") else None}
                if tl.get("cae") is not None:
                    self.nota(ln["tel"]["cae"], i, f"cae la oferta {o['precio'] if o else ''} (vibra el teléfono)")
                if tl.get("ruta"):
                    self.nota(ln["tel"]["ruta"][0], i, "se dibuja la ruta")
            # tarjeta de oferta
            if "chip" in a:
                kfs = [{"t": (self.t(i, k.get("en")) if k.get("en") is not None else sc["start"] - 1), "precio": k["precio"], "sub": k["sub"]} for k in a["chip"]["kfs"]]
                ln["chip"] = {"kfs": kfs}
                for k in kfs[1:]:
                    self.nota(k["t"], i, f"la tarjeta de oferta pasa a {k['precio']}")
                if a["chip"].get("pulso") is not None:
                    D["chipPulso"].append(self.t(i, a["chip"]["pulso"]))
                    self.nota(D["chipPulso"][-1], i, "la tarjeta de oferta late («esa oferta»)")
                if a["chip"].get("brillo") is not None:
                    D["chipBrillo"].append(self.t(i, a["chip"]["brillo"]))
                    self.nota(D["chipBrillo"][-1], i, "la tarjeta de oferta brilla en verde («se ven bien»)")
                if a["chip"].get("escaneo"):
                    e = a["chip"]["escaneo"]
                    D["scan"].append([self.t(i, e[0]), self.t(i, e[1])])
                    self.nota(D["scan"][-1][0], i, "barrido de escaneo sobre la tarjeta de oferta («lee»)")
            # pop-up
            if "popup" in a:
                pp = a["popup"]
                ests = []
                for k in pp["estados"]:
                    e = {"t": self.t(i, k.get("en")) if k.get("en") is not None else sc["start"] - 1,
                         "nivel": k["nivel"], "pv": k.get("pv", ""), "gas": k.get("gas"), "ok": ([1 if x else 0 for x in k["ok"]] if isinstance(k.get("ok"), list) else 1 if k.get("ok") else 0),
                         "h": k.get("h"), "k": k.get("k"), "m": k.get("m"), "km": k.get("km"), "nl": k.get("nl", "netos"), "txt": k.get("txt")}
                    if k["nivel"] not in NIVELES:
                        raise SystemExit(f"línea {sc['n']}: nivel «{k['nivel']}» (usa {NIVELES})")
                    ne = k.get("neta", "···")
                    if isinstance(ne, dict):
                        c = ne["cuenta"]
                        e["neta"] = {"cuenta": {"de": c["de"], "a": c["a"], "t0": self.t(i, c["en"]), "t1": self.t(i, c["hasta"])}}
                        self.nota(e["neta"]["cuenta"]["t0"], i, f"el número baja de ${c['de']} a ${c['a']}")
                    else:
                        e["neta"] = ne
                    if k.get("bump") is not None:
                        e["bump"] = self.t(i, k["bump"])
                        self.nota(e["bump"], i, f"el número late: {e['neta'] if isinstance(e['neta'], str) else '$' + str(k['neta']['cuenta']['a'])} {e['nl']}")
                    e["_en"] = k.get("en")
                    ests.append(e)
                    if k.get("en") is not None and k["nivel"] != "gris":
                        self.nota(e["t"], i, f"el pop-up cambia a {k['nivel'].upper()} · {TXT[k['nivel']]}")
                ests.sort(key=lambda e: e["t"])
                for e in ests:
                    e.pop("_en", None)
                ln["popup"] = {"estados": ests, "gas_en": self.t(i, pp.get("gas_en")),
                               "fichas": (1 if pp.get("fichas") == "siempre" else self.t(i, pp.get("fichas")) if pp.get("fichas") is not None else None)}
                if pp.get("entra") is not None:
                    pop_entra = self.t(i, pp["entra"])
                    self.nota(pop_entra, i, "entra el pop-up")
                if pp.get("gas_en") is not None:
                    self.nota(ln["popup"]["gas_en"], i, "aparece la gasolina en rojo")
                if ln["popup"]["fichas"] not in (None, 1):
                    self.nota(ln["popup"]["fichas"], i, "entran las 3 fichas con su valor final")
            # dinero (vista «dinero»): pila de billetes a escala, manguera de gasolina, llanta y dos columnas
            if "dinero" in a:
                self.dinero(i, a["dinero"], D)
            # pop-up E de la APK dentro del teléfono (vista «telpop»), arriba de la oferta
            if "pe" in a:
                q = a["pe"]
                ests = []
                for k in q["estados"]:
                    if k["nivel"] not in NIVELES[1:]:
                        raise SystemExit(f"línea {sc['n']}: nivel del pop-up E «{k['nivel']}» (usa verde, amarillo o rojo)")
                    ests.append({"t": self.t(i, k.get("en")) if k.get("en") is not None else sc["start"] - 1, "nivel": k["nivel"], "monto": k["monto"],
                                 "h": k["h"], "okh": 1 if k.get("okh") else 0, "k": k["k"], "okk": 1 if k.get("okk") else 0, "tot": k["tot"]})
                    if k.get("en") is not None:
                        self.nota(ests[-1]["t"], i, f"el pop-up E pasa a {k['nivel'].upper()} {k['monto']}")
                ests.sort(key=lambda e: e["t"])
                ln["pe"] = {"estados": ests, "bump": self.t(i, q.get("bump")), "pulso": self.t(i, q.get("pulso"))}
                if q.get("entra") is not None:
                    D["peEntra"] = self.t(i, q["entra"])
                    self.nota(D["peEntra"], i, f"entra el pop-up E arriba de la oferta, ya con su valor ({ests[0]['nivel'].upper()} {ests[0]['monto']})")
                if q.get("bump") is not None:
                    self.nota(ln["pe"]["bump"], i, "el monto del pop-up E late")
                if q.get("pulso") is not None:
                    self.nota(ln["pe"]["pulso"], i, "el borde del pop-up E brilla")
            # metas
            if "metas" in a:
                m = a["metas"]
                ln["metas"] = {"km": self.t(i, m["km"]), "hora": self.t(i, m["hora"]), "km_v": m["km_v"], "hora_v": m["hora_v"]}
                self.nota(ln["metas"]["km"], i, f"se resalta «por km» ({m['km_v'][0]}→{m['km_v'][1]})")
                self.nota(ln["metas"]["hora"], i, f"se resalta «por hora» ({m['hora_v'][0]}→{m['hora_v'][1]})")
            # extras sueltos
            if a.get("ml") is not None:
                D["ml"].append(self.t(i, a["ml"]))
                self.nota(D["ml"][-1], i, "pop del logo del semáforo")
            if a.get("sello"):
                s = a["sello"]
                D["stamp"].append({"t": self.t(i, s["en"]), "fin": self.t(i, s.get("hasta"), fin - 0.07) if s.get("hasta") is not None else round(fin - 0.07, 3), "txt": s["texto"]})
                self.nota(D["stamp"][-1]["t"], i, f"sello «{s['texto']}»")
            if a.get("reloj"):
                r = a["reloj"]
                D["ring"].append({"a": self.t(i, r["de"]), "b": self.t(i, r["a"]), "seg": r.get("seg", 10)})
                self.nota(D["ring"][-1]["a"], i, f"reloj de {r.get('seg', 10)} s")
            if a.get("ojo"):
                D["ojo"].append({"a": self.t(i, a["ojo"]["en"]), "b": self.t(i, a["ojo"]["hasta"])})
                self.nota(D["ojo"][-1]["a"], i, "ojo tachado")
            if a.get("altavoz"):
                D["altavoz"].append({"a": self.t(i, a["altavoz"]["en"]), "b": self.t(i, a["altavoz"]["hasta"])})
                self.nota(D["altavoz"][-1]["a"], i, "altavoz")
            if a.get("cursor"):
                c = a["cursor"]
                ac = self.t(i, c["en"])
                D["cursor"].append({"a": ac - 1.1, "ac_fin": ac + 1.8, "fin": self.t(i, c.get("hasta", "fin")) if c.get("hasta") is not None else fin, "ban": ac - 0.05,
                                    "ban_fin": self.t(i, c["ban_hasta"])})
                self.nota(ac - 0.05, i, "el cursor se frena sobre «Aceptar» y aparece el 🚫")
            for g in a.get("golpes", []):
                en = self.t(i, g["en"])
                D["golpes"].append({"a": round(en + g.get("de", -0.05), 3), "b": round(en + g.get("a", 0.9), 3), "txt": g["texto"], "color": g.get("color", "hueso")})
                self.nota(en + g.get("de", -0.05), i, f"golpe «{g['texto']}»")
            for l in a.get("lavado", []):
                D["lavado"].append({"t": self.t(i, l["en"]), "color": l["color"]})
                self.nota(D["lavado"][-1]["t"], i, f"lavado de color {l['color']} a pantalla completa")
            for x in a.get("sacude", []):
                D["sacude"].append(self.t(i, x))
                self.nota(D["sacude"][-1], i, "sacudida")
            # cierre
            if vista == "cta":
                c = a.get("cta", {})
                cta_i = i
                D["cta"] = {"t": sc["start"], "pill": self.t(i, c.get("pill", 0.5)), "gp": self.t(i, c["google"]) if c.get("google") else None,
                            "handles": round(fin_voz + 0.11, 3)}
                self.nota(sc["start"], i, "cierre: logo del semáforo")
                self.nota(D["cta"]["pill"], i, "botón «Comenta CHECA»")
                if D["cta"]["gp"] is not None:
                    self.nota(D["cta"]["gp"], i, "chip de Google Play")
            # carreras de la tarjeta de oferta (pop con tarjeta)
            tiene_chip = vista == "pop"
            if tiene_chip and not previa_chip:
                run = [sc["start"], fin]
            elif tiene_chip and run:
                run[1] = fin
            if not tiene_chip and previa_chip and run:
                D["chipRuns"].append(run)
                run = None
            previa_chip = tiene_chip
            D["lines"].append(ln)
        if run:
            D["chipRuns"].append(run)
        if D["din"] is not None:
            ult = max(j for j, a in enumerate(A["lineas"]) if a["vista"] == "dinero")
            D["din"]["fin"] = self.sc[ult + 1]["start"] if ult + 1 < n else total
        if cta_i is None:
            raise SystemExit("falta una línea con vista «cta» (la última)")
        if pop_entra is None and D.get("peEntra") is None:
            raise SystemExit("ninguna línea trae popup.entra ni pe.entra")
        D["gasRef"] = next((e["gas"] for l in D["lines"] if l.get("popup") for e in l["popup"]["estados"] if e["gas"]), "Gasolina")
        D["popEntra"] = pop_entra if pop_entra is not None else 1e6  # sin pop-up grande: nunca entra
        D.setdefault("peEntra", None)
        D["popSale"] = round(self.sc[cta_i]["start"] - 0.15, 3)
        D["cursor"] = [c for c in D["cursor"]]
        D["capFin"] = round(fin_voz + 0.06, 3)
        D["caps"] = self.subtitulos()
        return D

    def dinero(self, i, d, D):
        """Datos de la escena de billetes; se acumulan en D["din"] (una sola pila por video)."""
        z = D["din"] = D["din"] or {"max": d.get("max", 1500), "monto": d.get("monto", 1500), "cuenta": [], "tacha": None, "corte": None,
                                    "manguera": None, "llanta": None, "cols": None}
        if d.get("tacha") is not None:
            z["tacha"] = self.t(i, d["tacha"])
            self.nota(z["tacha"], i, "se tacha el monto de la pila")
        if d.get("manguera") is not None:
            z["manguera"] = self.t(i, d["manguera"])
            self.nota(z["manguera"], i, "entra la manguera de gasolina")
        if d.get("corte"):
            c = d["corte"]
            z["corte"] = {"valor": c["valor"], "t": self.t(i, c["en"]), "chupa": self.t(i, c["chupa"])}
            self.nota(z["corte"]["t"], i, f"la rebanada de ${c['valor']} se pinta de rojo (a escala)")
            self.nota(z["corte"]["chupa"], i, "la manguera se traga la rebanada")
        for c in d.get("cuenta", []):
            z["cuenta"].append({"de": c["de"], "a": c["a"], "t0": self.t(i, c["en"]), "t1": self.t(i, c["hasta"])})
            self.nota(z["cuenta"][-1]["t0"], i, f"el monto baja de ${c['de']} a ${c['a']}")
        if d.get("llanta") is not None:
            z["llanta"] = self.t(i, d["llanta"])
            self.nota(z["llanta"], i, "llega la llanta y raspa la pila")
        if d.get("columnas"):
            c = d["columnas"]
            z["cols"] = {"t": self.t(i, c["en"]), "neq": self.t(i, c["neq"]), "verde": self.t(i, c["verde"]), "a": c["a"], "l1": c.get("l1", "FACTURAS"), "l2": c.get("l2", "GANAS")}
            self.nota(z["cols"]["t"], i, f"la pila se parte en dos columnas a escala: {z['cols']['l1']} ${z['max']} y {z['cols']['l2']} ${c['a']}")
            self.nota(z["cols"]["neq"], i, "sello ≠ entre las columnas")
            self.nota(z["cols"]["verde"], i, "la columna de lo que ganas brilla en verde")

    def subtitulos(self):
        """Grupos de hasta 3 palabras (corte en puntuación); resalta la más larga."""
        caps, g = [], []

        def flush():
            nonlocal g
            if g:
                key = max(g, key=lambda w: len(re.sub(r"\W", "", w[0])))
                caps.append([round(g[0][1] - 0.02, 3), round(g[-1][2] + 0.12, 3), " ".join(f"<em>{w[0]}</em>" if w is key else w[0] for w in g)])
                g = []
        for s in self.sc:
            for w in s["words"]:
                g.append([w[0], round(s["start"] + w[1], 3), round(s["start"] + w[2], 3)])
                if len(g) >= 3 or re.search(r"[.,:?…]$", w[0]) or (len(g) == 2 and len(w[0]) > 7):
                    flush()
        flush()
        for i in range(len(caps) - 1):
            caps[i][1] = min(caps[i][1], caps[i + 1][0] - 0.02)
        return caps


TXT = {"gris": "CHECA", "verde": "ACEPTAR", "amarillo": "CONSIDERAR", "rojo": "RECHAZAR"}


def sonidos(D, T):
    """Efectos oficiales en los momentos clave (ms). Se pueden reemplazar con «sonidos» en a3.json."""
    ev = []
    add = lambda n, t, v: ev.append([n, int(round(t * 1000)), v])
    for ln in D["lines"][1:]:
        add("whoosh", ln["start"], .5)
    add("thump", 0, .9)
    add("whoosh", D["total"] - .05, .5)
    for g in D["golpes"]:
        if g["color"] in ("rojo", "#EF4444"):
            add("thump", g["a"] + .05, .6)
    for l in D["lavado"]:
        add({"rojo": "rechazar", "verde": "aceptar"}.get(l["color"], "sello"), l["t"], .6)
    if D["popEntra"] < D["total"]:
        add("popup-entra", D["popEntra"], .7)
    if D.get("peEntra") is not None:
        add("popup-entra", D["peEntra"], .7)
        add("considerar", D["peEntra"] + .1, .35)
    for ln in D["lines"]:
        if ln.get("pe"):
            if ln["pe"]["bump"] is not None:
                add("sello", ln["pe"]["bump"], .4)
    prev = {}
    for ln in D["lines"]:
        pp = ln.get("popup")
        if not pp:
            continue
        for e in pp["estados"]:
            if e["t"] > ln["start"] - .5 and e["nivel"] != "gris" and not any(l["t"] == e["t"] for l in D["lavado"]) and e["t"] > D["popEntra"]:
                add({"verde": "aceptar", "amarillo": "considerar", "rojo": "rechazar"}[e["nivel"]], e["t"], .4)
            if isinstance(e["neta"], dict):
                add("numero-sube", e["neta"]["cuenta"]["t0"], .5)
            if e.get("bump") is not None:
                add("sello", e["bump"], .4)
        if pp["fichas"] not in (None, 1):
            add("popup-entra", pp["fichas"], .4)
    for m in D["ml"]:
        add("logo-semaforo", m, .45)
    z = D.get("din")
    if z:
        if z["corte"]:
            add("thump", z["corte"]["t"], .7)
            add("whoosh", z["corte"]["chupa"], .6)
        for c in z["cuenta"]:
            add("numero-sube", c["t0"], .45)
        if z["llanta"] is not None:
            add("whoosh", z["llanta"] - .5, .4)
            add("thump", z["llanta"], .5)
        if z["cols"]:
            add("whoosh", z["cols"]["t"], .45)
            add("sello", z["cols"]["neq"], .55)
            add("aceptar", z["cols"]["verde"], .4)
    for ln in D["lines"]:
        if ln.get("metas"):
            add("numero-sube", ln["metas"]["km"], .35)
            add("numero-sube", ln["metas"]["hora"], .35)
        if ln.get("tel") and ln["tel"]["cae"] is not None:
            add("whoosh", ln["tel"]["cae"], .4)
    for s in D["scan"]:
        add("whoosh", s[0], .35)
    for o in D["ojo"]:
        add("whoosh", o["a"], .3)
    for c in D["cursor"]:
        add("sello", c["ban"], .6)
    for s in D["stamp"]:
        add("sello", s["t"], .5)
    add("logo-semaforo", D["cta"]["t"] + .15, .7)
    if D["cta"]["gp"] is not None:
        add("sello", D["cta"]["gp"], .4)
    ev.sort(key=lambda e: e[1])
    return {"duracion": D["total"], "fade_out": [round(D["total"] - .25, 2), .25], "eventos": ev}


def construir(proyecto, ruta_fuentes):
    T = json.load(open(os.path.join(proyecto, "tiempos.json"), encoding="utf-8"))
    A = json.load(open(os.path.join(proyecto, "a3.json"), encoding="utf-8"))
    C = Compilador(T, A)
    D = C.compilar()
    cabeza = open(os.path.join(AQUI, "cabeza.html"), encoding="utf-8").read().replace("__FUENTES__", ruta_fuentes)
    js = open(os.path.join(AQUI, "runtime.js"), encoding="utf-8").read()
    html = cabeza + "<script>\nconst D=" + json.dumps(D, ensure_ascii=False) + ";\n" + js + "</script></body></html>\n"
    os.makedirs(os.path.join(proyecto, "a3"), exist_ok=True)
    open(os.path.join(proyecto, "a3", "index.html"), "w", encoding="utf-8").write(html)
    S = A.get("sonidos") or sonidos(D, T)
    json.dump(S, open(os.path.join(proyecto, "a3", "sonidos.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    return D, C.plan
