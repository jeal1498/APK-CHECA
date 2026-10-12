(function () {
  'use strict';

  // Ofertas de ejemplo. km y min totales = recogida + viaje (igual que la app).
  var OFFERS = [
    { app: 'Uber', fare: 120, pk: 1.5, pm: 4, tk: 10, tm: 18, surge: '+$15 incluido' },
    { app: 'DiDi', fare: 95, pk: 2.5, pm: 7, tk: 13, tm: 20, surge: '' },
    { app: 'Uber', fare: 62, pk: 4.5, pm: 11, tk: 6, tm: 19, surge: '' }
  ];
  var LABEL = { green: 'ACEPTAR', yellow: 'CONSIDERAR', red: 'RECHAZAR' };
  var settings = { metaKm: 8, metaHora: 150, gasPrice: 24, kml: 12 };

  function money(n) { return '$' + Math.round(n).toLocaleString('es-MX'); }
  function dec(n) { return (Math.round(n * 10) / 10).toLocaleString('es-MX'); }

  function evaluate(o) {
    var km = o.pk + o.tk, min = o.pm + o.tm;
    var perKm = o.fare / km, perHour = o.fare / min * 60;
    var gas = km / settings.kml * settings.gasPrice;
    var okKm = perKm >= settings.metaKm, okH = perHour >= settings.metaHora;
    var color = okKm && okH ? 'green' : (okKm || okH ? 'yellow' : 'red');
    return { km: km, min: min, perKm: perKm, perHour: perHour, gas: gas, net: o.fare - gas, okKm: okKm, okH: okH, color: color };
  }

  function render(pop, o) {
    var r = evaluate(o);
    pop.setAttribute('data-c', r.color);
    pop.innerHTML =
      '<div class="pop-band">' + LABEL[r.color] + '<small>' + o.app + '</small></div>' +
      '<div class="pop-body">' +
        '<div class="pop-net">' + money(r.net) + '<span>netos</span></div>' +
        '<div class="pop-sub">Viaje ' + money(o.fare) + ' · Gasolina −' + money(r.gas) + '</div>' +
        '<div class="chips">' +
          '<div class="chip ' + (r.okH ? 'ok' : 'no') + '"><b>' + money(r.perHour) + '</b>por hora</div>' +
          '<div class="chip ' + (r.okKm ? 'ok' : 'no') + '"><b>$' + dec(r.perKm) + '</b>por km</div>' +
          '<div class="chip"><b>' + r.min + ' min</b>' + dec(r.km) + ' km</div>' +
        '</div>' +
        '<div class="surge">' + (o.surge ? '⚡ Dinámica ' + o.surge : '') + '</div>' +
      '</div>';
    pop.setAttribute('aria-label', 'Checa dice ' + LABEL[r.color] + ': ' + money(r.net) + ' netos');
  }

  function fillOffer(card, o) {
    card.innerHTML = '<span>' + o.app + ' · Nuevo viaje</span><b>' + money(o.fare) + '</b>' +
      'Recogida ' + o.pm + ' min (' + dec(o.pk) + ' km) · Viaje ' + o.tm + ' min (' + dec(o.tk) + ' km)' +
      '<div class="ln"></div><div class="ln s"></div>';
  }

  // Hero: rota las 3 ofertas
  var heroPop = document.getElementById('hero-pop');
  var heroOffer = document.getElementById('hero-offer');
  var hi = 0;
  function heroShow() { render(heroPop, OFFERS[hi]); fillOffer(heroOffer, OFFERS[hi]); }
  if (heroPop) {
    heroShow();
    var still = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!still) setInterval(function () { hi = (hi + 1) % OFFERS.length; heroShow(); }, 3800);
  }

  // Demo interactiva
  var demoPop = document.getElementById('demo-pop');
  var btns = document.querySelectorAll('.offer-btn');
  var current = 0;
  function demoShow() {
    render(demoPop, OFFERS[current]);
    btns.forEach(function (b, i) { b.setAttribute('aria-pressed', i === current ? 'true' : 'false'); });
  }
  btns.forEach(function (b, i) {
    var o = OFFERS[i];
    b.innerHTML = '<span class="fare">' + money(o.fare) + '</span><span class="app">' + o.app + '</span>' +
      '<span class="det">Recogida ' + dec(o.pk) + ' km · ' + o.pm + ' min &nbsp;|&nbsp; Viaje ' + dec(o.tk) + ' km · ' + o.tm + ' min</span>';
    b.addEventListener('click', function () { current = i; demoShow(); });
  });
  function bindRange(id, key, fmt) {
    var el = document.getElementById(id), out = document.getElementById(id + '-out');
    if (!el) return;
    function upd() { settings[key] = parseFloat(el.value); out.textContent = fmt(settings[key]); demoShow(); }
    el.addEventListener('input', upd); upd();
  }
  if (demoPop) {
    bindRange('r-km', 'metaKm', function (v) { return '$' + dec(v) + '/km'; });
    bindRange('r-h', 'metaHora', function (v) { return '$' + v + '/h'; });
    bindRange('r-gas', 'gasPrice', function (v) { return '$' + dec(v) + '/l'; });
    demoShow();
  }

  // Calculadora de ahorro
  var calc = document.getElementById('calc');
  if (calc) {
    var f = function (id) { return parseFloat(document.getElementById(id).value) || 0; };
    var upd = function () {
      var perTrip = (f('c-good') - f('c-bad')) * f('c-km');
      var day = perTrip * f('c-n');
      var month = day * f('c-days');
      document.getElementById('c-month').textContent = money(Math.max(0, month));
      document.getElementById('c-detail').textContent =
        money(Math.max(0, perTrip)) + ' por viaje × ' + f('c-n') + ' al día = ' + money(Math.max(0, day)) + ' al día, por ' + f('c-days') + ' días.';
    };
    calc.addEventListener('input', upd); upd();
  }

  // Versión del APK (si falla, el botón sigue funcionando)
  var metas = document.querySelectorAll('.dl-meta');
  if (metas.length && window.fetch) {
    fetch('descargar/version.json', { cache: 'no-store' })
      .then(function (r) { if (!r.ok) throw 0; return r.json(); })
      .then(function (v) {
        var parts = ['Versión ' + v.version];
        if (v.fecha) {
          var d = new Date(v.fecha + 'T12:00:00');
          parts.push(isNaN(d) ? v.fecha : d.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' }));
        }
        if (v.tamano) parts.push(v.tamano);
        var txt = parts.join(' · ') + ' · Android 8+';
        metas.forEach(function (m) { m.textContent = txt; });
      })
      .catch(function () {});
  }

  var y = document.getElementById('year');
  if (y) y.textContent = new Date().getFullYear();
})();
