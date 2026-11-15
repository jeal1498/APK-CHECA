// Checa Admin MVP (E6) y panel de métricas (E7). Sin dependencias propias; habla solo con las funciones
// public.admin_* (que comprueban el rol y registran cada acción). Todo dato se pinta con textContent: nada de
// innerHTML con datos (evita inyección). No guarda nada en el navegador.
(function () {
  "use strict";
  var cli = null, vista = "inicio", pag = { errores: 1, ofertas: 1 }, F = {};
  var IDS = ["f-desde", "f-hasta", "f-app", "f-version", "f-tipo", "f-estado", "f-resultado"];
  var PROPORCIONES = ["duplicadas", "abstenciones", "captura_exacta", "con_referencia_humana", "precision_humana",
                      "falsos_positivos_humana", "mezcla_tarjetas_humana", "acuerdo_ia_lector", "banco_heredado"];

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
  function aviso(t) { var a = document.getElementById("aviso"); a.textContent = t || ""; a.hidden = !t; }
  function txt(v) { return v === null || v === undefined || v === "" ? "—" : String(v); }
  function iso(d) { return d.toISOString().slice(0, 10); }
  function rango() {
    return { desde: (F["f-desde"] || iso(new Date(Date.now() - 7 * 864e5))) + "T00:00:00Z",
             hasta: (F["f-hasta"] || iso(new Date(Date.now() + 864e5))) + "T00:00:00Z" };
  }
  function valor(id) { return F[id] ? F[id] : null; }

  async function llamar(fn, args) {
    aviso("");
    var r;
    try { r = await cli.rpc(fn, args); } catch (e) { aviso("Sin conexión con el servidor. Intenta de nuevo."); return null; }
    if (r.error) { aviso("Error del servidor: " + (r.error.message || r.error.code || "desconocido")); return null; }
    var d = r.data;
    if (d && d.error) { aviso(d.error === "no_autorizado" ? "No autorizado: tu cuenta no es administradora." : "Filtro inválido: " + d.error); return null; }
    return d;
  }

  function filtrosComunes(extra) {
    var r = rango();
    var nodo = el("div", { clase: "filtros" }, [
      el("label", {}, ["Desde", el("input", { id: "f-desde", type: "date", value: r.desde.slice(0, 10) })]),
      el("label", {}, ["Hasta", el("input", { id: "f-hasta", type: "date", value: r.hasta.slice(0, 10) })]),
      el("label", {}, ["App", el("select", { id: "f-app" }, [el("option", { value: "", texto: "Todas" }),
        el("option", { value: "uber", texto: "Uber" }), el("option", { value: "didi", texto: "DiDi" })])]),
      el("label", {}, ["Versión", el("input", { id: "f-version", placeholder: "10.81", size: "6" })])
    ].concat(extra || []).concat([el("button", { id: "f-aplicar", texto: "Aplicar", onclick: function () {
      IDS.forEach(function (id) { var x = document.getElementById(id); if (x) F[id] = x.value; });
      pag.errores = pag.ofertas = 1; pintar(); } })]));
    // Los filtros elegidos se conservan al cambiar de página o de pestaña.
    IDS.forEach(function (id) { var x = nodo.querySelector("#" + id); if (x && F[id] !== undefined) x.value = F[id]; });
    return nodo;
  }
  function selectEstado() {
    return el("label", {}, ["Veredicto", el("select", { id: "f-estado" }, ["", "referencia_doble", "referencia_humana", "conflicto_humano",
      "solo_ia", "heredado_no_revisado", "sin_veredicto"].map(function (v) { return el("option", { value: v, texto: v || "Todos" }); }))]);
  }
  function tabla(cols, filas) {
    var t = el("table", {}, [el("thead", {}, [el("tr", {}, cols.map(function (c) { return el("th", { texto: c[0] }); }))]),
      el("tbody", {}, filas.map(function (f) { return el("tr", {}, cols.map(function (c) { return el("td", { texto: txt(c[1](f)) }); })); }))]);
    return el("div", { clase: "tabla" }, [t]);
  }
  function paginas(clave, d) {
    var max = Math.max(1, Math.ceil(d.total / d.por_pagina));
    return el("div", { clase: "paginas" }, [
      el("button", { id: "p-ant", texto: "‹ Anterior", disabled: pag[clave] <= 1 ? "" : null, onclick: function () { pag[clave]--; pintar(); } }),
      el("span", { id: "p-info", texto: "Página " + d.pagina + " de " + max + " · " + d.total + " en total" }),
      el("button", { id: "p-sig", texto: "Siguiente ›", disabled: pag[clave] >= max ? "" : null, onclick: function () { pag[clave]++; pintar(); } })
    ]);
  }

  var VISTAS = {
    inicio: async function (c) {
      c.appendChild(filtrosComunes());
      var r = rango(), d = await llamar("admin_inicio", { desde: r.desde, hasta: r.hasta });
      if (!d) return;
      var t = [["Lecturas", d.lecturas], ["Conductores", d.conductores], ["Errores del lector", d.errores_lector],
               ["Errores sin veredicto humano", d.errores_sin_veredicto_humano], ["Reintentos recibidos", d.reintentos_recibidos],
               ["Fallas de la IA", d.fallas_ia], ["Última recepción", d.ultima_recepcion]];
      c.appendChild(el("div", { clase: "tarjetas", id: "inicio" }, t.map(function (x) {
        return el("div", { clase: "tarjeta" }, [el("div", { clase: "detalle", texto: x[0] }), el("div", { clase: "valor", texto: txt(x[1]) })]);
      })));
      c.appendChild(el("h2", { texto: "Por resultado" }));
      c.appendChild(tabla([["Resultado", function (f) { return f[0]; }], ["Eventos", function (f) { return f[1]; }]],
        Object.keys(d.por_resultado || {}).map(function (k) { return [k, d.por_resultado[k]]; })));
      c.appendChild(el("h2", { texto: "Alertas" }));
      c.appendChild(el("ul", { id: "alertas" }, (d.alertas || []).length ? d.alertas.map(function (a) { return el("li", { texto: a }); })
        : [el("li", { texto: "Sin alertas" })]));
    },
    errores: async function (c) {
      c.appendChild(filtrosComunes([el("label", {}, ["Tipo", el("select", { id: "f-tipo" }, ["", "diferencia", "rescate", "desacuerdo", "reporte"]
        .map(function (v) { return el("option", { value: v, texto: v || "Todos" }); }))]), selectEstado()]));
      var r = rango(), d = await llamar("admin_errores", { desde: r.desde, hasta: r.hasta, app: valor("f-app"), version: valor("f-version"),
        tipo: valor("f-tipo"), estado_veredicto: valor("f-estado"), pagina: pag.errores, por_pagina: 50 });
      if (!d) return;
      c.appendChild(tabla([["Id", function (f) { return f.id; }], ["Fecha", function (f) { return (f.creado || "").slice(0, 16); }],
        ["App", function (f) { return f.app; }], ["Versión", function (f) { return f.version; }], ["Tipo", function (f) { return f.tipo; }],
        ["Lector", function (f) { var l = f.lector_lectura || {}; return [l.tarifa, l.km, l.min].map(txt).join(" · "); }],
        ["IA", function (f) { var l = f.ia_lectura || {}; return l.oferta === false ? "no era oferta" : [l.tarifa, l.km, l.min].map(txt).join(" · "); }],
        ["Veredicto", function (f) { return f.estado_veredicto; }],
        ["Referencia humana", function (f) { var x = f.referencia; return x ? [x.clase, x.tarifa, x.km, x.min].map(txt).join(" · ") : null; }],
        ["Captura", function (f) { return f.tiene_captura ? "sí (acceso restringido)" : "no"; }]], d.filas));
      c.appendChild(paginas("errores", d));
    },
    ofertas: async function (c) {
      c.appendChild(filtrosComunes([el("label", {}, ["Resultado", el("select", { id: "f-resultado" }, ["", "oferta", "duplicada", "varias", "abstencion",
        "no_oferta", "descartada", "timeout", "error"].map(function (v) { return el("option", { value: v, texto: v || "Todos" }); }))]), selectEstado()]));
      var r = rango(), d = await llamar("admin_ofertas", { desde: r.desde, hasta: r.hasta, app: valor("f-app"), version: valor("f-version"),
        resultado: valor("f-resultado"), estado_veredicto: valor("f-estado"), pagina: pag.ofertas, por_pagina: 50 });
      if (!d) return;
      c.appendChild(tabla([["Fecha", function (f) { return (f.ts_evento || "").slice(0, 19); }], ["App", function (f) { return f.app; }],
        ["Versión", function (f) { return f.version_app; }], ["Lector", function (f) { return f.lector; }], ["Resultado", function (f) { return f.resultado; }],
        ["Motivo", function (f) { return f.motivo; }], ["Tarifa · km · min", function (f) { return [f.tarifa, f.km, f.min].map(txt).join(" · "); }],
        ["Semáforo", function (f) { return f.nivel; }], ["Captura", function (f) { return f.captura_vinculo; }],
        ["Veredicto", function (f) { return f.estado_veredicto; }],
        ["Referencia humana", function (f) { var x = f.referencia; return x ? [x.clase, x.tarifa, x.km, x.min].map(txt).join(" · ") : null; }],
        ["Etapas", function (f) { return (f.etapas || []).map(function (e) { return e.etapa + ":" + e.estado; }).join(" › "); }],
        ["Conductor", function (f) { return f.conductor; }]], d.filas));
      c.appendChild(paginas("ofertas", d));
    },
    metricas: async function (c) {
      c.appendChild(filtrosComunes());
      var r = rango(), d = await llamar("admin_metricas", { desde: r.desde, hasta: r.hasta, app: valor("f-app"), version: valor("f-version") });
      if (!d) return;
      var grupos = { operacion: "Operación (telemetría)", calidad_humana: "Calidad contra referencia humana",
                     acuerdo_ia: "Acuerdo IA / lector (no es exactitud)", banco_historico: "Banco histórico (etiquetas heredadas)" };
      Object.keys(grupos).forEach(function (g) {
        var ms = (d.metricas || []).filter(function (m) { return m.grupo === g; });
        if (!ms.length) return;
        c.appendChild(el("h2", { texto: grupos[g] }));
        c.appendChild(el("div", { clase: "tarjetas" }, ms.map(function (m) {
          var v = m.valor === null || m.valor === undefined ? "—" :
            (PROPORCIONES.indexOf(m.clave) >= 0 ? (Number(m.valor) * 100).toFixed(1) + " %" : String(m.valor));
          return el("div", { clase: "tarjeta", "data-clave": m.clave, title: m.formula + " · población: " + m.poblacion + " · fuente: " + m.fuente }, [
            el("div", { clase: "detalle", texto: m.nombre }), el("div", { clase: "valor", texto: v }),
            el("div", { clase: "detalle", texto: (m.k !== null && m.k !== undefined ? m.k + " / " : "") + "n = " + txt(m.n) }),
            el("span", { clase: "etiqueta " + m.estado, texto: m.estado.replace("_", " ") }),
            m.motivo ? el("div", { clase: "detalle", texto: m.motivo }) : null]);
        })));
      });
      c.appendChild(el("h2", { texto: "Segmentos (app × versión × fabricante)" }));
      c.appendChild(tabla([["App", function (s) { return s.app; }], ["Versión", function (s) { return s.version; }], ["Fabricante", function (s) { return s.fabricante; }],
        ["n", function (s) { return s.n; }], ["Ofertas", function (s) { return s.ofertas; }], ["Abstenciones", function (s) { return s.abstenciones; }],
        ["Muestra", function (s) { return s.suficiente ? "suficiente" : "insuficiente (< 30)"; }]], d.segmentos || []));
      c.appendChild(el("h2", { texto: "Tendencia diaria" }));
      c.appendChild(tabla([["Día", function (t) { return t.dia; }], ["Lecturas", function (t) { return t.lecturas; }],
        ["Ofertas", function (t) { return t.ofertas; }], ["Duplicadas", function (t) { return t.duplicadas; }]], d.tendencia || []));
    },
    bitacora: async function (c) {
      var d = await llamar("admin_bitacora", { pagina: 1 });
      if (!d) return;
      c.appendChild(tabla([["Fecha", function (f) { return (f.creado || "").slice(0, 19); }], ["Admin", function (f) { return f.admin; }],
        ["Acción", function (f) { return f.accion; }], ["Resultado", function (f) { return f.resultado; }], ["Filas", function (f) { return f.filas; }]], d.filas));
    }
  };

  async function pintar() {
    var raiz = document.getElementById("raiz");
    raiz.textContent = "";
    raiz.appendChild(el("nav", {}, Object.keys(VISTAS).map(function (k) {
      return el("button", { id: "t-" + k, "aria-selected": String(k === vista), texto: k[0].toUpperCase() + k.slice(1),
        onclick: function () { vista = k; pintar(); } });
    })));
    var c = el("section", { id: "vista-" + vista });
    raiz.appendChild(c);
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
    document.getElementById("quien").textContent = "Sesión iniciada";
    var salir = document.getElementById("salir");
    salir.hidden = false;
    salir.onclick = async function () { await cli.auth.signOut(); location.reload(); };
    await pintar();
  }

  window.Admin = { iniciar: iniciar, aviso: aviso, pintar: pintar };

  // Arranque (sin script en línea: lo prohíbe la CSP). En pruebas se inyecta window.CHECA_CLIENTE.
  document.addEventListener("DOMContentLoaded", function () {
    if (window.CHECA_CLIENTE) { iniciar(window.CHECA_CLIENTE); return; }
    var sc = document.createElement("script");
    sc.src = "https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2.45.4/dist/umd/supabase.min.js";
    sc.crossOrigin = "anonymous";
    sc.onload = function () { iniciar(window.supabase.createClient(window.CHECA_CONFIG.url, window.CHECA_CONFIG.anon)); };
    sc.onerror = function () { aviso("No se pudo cargar la biblioteca de Supabase."); };
    document.head.appendChild(sc);
  });
})();
