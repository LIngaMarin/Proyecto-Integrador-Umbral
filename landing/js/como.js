// "Cómo funciona": el carril azul avanza con el scroll y cada paso se enciende al alcanzarlo.
(() => {
  const lista = document.querySelector('.pasos')
  if (!lista || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const pasos = [...lista.querySelectorAll('.paso')]
  let programado = false

  function actualizar() {
    programado = false
    const ancla = window.innerHeight * 0.55
    const r = lista.getBoundingClientRect()
    const avance = Math.min(1, Math.max(0, (ancla - r.top) / r.height))
    lista.style.setProperty('--avance', avance.toFixed(3))
    pasos.forEach((paso) => paso.classList.toggle('activo', paso.getBoundingClientRect().top < ancla))
  }

  const pedir = () => {
    if (!programado) {
      programado = true
      requestAnimationFrame(actualizar)
    }
  }

  window.addEventListener('scroll', pedir, { passive: true })
  window.addEventListener('resize', pedir)
  actualizar()
})()
