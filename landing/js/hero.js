// Portada: la puerta crece con el scroll hasta cubrir la pantalla, y la cabecera
// cambia de color según la sección que tenga debajo.
(() => {
  const reducido = window.matchMedia('(prefers-reduced-motion: reduce)')
  const cabecera = document.querySelector('.cabecera')
  const hero = document.querySelector('.hero')
  const escena = hero?.querySelector('.hero-escena')
  const arco = hero?.querySelector('.arco')
  const luz = hero?.querySelector('.arco-luz')
  const dentro = hero?.querySelector('.hero-dentro')
  const texto = hero?.querySelector('.hero-texto')
  if (!cabecera || !hero || !escena || !arco) return

  const acotar = (v, a = 0, b = 1) => Math.min(b, Math.max(a, v))
  const suave = (a, b, v) => {
    const t = acotar((v - a) / (b - a))
    return t * t * (3 - 2 * t)
  }
  const curva = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2)

  let geometria = null
  let progreso = 0
  let programado = false

  // Mide la puerta sin transformar para saber cuánto debe crecer y hacia dónde.
  function medir() {
    arco.style.transform = 'none'
    const r = arco.getBoundingClientRect()
    const e = escena.getBoundingClientRect()
    geometria = {
      dx: e.width / 2 - (r.left - e.left + r.width / 2),
      dy: e.height / 2 - (r.top - e.top + r.height / 2),
      // Suficiente para cubrir también las esquinas, donde la puerta es redondeada.
      escala: Math.max((e.width * 1.8) / r.width, (e.height + e.width * 0.3) / r.height),
    }
  }

  function pintarCabecera() {
    const y = cabecera.offsetHeight / 2
    let tema = 'claro'
    document.querySelectorAll('[data-tema]').forEach((seccion) => {
      if (seccion === cabecera) return
      const r = seccion.getBoundingClientRect()
      if (r.top <= y && r.bottom > y) {
        tema = seccion === hero ? (progreso > 0.5 ? 'oscuro' : 'claro') : seccion.dataset.tema
      }
    })
    cabecera.dataset.tema = tema
  }

  function actualizar() {
    programado = false
    const recorrido = hero.offsetHeight - window.innerHeight
    progreso = !reducido.matches && recorrido > 0 ? acotar(-hero.getBoundingClientRect().top / recorrido) : 0

    if (geometria && !reducido.matches) {
      const e = curva(progreso)
      const k = Math.pow(geometria.escala, e)
      arco.style.transform = `translate(${geometria.dx * e}px, ${geometria.dy * e}px) scale(${k})`
      luz.style.opacity = String(1 - suave(0.5, 0.92, progreso))
      const salida = suave(0, 0.35, progreso)
      texto.style.opacity = String(1 - salida)
      texto.style.transform = `translateY(${-36 * salida}px)`
      const entra = suave(0.6, 0.88, progreso)
      dentro.style.opacity = String(entra)
      dentro.style.transform = `translateY(${24 * (1 - entra)}px)`
      texto.style.visibility = progreso > 0.4 ? 'hidden' : ''
    }
    pintarCabecera()
  }

  const pedir = () => {
    if (!programado) {
      programado = true
      requestAnimationFrame(actualizar)
    }
  }

  const reiniciar = () => {
    medir()
    actualizar()
  }

  window.addEventListener('scroll', pedir, { passive: true })
  window.addEventListener('resize', reiniciar)
  reducido.addEventListener('change', () => {
    arco.style.transform = ''
    luz.style.opacity = ''
    texto.style.cssText = ''
    dentro.style.cssText = ''
    reiniciar()
  })
  reiniciar()
  // Las fuentes web cambian el alto del texto y, con él, la posición de la puerta.
  document.fonts?.ready.then(reiniciar)
})()
