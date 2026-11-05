// Aplica los colores que la academia configuró en el panel de administración.
export function aplicarMarca(marca) {
  if (!marca) return
  const raiz = document.documentElement.style
  if (marca.color_primario) raiz.setProperty('--color-primario', marca.color_primario)
}
