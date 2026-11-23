package com.umbral.app.ui.alumno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.PreguntaPractica
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.Etiqueta
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.TituloSeccion
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.Acento
import com.umbral.app.ui.theme.AcentoOscuro
import com.umbral.app.ui.theme.ErrorRojo
import com.umbral.app.ui.theme.Superficie
import com.umbral.app.ui.theme.Tinta
import com.umbral.app.ui.theme.TintaSuave
import com.umbral.app.ui.theme.TextoFuerte
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ModoPractica { SEMANA, TEMA }

data class PracticaUiState(
    val modo: ModoPractica = ModoPractica.SEMANA,
    val cargando: Boolean = false,
    val temas: List<String> = emptyList(),
    val temaElegido: String? = null,
    val preguntas: List<PreguntaPractica> = emptyList(),
    val indice: Int = 0,
    val eleccion: Int? = null,
    val correctas: Int = 0,
    val terminado: Boolean = false,
    val dificultades: Map<Int, Dificultad> = emptyMap(),
    val favoritas: Set<Int> = emptySet(),
)

private fun PracticaUiState.conPreguntas(lista: List<PreguntaPractica>, tema: String?): PracticaUiState =
    copy(
        cargando = false,
        preguntas = lista,
        temaElegido = tema,
        indice = 0,
        eleccion = null,
        correctas = 0,
        terminado = false,
    )

class PracticaViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(PracticaUiState())
    val state: StateFlow<PracticaUiState> = _state.asStateFlow()

    init {
        cargarSemana()
        cargarTemas()
    }

    fun cambiarModo(modo: ModoPractica) {
        _state.value = _state.value.copy(modo = modo).conPreguntas(emptyList(), null)
        if (modo == ModoPractica.SEMANA) cargarSemana()
    }

    private fun cargarSemana() {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true)
            val lista = repo.preguntasSemana()
            val actual = _state.value
            _state.value = if (actual.modo == ModoPractica.SEMANA) {
                actual.conPreguntas(lista, null)
            } else {
                actual.copy(cargando = false)
            }
        }
    }

    private fun cargarTemas() {
        viewModelScope.launch {
            val lista = repo.temas()
            _state.value = _state.value.copy(temas = lista)
        }
    }

    fun elegirTema(tema: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true)
            val lista = repo.preguntasPorTema(tema)
            val actual = _state.value
            _state.value = if (actual.modo == ModoPractica.TEMA) {
                actual.conPreguntas(lista, tema)
            } else {
                actual.copy(cargando = false)
            }
        }
    }

    fun volverATemas() {
        _state.value = _state.value.conPreguntas(emptyList(), null)
    }

    fun elegirOpcion(opcion: Int) {
        val s = _state.value
        val pregunta = s.preguntas.getOrNull(s.indice) ?: return
        if (s.eleccion != null || s.terminado) return
        val acierto = opcion == pregunta.correcta
        _state.value = s.copy(
            eleccion = opcion,
            correctas = if (acierto) s.correctas + 1 else s.correctas,
        )
    }

    fun marcarDificultad(dificultad: Dificultad) {
        val s = _state.value
        val pregunta = s.preguntas.getOrNull(s.indice) ?: return
        _state.value = s.copy(dificultades = s.dificultades + (pregunta.id to dificultad))
    }

    fun alternarFavorita() {
        val s = _state.value
        val pregunta = s.preguntas.getOrNull(s.indice) ?: return
        val nuevas = if (pregunta.id in s.favoritas) s.favoritas - pregunta.id else s.favoritas + pregunta.id
        _state.value = s.copy(favoritas = nuevas)
    }

    fun siguiente() {
        val s = _state.value
        if (s.eleccion == null) return
        val proximo = s.indice + 1
        val nuevo = if (proximo >= s.preguntas.size) {
            s.copy(terminado = true)
        } else {
            s.copy(indice = proximo, eleccion = null)
        }
        _state.value = nuevo
    }

    fun practicarDeNuevo() {
        _state.value = _state.value.copy(indice = 0, eleccion = null, correctas = 0, terminado = false)
    }
}

private val margenH = Modifier.padding(horizontal = 16.dp)
private val dificultadesElegibles = listOf(Dificultad.FACIL, Dificultad.MEDIA, Dificultad.DIFICIL)

@Composable
fun PracticaScreen(repo: UmbralRepository) {
    val vm: PracticaViewModel = viewModel(factory = vmFactory { PracticaViewModel(repo) })
    val estado by vm.state.collectAsState()
    val pregunta = estado.preguntas.getOrNull(estado.indice)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Encabezado(titulo = "Práctica", subtitulo = "Resuelve preguntas y revisa el solucionario") }

        item {
            Row(modifier = margenH, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PestanaBoton(
                    texto = "De la semana",
                    seleccionada = estado.modo == ModoPractica.SEMANA,
                    onClick = { vm.cambiarModo(ModoPractica.SEMANA) },
                    modifier = Modifier.weight(1f),
                )
                PestanaBoton(
                    texto = "Por tema",
                    seleccionada = estado.modo == ModoPractica.TEMA,
                    onClick = { vm.cambiarModo(ModoPractica.TEMA) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (estado.cargando) {
            item { CircularProgressIndicator(modifier = margenH) }
        }

        if (estado.modo == ModoPractica.TEMA && estado.temaElegido == null) {
            item { TituloSeccion("Elige un tema", modifier = margenH) }
            if (!estado.cargando && estado.temas.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "Sin temas",
                        detalle = "Aún no hay temas disponibles para practicar.",
                        modifier = margenH,
                    )
                }
            }
            items(estado.temas) { tema ->
                Tarjeta(modifier = margenH.clickable { vm.elegirTema(tema) }) {
                    Text(tema, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        } else if (estado.preguntas.isEmpty()) {
            if (!estado.cargando) {
                item {
                    EstadoVacio(
                        titulo = "Sin preguntas",
                        detalle = "No hay preguntas para esta selección por ahora.",
                        modifier = margenH,
                    )
                }
                if (estado.modo == ModoPractica.TEMA) {
                    item {
                        TextButton(onClick = { vm.volverATemas() }, modifier = margenH) { Text("Cambiar tema") }
                    }
                }
            }
        } else if (estado.terminado) {
            item {
                val resumen = "${estado.correctas} de ${estado.preguntas.size} correctas"
                Tarjeta(modifier = margenH) {
                    Text("Resultado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        resumen,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = "Resultado: $resumen" },
                    )
                    Button(onClick = { vm.practicarDeNuevo() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Practicar de nuevo")
                    }
                    if (estado.modo == ModoPractica.TEMA) {
                        TextButton(onClick = { vm.volverATemas() }) { Text("Cambiar tema") }
                    }
                }
            }
        } else if (pregunta != null) {
            item { BloquePregunta(estado = estado, pregunta = pregunta, vm = vm) }
        }
    }
}

@Composable
private fun PestanaBoton(texto: String, seleccionada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (seleccionada) {
        Button(onClick = onClick, modifier = modifier) { Text(texto) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(texto) }
    }
}

/** Colores (fondo, texto) de una opción según el estado de la respuesta. */
private fun colorOpcion(indice: Int, correcta: Int, eleccion: Int?): Pair<Color, Color> {
    return when {
        eleccion == null -> Pair(Superficie, TextoFuerte)
        indice == correcta -> Pair(Acento, Tinta)
        indice == eleccion -> Pair(ErrorRojo, Color.White)
        else -> Pair(Superficie, TextoFuerte)
    }
}

@Composable
private fun BloquePregunta(estado: PracticaUiState, pregunta: PreguntaPractica, vm: PracticaViewModel) {
    val eleccion = estado.eleccion
    val respondida = eleccion != null

    Column(modifier = margenH, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Etiqueta(
                texto = "${pregunta.tema} · ${estado.indice + 1} de ${estado.preguntas.size}",
                fondo = TintaSuave,
                textoColor = Tinta,
            )
            if (estado.modo == ModoPractica.TEMA) {
                TextButton(onClick = { vm.volverATemas() }) { Text("Cambiar tema") }
            }
        }

        Tarjeta {
            Text(
                pregunta.enunciado,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            pregunta.opciones.forEachIndexed { i, texto ->
                val colores = colorOpcion(i, pregunta.correcta, eleccion)
                val letra = 'A' + i
                Button(
                    onClick = { vm.elegirOpcion(i) },
                    enabled = !respondida,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colores.first,
                        contentColor = colores.second,
                        disabledContainerColor = colores.first,
                        disabledContentColor = colores.second,
                    ),
                ) {
                    Text("$letra) $texto")
                }
            }
        }

        if (eleccion != null) {
            val acerto = eleccion == pregunta.correcta
            val textoResultado = if (acerto) {
                "¡Correcta!"
            } else {
                "Incorrecta. La correcta es ${'A' + pregunta.correcta}."
            }
            Text(
                textoResultado,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (acerto) AcentoOscuro else ErrorRojo,
                modifier = Modifier.semantics { contentDescription = textoResultado },
            )

            Tarjeta {
                Text("Solucionario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                pregunta.solucionario.forEachIndexed { i, paso ->
                    Text("${i + 1}. $paso", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("¿Qué tan difícil te pareció?", style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dificultadesElegibles.forEach { d ->
                    val seleccionada = estado.dificultades[pregunta.id] == d
                    if (seleccionada) {
                        Button(onClick = { vm.marcarDificultad(d) }, modifier = Modifier.weight(1f)) {
                            Text(d.texto)
                        }
                    } else {
                        OutlinedButton(onClick = { vm.marcarDificultad(d) }, modifier = Modifier.weight(1f)) {
                            Text(d.texto)
                        }
                    }
                }
            }

            val favorita = estado.favoritas.contains(pregunta.id)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { vm.alternarFavorita() },
                    modifier = Modifier.semantics {
                        contentDescription = if (favorita) "Quitar de favoritas" else "Marcar como favorita"
                    },
                ) {
                    Text(if (favorita) "⭐ Favorita" else "☆ Marcar favorita")
                }
                Button(onClick = { vm.siguiente() }, enabled = respondida) { Text("Siguiente") }
            }
        }
    }
}
