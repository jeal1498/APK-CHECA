// Composición estilo HyperFrames (GSAP, data-composition-id, timeline en window.__timelines) que corre en vivo en la página.
import { gsap } from "gsap";

export function montarHero(quieto) {
  const raiz = document.querySelector('[data-composition-id="hero-oferta"]');
  if (!raiz) return;
  const q = (s) => raiz.querySelector(s);
  const neto = q("#hf-neto");
  const contador = { v: 95 };
  const tl = gsap.timeline({ paused: true, repeat: -1, repeatDelay: 0.6, defaults: { ease: "expo.out" } });

  // Oferta roja: llega la oferta de Uber y el aviso de Checa salta encima
  tl.set(raiz, { "--c": "#EF4444" })
    .set(".hf-oferta-b, .hf-pop-b", { autoAlpha: 0 })
    .fromTo(q(".hf-oferta-a"), { y: 60, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.5 }, 0)
    .fromTo(q(".hf-pop-a"), { y: -40, scale: 0.92, autoAlpha: 0 }, { y: 0, scale: 1, autoAlpha: 1, duration: 0.45 }, 0.9)
    .fromTo(contador, { v: 95 }, { v: 57, duration: 0.9, ease: "power3.out", onUpdate: () => { neto.textContent = "$" + Math.round(contador.v); } }, 1.0)
    .to(q(".hf-pop-a"), { keyframes: { x: [0, -8, 8, -6, 5, -3, 0] }, duration: 0.45, ease: "none" }, 1.4)
    .fromTo(q(".hf-pill"), { scale: 0.8, autoAlpha: 0 }, { scale: 1, autoAlpha: 1, duration: 0.35 }, 1.55)
    .fromTo(q(".hf-voz-a"), { autoAlpha: 0, y: 10 }, { autoAlpha: 1, y: 0, duration: 0.3 }, 1.6)
    .fromTo(q(".hf-barra-a"), { scaleX: 1 }, { scaleX: 0, duration: 3.2, ease: "none" }, 0.9)
    // Oferta verde: se va la roja y entra una buena
    .to(".hf-oferta-a, .hf-pop-a, .hf-voz-a", { autoAlpha: 0, y: 20, duration: 0.3, ease: "power2.in" }, 4.4)
    .fromTo(q(".hf-oferta-b"), { y: 60, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.5 }, 4.8)
    .fromTo(q(".hf-pop-b"), { y: -40, scale: 0.92, autoAlpha: 0 }, { y: 0, scale: 1, autoAlpha: 1, duration: 0.45 }, 5.5)
    .fromTo(q(".hf-voz-b"), { autoAlpha: 0, y: 10 }, { autoAlpha: 1, y: 0, duration: 0.3 }, 5.9)
    .fromTo(q(".hf-barra-b"), { scaleX: 1 }, { scaleX: 0, duration: 3.2, ease: "none" }, 5.5)
    .to(".hf-oferta-b, .hf-pop-b, .hf-voz-b", { autoAlpha: 0, duration: 0.3 }, 9.2);

  window.__timelines = window.__timelines || {};
  window.__timelines["hero-oferta"] = tl;

  if (quieto) { tl.seek(3.5).pause(); return; }
  tl.play(0);
  raiz.addEventListener("click", () => tl.restart());

  // Entradas suaves de los bloques al llegar a ellos
  const bloques = document.querySelectorAll("[data-entra]");
  if ("IntersectionObserver" in window) {
    bloques.forEach((b) => {
      const hijos = b.querySelectorAll("[data-entra-hijo]");
      gsap.set(hijos, { autoAlpha: 0, y: 18 });
      const io = new IntersectionObserver((es) => {
        if (es[0].isIntersecting) { gsap.to(hijos, { autoAlpha: 1, y: 0, duration: 0.6, stagger: 0.12, ease: "expo.out" }); io.disconnect(); }
      }, { threshold: 0.25 });
      io.observe(b);
    });
  }
}
