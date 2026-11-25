import { montarHero } from "./hyperframes.js";
import { montarRemotion } from "./remotion.jsx";

const quieto = window.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
function iniciar() {
  montarHero(quieto);
  montarRemotion(quieto);
  // Botón fijo: aparece al pasar el botón de arriba y se oculta en el cierre
  const fijo = document.getElementById("fijo"), a = document.getElementById("cta1"), b = document.getElementById("cta2");
  let va = true, vb = false;
  if ("IntersectionObserver" in window && fijo && a && b) {
    const up = () => fijo.classList.toggle("on", !va && !vb);
    new IntersectionObserver((e) => { va = e[0].isIntersecting; up(); }).observe(a);
    new IntersectionObserver((e) => { vb = e[0].isIntersecting; up(); }).observe(b);
  }
}
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", iniciar); else iniciar();
