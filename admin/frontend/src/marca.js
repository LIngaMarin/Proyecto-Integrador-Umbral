export function aplicarMarca(marca) {
  if (!marca) return
  const raiz = document.documentElement.style
  raiz.setProperty('--color-primario', marca.color_primario)
  raiz.setProperty('--color-secundario', marca.color_secundario)
}
