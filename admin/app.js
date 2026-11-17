// Checa Admin: centro de control, salud y calidad (E6/E7). Sin dependencias propias; habla solo con las funciones
// public.admin_* (comprueban el rol en el servidor y registran cada consulta). Todo dato se pinta con textContent
// (nada de innerHTML con datos). No guarda nada en el navegador. Ningún dato inventado: sin datos → gris «sin datos».
(function () {
  "use strict";
  var cli = null, yo = null, vista = "inicio", param = null, pag = { errores: 1, ofertas: 1 }, F = {}, dias = 7;
  var SVG = "http://www.w3.org/2000/svg";
  // [id, nombre completo, nombre corto para la barra inferior del teléfono]
  var MODULOS = [["inicio", "Inicio · Salud", "Salud"], ["errores", "Errores", "Errores"], ["ofertas", "Ofertas", "Ofertas"], ["versiones", "Versiones", "Versiones"],
                 ["dispositivos", "Conductores y dispositivos", "Equipos"], ["ia", "Auditoría IA", "IA"], ["config", "Configuración", "Ajustes"]];
  var ESTADO = { operativo: "ok", degradado: "aviso", fuera_de_servicio: "mal", sin_comprobar: "nd", sin_datos: "nd", no_aplicable: "nd",
                 verde: "ok", amarillo: "aviso", amarillo_parcial: "aviso", rojo: "mal", gris: "nd" };
  var TEXTO = { operativo: "Operativo", degradado: "Degradado", fuera_de_servicio: "Fuera de servicio", sin_comprobar: "Sin comprobar",
                sin_datos: "Sin datos", no_aplicable: "No aplicable", verde: "Saludable", amarillo: "Degradado", amarillo_parcial: "Sin comprobar todo",
                rojo: "Incidente", gris: "Sin datos" };

  function el(tag, attrs, hijos) {
    var e = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (attrs[k] === null || attrs[k] === undefined) return;
      if (k === "texto") e.textContent = attrs[k];
      else if (k === "clase") e.className = attrs[k];
      else if (k.slice(0, 2) === "on") e.addEventListener(k.slice(2), attrs[k]);
      else e.setAttribute(k, attrs[k]);
    });
    (hijos || []).forEach(function (h) { if (h) e.appendChild(typeof h === "string" ? document.createTextNode(h) : h); });
    return e;
  }
  function sv(tag, attrs, hijos) {
    var e = document.createElementNS(SVG, tag);
    Object.keys(attrs || {}).forEach(function (k) { e.setAttribute(k, attrs[k]); });
    (hijos || []).forEach(function (h) { e.appendChild(h); });
    return e;
  }
  function aviso(t) { var a = document.getElementById("aviso"); a.textContent = t || ""; a.hidden = !t; }
  function txt(v) { return v === null || v === undefined || v === "" ? "—" : String(v); }
  function pct(v) { return v === null || v === undefined ? "—" : (Number(v) * 100).toFixed(1) + " %"; }
  function iso(d) { return d.toISOString().slice(0, 10); }
  function rango() {
    return { desde: (F.desde || iso(new Date(Date.now() - dias * 864e5))) + "T00:00:00Z", hasta: (F.hasta || iso(new Date(Date.now() + 864e5))) + "T00:00:00Z" };
  }
  function esc(s) { return s; }
  function punto(estado) { return el("span", { clase: "punto " + (ESTADO[estado] || "nd"), title: TEXTO[estado] || estado }); }
  function insignia(estado) { return el("span", { clase: "insignia " + (ESTADO[estado] || "nd"), texto: TEXTO[estado] || estado }); }

  async function llamar(fn, args) {
    aviso("");
    var r;
    try { r = await cli.rpc(fn, args || {}); } catch (e) { aviso("Sin conexión con el servidor. Intenta de nuevo."); return null; }
    if (r.error) { aviso("Error del servidor: " + (r.error.message || r.error.code || "desconocido")); return null; }
    var d = r.data;
    if (d && d.error) { aviso(d.error === "no_autorizado" ? "No autorizado: tu cuenta no es administradora." : d.error === "no_existe" ? "No existe ese registro." : "Filtro inválido: " + d.error); return null; }
    return d;
  }
  function ir(v, p) { vista = v; param = p || null; if (location.hash !== "#" + v + (p ? "/" + p : "")) { try { history.replaceState(null, "", "#" + v + (p ? "/" + p : "")); } catch (e) { /* sin historial */ } } pintar(); }
  function enlace(texto, v, p) { return el("a", { href: "#" + v + (p ? "/" + p : ""), clase: "enlace", texto: texto, onclick: function (ev) { ev.preventDefault(); ir(v, p); } }); }

  // ---------- componentes visuales ----------
  function tarjeta(t, extra) {
    var est = t.estado || (t.n !== undefined && t.n !== null && t.n < 30 ? "insuficiente" : null);
    var valor = t.valor === undefined || t.valor === null ? (t.k !== undefined && t.n ? (t.k / t.n) : null) : t.valor;
    var texto = valor === null ? "—" : (t.unidad && t.unidad.indexOf("proporci") === 0 ? pct(valor) : String(valor));
    return el("div", { clase: "tarjeta", "data-clave": t.clave, title: t.definicion || "" }, [
      el("div", { clase: "detalle", texto: t.nombre }),
      el("div", { clase: "valor", texto: texto }),
      el("div", { clase: "detalle", texto: (t.k !== undefined && t.k !== null ? t.k + " / " : "") + (t.n !== undefined && t.n !== null ? "n = " + t.n : "") + (t.unidad && t.unidad.indexOf("proporci") !== 0 ? " · " + t.unidad : "") }),
      est ? el("span", { clase: "etiqueta " + (est === "suficiente" ? "ok" : "aviso"), texto: est === "suficiente" ? "muestra suficiente" : "muestra insuficiente" }) : null,
      t.definicion ? el("div", { clase: "def", texto: t.definicion }) : null].concat(extra || []));
  }
  function tabla(cols, filas, vacio) {
    if (!filas.length) return el("div", { clase: "vacio", texto: vacio || "Sin datos en este periodo (no es lo mismo que cero errores)." });
    var t = el("table", {}, [el("thead", {}, [el("tr", {}, cols.map(function (c) { return el("th", { texto: c[0] }); }))]),
      el("tbody", {}, filas.map(function (f) { return el("tr", {}, cols.map(function (c) {
        var v = c[1](f); return el("td", { "data-label": c[0] }, [v instanceof Node ? v : document.createTextNode(txt(v))]); })); }))]);
    return el("div", { clase: "tabla" }, [t]);
  }
  function paginas(clave, d) {
    var max = Math.max(1, Math.ceil(d.total / d.por_pagina));
    return el("div", { clase: "paginas" }, [
      el("button", { id: "p-ant", texto: "‹ Anterior", disabled: pag[clave] <= 1 ? "" : null, onclick: function () { pag[clave]--; pintar(); } }),
      el("span", { id: "p-info", texto: "Página " + d.pagina + " de " + max + " · " + d.total + " en total" }),
      el("button", { id: "p-sig", texto: "Siguiente ›", disabled: pag[clave] >= max ? "" : null, onclick: function () { pag[clave]++; pintar(); } })]);
  }
  // Gráfica de líneas simple en SVG: series [{nombre, puntos:[{x:etiqueta, y:número|null}], clase}]. Sin datos → mensaje.
  function grafica(titulo, series, unidad) {
    var base = series[0] ? series[0].puntos : [];
    var caja = el("div", { clase: "grafica" }, [el("div", { clase: "detalle", texto: titulo })]);
    if (!base.length || !series.some(function (s) { return s.puntos.some(function (p) { return p.y !== null && p.y !== undefined; }); })) {
      caja.appendChild(el("div", { clase: "vacio", texto: "Sin datos" })); return caja;
    }
    var W = 360, H = 120, P = 22, max = 0;
    series.forEach(function (s) { s.puntos.forEach(function (p) { if (p.y > max) max = p.y; }); });
    max = max || 1;
    var svg = sv("svg", { viewBox: "0 0 " + W + " " + H, role: "img", "aria-label": titulo, clase: "svg" });
    svg.setAttribute("class", "svg");
    svg.appendChild(sv("line", { x1: P, y1: H - P, x2: W - 4, y2: H - P, stroke: "currentColor", "stroke-opacity": "0.25" }));
    var t0 = sv("text", { x: 2, y: 12, "font-size": "9", fill: "currentColor" }); t0.textContent = String(max) + (unidad || ""); svg.appendChild(t0);
    series.forEach(function (s, si) {
      var pts = s.puntos.map(function (p, i) { return p.y === null || p.y === undefined ? null :
        [P + (base.length === 1 ? (W - P - 4) / 2 : i * (W - P - 4) / (base.length - 1)), H - P - (p.y / max) * (H - P - 14)]; });
      var d = pts.filter(Boolean).map(function (q, i) { return (i ? "L" : "M") + q[0].toFixed(1) + " " + q[1].toFixed(1); }).join(" ");
      svg.appendChild(sv("path", { d: d, fill: "none", "stroke-width": "2", class: "linea " + (s.clase || "a" + si) }));
      s.puntos.forEach(function (p, i) { if (!pts[i]) return;
        var c = sv("circle", { cx: pts[i][0], cy: pts[i][1], r: "3", class: "pt " + (s.clase || "a" + si) });
        var tt = sv("title"); tt.textContent = s.nombre + " · " + p.x + ": " + p.y + (unidad || ""); c.appendChild(tt); svg.appendChild(c); });
    });
    caja.appendChild(svg);
    caja.appendChild(el("div", { clase: "detalle", texto: series.map(function (s) { return s.nombre; }).join(" · ") + " · " + base[0].x + " → " + base[base.length - 1].x }));
    return caja;
  }
  function selPeriodo(onCambio) {
    return el("div", { clase: "periodo", role: "group", "aria-label": "Periodo" }, [7, 30, 90].map(function (n) {
      return el("button", { "data-dias": n, "aria-pressed": String(dias === n && !F.desde), texto: n + " días", onclick: function () { dias = n; F.desde = F.hasta = null; onCambio(); } });
    }));
  }
  function skeleton(c) { c.appendChild(el("div", { clase: "skeleton", "aria-busy": "true" })); }

  // ---------- 01 Inicio ----------
  async function inicio(c) {
    skeleton(c);
    var r = rango(), d = await llamar("admin_salud", { desde: r.desde, hasta: r.hasta });
    c.textContent = "";
    if (!d) return;
    var inc = d.atencion.length, n = d.componentes.length;
    var ok = d.componentes.filter(function (x) { return x.estado === "operativo"; }).length;
    var comprobados = d.componentes.filter(function (x) { return ["operativo", "degradado", "fuera_de_servicio"].indexOf(x.estado) >= 0; }).length;
    c.appendChild(el("div", { clase: "encabezado" }, [
      el("div", {}, [el("h1", { texto: "SALUD DE CHECA" }), el("div", { clase: "detalle", texto: "Centro de control y monitoreo del ecosistema Checa." })]),
      el("div", { clase: "estado-general", id: "estado-general" }, [insignia(d.estado_general),
        el("div", { clase: "detalle", texto: "Actualizado: " + (d.generado || "").slice(0, 19).replace("T", " ") + " UTC" }),
        el("div", { clase: "detalle", texto: "Periodo: " + r.desde.slice(0, 10) + " → " + r.hasta.slice(0, 10) }),
        el("div", { clase: "detalle", texto: "Componentes operativos: " + ok + " de " + comprobados + " comprobados (de " + n + ")" }),
        el("div", { clase: "detalle", texto: "Incidentes activos: " + inc })]),
      selPeriodo(pintar)]));
    c.appendChild(el("h2", { texto: "Indicadores principales" }));
    c.appendChild(el("div", { clase: "tarjetas", id: "inicio" }, d.indicadores.map(function (i) {
      return tarjeta(i, [i.clave === "calidad_interpretacion" ? enlace("Ver detalle", "ia") : i.clave === "veredictos_pendientes" ? enlace("Ver errores", "errores") :
        i.clave === "lecturas" || i.clave === "tasa_errores" ? enlace("Ver ofertas", "ofertas") : null]);
    })));
    c.appendChild(el("h2", { texto: "ATENCIÓN REQUERIDA" }));
    c.appendChild(el("div", { id: "alertas", clase: "atencion" }, d.atencion.length ? d.atencion.map(function (a) {
      return el("div", { clase: "alerta " + a.gravedad }, [el("strong", { texto: a.gravedad.toUpperCase() + " · " + a.titulo }),
        el("div", { clase: "detalle", texto: a.descripcion + " · componente: " + a.componente }),
        el("div", { clase: "detalle", texto: "Evidencia: " + a.evidencia + " · " + a.estado }),
        enlace("Abrir", a.componente === "revision_ia" ? "ia" : a.componente === "revision_humana" ? "errores" : "ofertas")]);
    }) : [el("div", { clase: "vacio", texto: "Ninguna regla de atención se cumple con los datos del periodo." })]));
    c.appendChild(el("h2", { texto: "Estado de los componentes" }));
    c.appendChild(el("div", { clase: "tarjetas", id: "componentes" }, d.componentes.map(function (x) {
      return el("div", { clase: "tarjeta comp", "data-clave": x.clave }, [
        el("div", { clase: "fila" }, [punto(x.estado), el("strong", { texto: x.nombre })]), insignia(x.estado),
        el("div", { clase: "detalle", texto: "Comprobación: " + txt(x.comprobacion) }),
        el("div", { clase: "detalle", texto: "Última comprobación: " + (x.ultima_comprobacion ? String(x.ultima_comprobacion).slice(0, 19).replace("T", " ") : "—") }),
        x.k !== undefined ? el("div", { clase: "detalle", texto: x.k + " / n = " + x.n }) : null,
        x.ms ? el("div", { clase: "detalle", texto: "Tiempo mediano: " + x.ms + " ms" }) : null,
        el("div", { clase: "detalle", texto: "Depende de: " + ((x.depende_de || []).join(", ") || "—") })]);
    })));
    var p = d.procesamiento;
    c.appendChild(el("h2", { texto: "Salud del procesamiento" }));
    c.appendChild(el("div", { clase: "tarjetas", id: "procesamiento" }, [["Recibidas", p.recibidas], ["Procesadas", p.procesadas], ["Fallidas", p.fallidas],
      ["Pendientes", p.pendientes === null ? "sin dato" : p.pendientes], ["Tiempo mediano (ms)", p.ms_lectura_p50], ["Tiempo p90 (ms)", p.ms_lectura_p90],
      ["Tiempo máximo (ms)", p.ms_lectura_max], ["Reintentos recibidos", p.reintentos]].map(function (x) {
      return el("div", { clase: "tarjeta" }, [el("div", { clase: "detalle", texto: x[0] }), el("div", { clase: "valor", texto: txt(x[1]) })]); })));
    c.appendChild(el("div", { clase: "detalle", texto: p.pendientes_nota }));
    c.appendChild(el("h2", { texto: "Calidad de los resultados" }));
    c.appendChild(el("div", { clase: "tarjetas" }, [["Con referencia humana", d.calidad.con_referencia], ["Sin veredicto", d.calidad.sin_veredicto], ["Conflictos humanos", d.calidad.conflictos]].map(function (x) {
      return el("div", { clase: "tarjeta" }, [el("div", { clase: "detalle", texto: x[0] }), el("div", { clase: "valor", texto: txt(x[1]) })]); })));
    c.appendChild(el("div", { clase: "detalle", texto: d.calidad.nota }));
    c.appendChild(el("h2", { texto: "Evolución" }));
    var h = d.historia || [];
    c.appendChild(el("div", { clase: "graficas", id: "graficas" }, [
      grafica("Lecturas y errores por día", [{ nombre: "Lecturas", clase: "a0", puntos: h.map(function (x) { return { x: x.dia, y: x.lecturas }; }) },
                                            { nombre: "Errores", clase: "a1", puntos: h.map(function (x) { return { x: x.dia, y: x.errores }; }) }]),
      grafica("Tiempo mediano de lectura", [{ nombre: "ms", clase: "a2", puntos: h.map(function (x) { return { x: x.dia, y: x.ms_p50 }; }) }], " ms"),
      grafica("Costo de la revisión con IA", [{ nombre: "USD", clase: "a3", puntos: (d.costos_historia || []).map(function (x) { return { x: x.dia, y: Number(x.usd) }; }) }], " USD")]));
  }

  // ---------- 02 Errores ----------
  function filtrosBase(extra) {
    var r = rango();
    var nodo = el("div", { clase: "filtros" }, [
      el("label", {}, ["Desde", el("input", { id: "f-desde", type: "date", value: r.desde.slice(0, 10) })]),
      el("label", {}, ["Hasta", el("input", { id: "f-hasta", type: "date", value: r.hasta.slice(0, 10) })]),
      el("label", {}, ["App", el("select", { id: "f-app" }, [el("option", { value: "", texto: "Todas" }), el("option", { value: "uber", texto: "Uber" }), el("option", { value: "didi", texto: "DiDi" })])]),
      el("label", {}, ["Versión", el("input", { id: "f-version", placeholder: "10.81", size: "6" })])].concat(extra || []).concat([
      el("button", { id: "f-aplicar", texto: "Aplicar", onclick: function () {
        ["f-desde", "f-hasta", "f-app", "f-version", "f-tipo", "f-estado", "f-resultado"].forEach(function (id) { var x = document.getElementById(id); if (x) F[id] = x.value; });
        F.desde = F["f-desde"] || null; F.hasta = F["f-hasta"] || null; pag.errores = pag.ofertas = 1; pintar(); } })]));
    ["f-app", "f-version", "f-tipo", "f-estado", "f-resultado"].forEach(function (id) { var x = nodo.querySelector("#" + id); if (x && F[id] !== undefined) x.value = F[id]; });
    return nodo;
  }
  function opciones(id, etiqueta, vals) {
    return el("label", {}, [etiqueta, el("select", { id: id }, vals.map(function (v) { return el("option", { value: v, texto: v || "Todos" }); }))]);
  }
  var VEREDICTOS = ["", "referencia_doble", "referencia_adjudicada", "referencia_humana", "conflicto_humano", "solo_ia", "heredado_no_revisado", "sin_veredicto"];
  function valor(id) { return F[id] ? F[id] : null; }

  async function errores(c) {
    if (param) return expediente(c, Number(param));
    c.appendChild(filtrosBase([opciones("f-tipo", "Tipo", ["", "diferencia", "rescate", "desacuerdo", "reporte"]), opciones("f-estado", "Veredicto", VEREDICTOS)]));
    skeleton(c);
    var r = rango(), d = await llamar("admin_errores", { desde: r.desde, hasta: r.hasta, app: valor("f-app"), version: valor("f-version"),
      tipo: valor("f-tipo"), estado_veredicto: valor("f-estado"), pagina: pag.errores, por_pagina: 50 });
    var sk = c.querySelector(".skeleton"); if (sk) sk.remove();
    if (!d) return;
    c.appendChild(tabla([["Error", function (f) { return enlace("#" + f.id, "errores", String(f.id)); }], ["Fecha", function (f) { return (f.creado || "").slice(0, 16); }],
      ["Tipo", function (f) { return f.tipo; }], ["App", function (f) { return f.app; }],
      ["Versión", function (f) { return enlace(txt(f.version), "versiones"); }],
      ["Lector", function (f) { var l = f.lector_lectura || {}; return [l.tarifa, l.km, l.min].map(txt).join(" · "); }],
      ["IA", function (f) { var l = f.ia_lectura || {}; return l.oferta === false ? "no era oferta" : [l.tarifa, l.km, l.min].map(txt).join(" · "); }],
      ["Veredicto", function (f) { return f.estado_veredicto; }],
      ["Referencia humana", function (f) { var x = f.referencia; return x ? [x.clase, x.tarifa, x.km, x.min].map(txt).join(" · ") : null; }],
      ["Captura", function (f) { return f.tiene_captura ? "sí (acceso restringido)" : "no"; }]], d.filas));
    c.appendChild(paginas("errores", d));
  }
  var ESTADOS_SEG = ["nuevo", "pendiente_de_revision", "en_investigacion", "causa_identificada", "corregido", "verificado", "cerrado"];
  var CAUSAS = ["", "ocr", "interpretacion", "tarjeta_equivocada", "pantalla_cambio", "ia", "dato_ambiguo", "otra"];
  async function expediente(c, id) {
    c.appendChild(el("button", { id: "volver", texto: "‹ Errores", onclick: function () { ir("errores"); } }));
    var e = await llamar("admin_error", { id: id });
    if (!e) return;
    c.appendChild(el("h1", { id: "exp-titulo", texto: "Error #" + e.id + " · " + e.app + " · " + e.version }));
    c.appendChild(el("div", { clase: "detalle", texto: "Clasificación: " + e.clasificacion + " · tipo " + e.tipo + " · conductor " + e.conductor + " · " + (e.creado || "").slice(0, 19) }));
    var lec = e.lector || {}, ia = e.ia || {}, hu = e.veredicto_humano || {};
    c.appendChild(el("div", { clase: "comparar", id: "comparar" }, [
      el("div", { clase: "tarjeta" }, [el("strong", { texto: "Lector (OCR + Parser)" }), el("div", { texto: [lec.tarifa, lec.km, lec.min].map(txt).join(" · ") })]),
      el("div", { clase: "tarjeta" }, [el("strong", { texto: "IA (revisión)" }), el("div", { texto: ia.oferta === false ? "no era oferta" : [ia.tarifa, ia.km, ia.min].map(txt).join(" · ") })]),
      el("div", { clase: "tarjeta" }, [el("strong", { texto: "Veredicto humano" }), el("div", { texto: hu.estado || "—" }),
        el("div", { texto: hu.clase ? [hu.clase, hu.tarifa, hu.km, hu.min].map(txt).join(" · ") : "sin referencia" })]),
      el("div", { clase: "tarjeta" }, [el("strong", { texto: "Veredicto heredado" }), el("div", { texto: txt(e.veredicto_heredado) }), el("div", { clase: "detalle", texto: "no es verdad humana" })])]));
    c.appendChild(el("h2", { texto: "Evidencia original" }));
    c.appendChild(el("div", { clase: "detalle", id: "evidencia", texto: "Captura: " + (e.evidencia.captura ? "existe" : "no existe") + " (" + e.evidencia.captura_acceso + ") · líneas guardadas: " + e.evidencia.lineas_guardadas + " (no se muestran: traen direcciones)" }));
    var en = e.enlace;
    c.appendChild(el("div", { clase: "detalle", id: "enlace", texto: en
      ? "Enlace: fotograma " + en.fotograma_id + " → lectura " + en.lectura_id + " · captura " + en.captura_vinculo + " · huella sha256 " + txt(en.captura_sha256)
      : "Sin enlace por fotograma (caso anterior a la 10.82 o sin lectura registrada): no se puede comprobar la huella." }));
    c.appendChild(el("h2", { texto: "Relacionados" }));
    c.appendChild(el("div", { clase: "enlaces" }, [enlace("Versión " + e.version + " (" + e.relacionados.misma_version + " errores)", "versiones"), enlace("Conductor " + e.conductor + " (" + e.relacionados.mismo_conductor + ")", "dispositivos"),
      enlace("Auditoría IA", "ia")]));
    c.appendChild(el("h2", { texto: "Seguimiento: " + e.estado_seguimiento }));
    if (yo && yo.rol === "revisor") {
      c.appendChild(el("div", { clase: "filtros", id: "seguimiento" }, [opciones("s-estado", "Estado", ESTADOS_SEG.slice(1)), opciones("s-causa", "Causa", CAUSAS),
        el("label", {}, ["Nota", el("input", { id: "s-nota", maxlength: "300", size: "30" })]),
        el("button", { id: "s-guardar", texto: "Registrar", onclick: async function () {
          var r = await llamar("admin_seguimiento", { id: id, estado: document.getElementById("s-estado").value, causa: document.getElementById("s-causa").value || null,
            nota: document.getElementById("s-nota").value || null });
          if (r) pintar(); } })]));
    } else { c.appendChild(el("div", { clase: "detalle", texto: "Tu rol es de solo lectura: no puedes cambiar el seguimiento." })); }
    c.appendChild(tabla([["Fecha", function (h) { return (h.creado || "").slice(0, 19); }], ["Estado", function (h) { return h.estado; }], ["Causa", function (h) { return h.causa; }],
      ["Nota", function (h) { return h.nota; }], ["Quién", function (h) { return h.admin; }]], e.historial, "Sin cambios registrados todavía."));
  }

  // ---------- 03 Ofertas ----------
  async function ofertas(c) {
    c.appendChild(filtrosBase([opciones("f-resultado", "Resultado", ["", "oferta", "duplicada", "varias", "abstencion", "no_oferta", "descartada", "timeout", "error"]), opciones("f-estado", "Veredicto", VEREDICTOS)]));
    skeleton(c);
    var r = rango(), m = await llamar("admin_metricas", { desde: r.desde, hasta: r.hasta, app: valor("f-app"), version: valor("f-version") });
    var d = await llamar("admin_ofertas", { desde: r.desde, hasta: r.hasta, app: valor("f-app"), version: valor("f-version"),
      resultado: valor("f-resultado"), estado_veredicto: valor("f-estado"), pagina: pag.ofertas, por_pagina: 50 });
    var sk = c.querySelector(".skeleton"); if (sk) sk.remove();
    if (m) {
      var grupos = { operacion: "Operación (telemetría)", calidad_humana: "Calidad contra referencia humana", acuerdo_ia: "Acuerdo IA / lector (no es exactitud)", banco_historico: "Banco histórico (etiquetas heredadas)" };
      var PROP = ["duplicadas", "abstenciones", "captura_exacta", "con_referencia_humana", "precision_humana", "falsos_positivos_humana", "mezcla_tarjetas_humana", "acuerdo_ia_lector", "banco_heredado"];
      Object.keys(grupos).forEach(function (g) {
        var ms = (m.metricas || []).filter(function (x) { return x.grupo === g; });
        if (!ms.length) return;
        c.appendChild(el("h2", { texto: grupos[g] }));
        c.appendChild(el("div", { clase: "tarjetas" }, ms.map(function (x) {
          var v = x.valor === null || x.valor === undefined ? "—" : (PROP.indexOf(x.clave) >= 0 ? pct(x.valor) : String(x.valor));
          return el("div", { clase: "tarjeta", "data-clave": x.clave, title: x.formula + " · población: " + x.poblacion + " · fuente: " + x.fuente }, [
            el("div", { clase: "detalle", texto: x.nombre }), el("div", { clase: "valor", texto: v }),
            el("div", { clase: "detalle", texto: (x.k !== null && x.k !== undefined ? x.k + " / " : "") + "n = " + txt(x.n) }),
            el("span", { clase: "etiqueta " + (x.estado === "suficiente" ? "ok" : x.estado === "no_medible" ? "mal" : "aviso"), texto: x.estado.replace("_", " ") }),
            x.motivo ? el("div", { clase: "detalle", texto: x.motivo }) : null]); })));
      });
    }
    if (!d) return;
    c.appendChild(el("h2", { texto: "Eventos de lectura" }));
    c.appendChild(tabla([["Fecha", function (f) { return (f.ts_evento || "").slice(0, 19); }], ["App", function (f) { return f.app; }],
      ["Versión", function (f) { return f.version_app; }], ["Lector", function (f) { return f.lector; }], ["Resultado", function (f) { return f.resultado; }],
      ["Motivo", function (f) { return f.motivo; }], ["Tarifa · km · min", function (f) { return [f.tarifa, f.km, f.min].map(txt).join(" · "); }],
      ["Semáforo", function (f) { return f.nivel; }], ["Captura", function (f) { return f.captura_vinculo; }], ["Veredicto", function (f) { return f.estado_veredicto; }],
      ["Referencia humana", function (f) { var x = f.referencia; return x ? [x.clase, x.tarifa, x.km, x.min].map(txt).join(" · ") : null; }],
      ["Etapas", function (f) { return (f.etapas || []).map(function (e) { return e.etapa + ":" + e.estado; }).join(" › "); }],
      ["Conductor", function (f) { return f.conductor; }]], d.filas));
    c.appendChild(paginas("ofertas", d));
  }

  // ---------- 04 Versiones ----------
  var elegidas = {};
  async function versiones(c) {
    c.appendChild(el("div", { clase: "encabezado" }, [el("h1", { texto: "Versiones" }), selPeriodo(pintar)]));
    skeleton(c);
    var r = rango(), d = await llamar("admin_versiones", { desde: r.desde, hasta: r.hasta });
    c.textContent = ""; c.appendChild(el("div", { clase: "encabezado" }, [el("h1", { texto: "Versiones" }), selPeriodo(pintar)]));
    if (!d) return;
    c.appendChild(tabla([["Comparar", function (v) { return el("input", { type: "checkbox", "data-version": v.version, checked: elegidas[v.version] ? "" : null,
        onchange: function (ev) { elegidas[v.version] = ev.target.checked; pintarComparacion(d.versiones, cmp); } }); }],
      ["Versión", function (v) { return v.version; }], ["Primera lectura", function (v) { return (v.primera || "").slice(0, 10); }], ["Lecturas", function (v) { return v.lecturas; }],
      ["Errores de proceso", function (v) { return v.errores_proceso; }], ["Errores del lector", function (v) { return v.errores_lector; }],
      ["Con referencia", function (v) { return v.con_referencia; }],
      ["Coinciden (humano)", function (v) { return v.con_referencia ? v.coinciden + " / " + v.con_referencia + " = " + pct(v.coinciden / v.con_referencia) : "sin referencia"; }],
      ["ms pop-up (mediana)", function (v) { return v.ms_popup_p50; }], ["Conductores", function (v) { return v.conductores; }]], d.versiones));
    var cmp = el("div", { id: "comparacion" });
    c.appendChild(el("h2", { texto: "Comparar versiones" })); c.appendChild(cmp);
    c.appendChild(el("div", { clase: "detalle", texto: d.nota_comparacion }));
    pintarComparacion(d.versiones, cmp);
  }
  function pintarComparacion(vs, cont) {
    cont.textContent = "";
    var sel = vs.filter(function (v) { return elegidas[v.version]; });
    if (sel.length < 2) { cont.appendChild(el("div", { clase: "vacio", texto: "Marca dos o más versiones para compararlas." })); return; }
    var aviso2 = [];
    sel.forEach(function (v) { if (v.con_referencia < 30) aviso2.push("La " + v.version + " tiene solo " + v.con_referencia + " casos con referencia (< 30): no se puede declarar mejora ni regresión."); });
    cont.appendChild(tabla([["Versión", function (v) { return v.version; }], ["Lecturas", function (v) { return v.lecturas; }], ["Casos con referencia", function (v) { return v.con_referencia; }],
      ["Calidad (humana)", function (v) { return v.con_referencia ? pct(v.coinciden / v.con_referencia) : "sin referencia"; }],
      ["Tasa de error de proceso", function (v) { return pct(v.lecturas ? v.errores_proceso / v.lecturas : null); }], ["ms pop-up", function (v) { return v.ms_popup_p50; }]], sel));
    cont.appendChild(el("div", { id: "aviso-comparacion", clase: aviso2.length ? "alerta media" : "detalle",
      texto: aviso2.length ? aviso2.join(" ") : "Todas las versiones elegidas tienen n ≥ 30 con referencia. Aun así, los casos no son los mismos en cada versión: confirma la cobertura antes de concluir." }));
  }

  // ---------- 05 Conductores y dispositivos ----------
  async function dispositivos(c) {
    c.appendChild(el("div", { clase: "encabezado" }, [el("h1", { texto: "Conductores y dispositivos" }), selPeriodo(pintar)]));
    skeleton(c);
    var r = rango(), d = await llamar("admin_dispositivos", { desde: r.desde, hasta: r.hasta });
    var sk = c.querySelector(".skeleton"); if (sk) sk.remove();
    if (!d) return;
    c.appendChild(el("h2", { texto: "Dispositivos" }));
    c.appendChild(tabla([["Modelo", function (x) { return x.telefono; }], ["Android", function (x) { return x.android; }], ["Versiones de Checa", function (x) { return (x.versiones || []).join(", "); }],
      ["Lecturas", function (x) { return x.lecturas; }], ["Errores", function (x) { return x.errores; }], ["Abstenciones", function (x) { return x.abstenciones; }],
      ["Última actividad", function (x) { return (x.ultima || "").slice(0, 16); }], ["Muestra", function (x) { return x.muestra_suficiente ? "suficiente" : "insuficiente (< 30)"; }]], d.dispositivos));
    c.appendChild(el("h2", { texto: "Conductores (seudónimo)" }));
    c.appendChild(tabla([["Conductor", function (x) { return x.conductor; }], ["Lecturas", function (x) { return x.lecturas; }], ["Errores de proceso", function (x) { return x.errores; }],
      ["Errores del lector", function (x) { return x.errores_lector; }], ["Versiones", function (x) { return (x.versiones || []).join(", "); }], ["Última actividad", function (x) { return (x.ultima || "").slice(0, 16); }]], d.conductores));
    c.appendChild(el("div", { clase: "detalle", texto: d.nota }));
  }

  // ---------- 06 Auditoría IA ----------
  async function ia(c) {
    c.appendChild(el("div", { clase: "encabezado" }, [el("h1", { texto: "Auditoría IA" }), selPeriodo(pintar)]));
    skeleton(c);
    var r = rango(), d = await llamar("admin_ia", { desde: r.desde, hasta: r.hasta });
    var sk = c.querySelector(".skeleton"); if (sk) sk.remove();
    if (!d) return;
    c.appendChild(el("div", { clase: "tarjetas" }, [["Casos de errores_lector", d.casos], ["Con referencia humana", d.con_referencia_humana],
      ["Acuerdo IA/lector", d.acuerdo_ia_lector.n ? d.acuerdo_ia_lector.coinciden + " / " + d.acuerdo_ia_lector.n + " (no es exactitud)" : "sin datos"]].map(function (x) {
      return el("div", { clase: "tarjeta" }, [el("div", { clase: "detalle", texto: x[0] }), el("div", { clase: "valor", texto: txt(x[1]) })]); })));
    c.appendChild(el("h2", { texto: "Quién acierta contra la referencia humana" }));
    c.appendChild(tabla([["Componente", function (x) { return x.componente + (x.modelo ? " (" + x.modelo + ")" : ""); }], ["Casos con referencia (oferta)", function (x) { return x.n; }],
      ["Coinciden", function (x) { return x.coinciden; }], ["Proporción", function (x) { return x.n ? pct(x.coinciden / x.n) + (x.n < 30 ? " · muestra insuficiente" : "") : "sin referencia"; }]], d.componentes));
    c.appendChild(el("h2", { texto: "Clasificación de discrepancias" }));
    c.appendChild(tabla([["Clase", function (x) { return x[0]; }], ["Casos", function (x) { return x[1]; }]],
      Object.keys(d.discrepancias).map(function (k) { return [k, d.discrepancias[k]]; })));
    c.appendChild(el("h2", { texto: "Fallas de la IA" }));
    c.appendChild(tabla([["Tipo", function (x) { return x[0]; }], ["Veces", function (x) { return x[1]; }]], Object.keys(d.fallas).map(function (k) { return [k, d.fallas[k]]; }), "Sin fallas registradas en el periodo."));
    c.appendChild(el("div", { clase: "detalle", texto: d.nota }));
    c.appendChild(enlace("Ir a los errores para revisar cada caso", "errores"));
  }

  // ---------- 07 Configuración ----------
  async function config(c) {
    c.appendChild(el("div", { clase: "encabezado" }, [el("h1", { texto: "Configuración" }), selPeriodo(pintar)]));
    skeleton(c);
    var r = rango(), g = await llamar("admin_config"), k = await llamar("admin_costos", { desde: r.desde, hasta: r.hasta }), b = await llamar("admin_bitacora", { pagina: 1 });
    var sk = c.querySelector(".skeleton"); if (sk) sk.remove();
    if (g) {
      c.appendChild(el("h2", { texto: "Usuarios y permisos" }));
      c.appendChild(el("div", { clase: "detalle", texto: "Tú: " + g.yo.admin + " · rol " + g.yo.rol }));
      c.appendChild(tabla([["Administrador", function (x) { return x.admin; }], ["Rol", function (x) { return x.rol; }], ["Alta", function (x) { return (x.alta || "").slice(0, 10); }]], g.administradores));
      c.appendChild(el("div", { clase: "detalle", texto: "Permisos — lector: " + g.permisos.lector.join(", ") + " · revisor: " + g.permisos.revisor.join(", ") + ". Las altas se hacen a mano en el editor SQL." }));
      c.appendChild(el("h2", { texto: "Retención y almacenamiento" }));
      c.appendChild(el("div", { clase: "tarjetas" }, [["Capturas almacenadas", g.almacenamiento.capturas_objetos], ["Bytes de capturas", g.almacenamiento.capturas_bytes], ["Lecturas guardadas", g.almacenamiento.lecturas_filas],
        ["Lecturas vencidas", g.retencion.lecturas_vencidas], ["Retención de lecturas (días)", g.retencion.lecturas_dias], ["Retención de capturas (días)", g.retencion.capturas_dias]].map(function (x) {
        return el("div", { clase: "tarjeta" }, [el("div", { clase: "detalle", texto: x[0] }), el("div", { clase: "valor", texto: txt(x[1]) })]); })));
      c.appendChild(el("div", { clase: "detalle", texto: g.retencion.nota }));
      c.appendChild(el("h2", { texto: "Alertas: reglas y umbrales" }));
      c.appendChild(tabla([["Regla", function (x) { return x.regla; }], ["Umbral", function (x) { return x.umbral; }], ["Gravedad", function (x) { return x.gravedad; }]], g.umbrales_alertas));
      c.appendChild(el("div", { clase: "detalle", texto: "Umbrales editables: " + (g.umbrales_editables ? "sí" : "no (fijos y documentados; cambiarlos requiere una migración)") + " · esquema " + g.esquema }));
    }
    if (k) {
      c.appendChild(el("h2", { texto: "Costos de la IA" }));
      c.appendChild(tabla([["Modelo", function (x) { return x.modelo; }], ["Consultas", function (x) { return x.consultas; }], ["Tokens entrada", function (x) { return x.tokens_entrada; }],
        ["Tokens salida", function (x) { return x.tokens_salida; }], ["USD", function (x) { return x.usd; }], ["ms IA (mediana)", function (x) { return x.ms_ia_p50; }]], k.por_modelo, "Sin consumo en el periodo."));
      c.appendChild(tabla([["Modelo", function (x) { return x.modelo; }], ["USD / M tokens entrada", function (x) { return x.usd_mtok_entrada; }], ["USD / M tokens salida", function (x) { return x.usd_mtok_salida; }]], k.tarifas));
      c.appendChild(el("div", { clase: "detalle", texto: k.nota }));
    }
    if (b) {
      c.appendChild(el("h2", { texto: "Historial de acciones" }));
      c.appendChild(tabla([["Fecha", function (f) { return (f.creado || "").slice(0, 19); }], ["Admin", function (f) { return f.admin; }], ["Acción", function (f) { return f.accion; }],
        ["Resultado", function (f) { return f.resultado; }], ["Filas", function (f) { return f.filas; }]], b.filas));
    }
  }

  var VISTAS = { inicio: inicio, errores: errores, ofertas: ofertas, versiones: versiones, dispositivos: dispositivos, ia: ia, config: config };
  var TITULOS = {}; MODULOS.forEach(function (m) { TITULOS[m[0]] = m[1]; });

  async function pintar() {
    var raiz = document.getElementById("raiz");
    raiz.textContent = "";
    var nav = el("nav", { id: "menu", "aria-label": "Módulos" }, MODULOS.map(function (m) {
      return el("button", { id: "t-" + m[0], "aria-selected": String(m[0] === vista), "aria-label": m[1], onclick: function () { ir(m[0]); } },
        [el("span", { clase: "largo", texto: m[1] }), el("span", { clase: "corto", texto: m[2] })]); }));
    var c = el("section", { id: "vista-" + vista, clase: "contenido" });
    raiz.appendChild(nav); raiz.appendChild(c);
    await VISTAS[vista](c);
  }

  async function iniciar(cliente) {
    cli = cliente;
    window.Admin.cliente = cliente;
    var s = null;
    try { s = (await cli.auth.getSession()).data.session; } catch (e) { s = null; }
    var btn = document.getElementById("btn-entrar");
    if (!s) {
      if (btn) btn.onclick = function () { cli.auth.signInWithOAuth({ provider: "google", options: { redirectTo: location.href.split("#")[0] } }); };
      return;
    }
    var h = (location.hash || "").replace(/^#/, "").split("/");
    if (VISTAS[h[0]]) { vista = h[0]; param = h[1] || null; }
    document.getElementById("quien").textContent = "Sesión iniciada";
    var salir = document.getElementById("salir");
    salir.hidden = false;
    salir.onclick = async function () { await cli.auth.signOut(); location.reload(); };
    var g = await llamar("admin_config");
    if (g) yo = g.yo;
    await pintar();
  }

  window.Admin = { iniciar: iniciar, aviso: aviso, pintar: pintar };

  document.addEventListener("DOMContentLoaded", function () {
    if (window.CHECA_CLIENTE) { iniciar(window.CHECA_CLIENTE); return; }
    var sc = document.createElement("script");
    // Archivo real del paquete npm con su huella SRI (npm 2.45.4): si el CDN sirviera otra cosa, el navegador no la ejecuta.
    sc.src = "https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2.45.4/dist/umd/supabase.js";
    sc.integrity = "sha384-0w2KAL2YHP6wKOkUDzkCDGgVvfmHnj02DHeQ6XcHOgTfFsGyonKOpShMH1x6nk9o";
    sc.crossOrigin = "anonymous";
    sc.onload = function () { iniciar(window.supabase.createClient(window.CHECA_CONFIG.url, window.CHECA_CONFIG.anon)); };
    sc.onerror = function () { aviso("No se pudo cargar la biblioteca de Supabase."); };
    document.head.appendChild(sc);
  });
})();
