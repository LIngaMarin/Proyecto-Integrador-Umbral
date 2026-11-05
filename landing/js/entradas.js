// "Entradas": toma las direcciones de config.js y comprueba cada minuto si el
// servicio responde, para mostrar su estado en la puerta correspondiente.
(() => {
  const entradas = window.UMBRAL?.entradas ?? {}
  const puertas = [...document.querySelectorAll('[data-entrada]')]
  const TEXTOS = {
    comprobando: 'Comprobando',
    'en-linea': 'En línea',
    apagado: 'Apagado ahora',
    proximamente: 'Próximamente',
  }

  const poner = (puerta, estado) => {
    puerta.querySelector('.estado').dataset.estado = estado
    puerta.querySelector('.estado b').textContent = TEXTOS[estado]
  }

  // Con no-cors no se puede leer la respuesta, pero basta saber si el servidor contesta.
  async function responde(url) {
    const control = new AbortController()
    const limite = setTimeout(() => control.abort(), 2500)
    try {
      await fetch(url, { mode: 'no-cors', cache: 'no-store', signal: control.signal })
      return true
    } catch {
      return false
    } finally {
      clearTimeout(limite)
    }
  }

  async function revisar() {
    await Promise.all(
      puertas.map(async (puerta) => {
        const url = entradas[puerta.dataset.entrada]
        if (url) poner(puerta, (await responde(url)) ? 'en-linea' : 'apagado')
      }),
    )
  }

  puertas.forEach((puerta) => {
    const url = entradas[puerta.dataset.entrada]
    const boton = puerta.querySelector('.boton')
    if (url) {
      boton.href = url
      poner(puerta, 'comprobando')
    } else {
      boton.removeAttribute('href')
      boton.setAttribute('aria-disabled', 'true')
      boton.textContent = 'Disponible pronto'
      poner(puerta, 'proximamente')
    }
  })

  revisar()
  setInterval(() => {
    if (!document.hidden) revisar()
  }, 60000)
})()
