package com.umbral.app.ui.alumno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.PreguntaPractica
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val error: String? = null,
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
        error = null,
    )

class PracticaViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(PracticaUiState())
    val state: StateFlow<PracticaUiState> = _state.asStateFlow()

    /** Ultimo tema pedido, para poder reintentar si fallo. */
    private var ultimoTema: String? = null

    init {
        cargarSemana()
        cargarTemas()
        cargarMarcas()
    }

    fun cambiarModo(modo: ModoPractica) {
        _state.update { it.copy(modo = modo).conPreguntas(emptyList(), null) }
        ultimoTema = null
        if (modo == ModoPractica.SEMANA) cargarSemana()
        if (modo == ModoPractica.TEMA && _state.value.temas.isEmpty()) cargarTemas()
    }

    /** Boton Reintentar: vuelve a pedir lo que fallo segun el modo. */
    fun reintentar() {
        _state.update { it.copy(error = null) }
        if (_state.value.modo == ModoPractica.SEMANA) {
            cargarSemana()
        } else {
            val tema = ultimoTema
            if (tema != null) elegirTema(tema) else cargarTemas()
        }
    }

    private fun cargarSemana() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            repo.preguntasSemana()
                .onSuccess { lista ->
                    _state.update { actual ->
                        if (actual.modo == ModoPractica.SEMANA) actual.conPreguntas(lista, null) else actual.copy(cargando = false)
                    }
                }
                .onFailure { e -> _state.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    private fun cargarTemas() {
        viewModelScope.launch {
            repo.temas()
                .onSuccess { lista -> _state.update { it.copy(temas = lista) } }
                .onFailure { e -> _state.update { it.copy(error = mensajeDe(e)) } }
        }
    }

    /** Dificultad y favoritas guardadas antes. Si falla, se empieza sin marcas (no bloquea). */
    private fun cargarMarcas() {
        viewModelScope.launch {
            repo.marcasPractica().onSuccess { marcas ->
                _state.update { it.copy(dificultades = marcas.dificultades, favoritas = marcas.favoritas) }
            }
        }
    }

    fun elegirTema(tema: String) {
        ultimoTema = tema
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            repo.preguntasPorTema(tema)
                .onSuccess { lista ->
                    _state.update { actual ->
                        if (actual.modo == ModoPractica.TEMA) actual.conPreguntas(lista, tema) else actual.copy(cargando = false)
                    }
                }
                .onFailure { e -> _state.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    fun volverATemas() {
        ultimoTema = null
        _state.update { it.conPreguntas(emptyList(), null) }
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
        // Se registra para el progreso del alumno (si falla la red, la practica sigue).
        viewModelScope.launch { repo.registrarRespuesta(pregunta.id, opcion, acierto) }
    }

    fun marcarDificultad(dificultad: Dificultad) {
        val s = _state.value
        val pregunta = s.preguntas.getOrNull(s.indice) ?: return
        _state.value = s.copy(dificultades = s.dificultades + (pregunta.id to dificultad))
        viewModelScope.launch { repo.marcarDificultad(pregunta.id, dificultad) }
    }

    fun alternarFavorita() {
        val s = _state.value
        val pregunta = s.preguntas.getOrNull(s.indice) ?: return
        val esFavorita = pregunta.id !in s.favoritas
        val nuevas = if (esFavorita) s.favoritas + pregunta.id else s.favoritas - pregunta.id
        _state.value = s.copy(favoritas = nuevas)
        viewModelScope.launch { repo.marcarFavorita(pregunta.id, esFavorita) }
    }

    fun siguiente() {
        val s = _state.value
        if (s.eleccion == null) return
        val proximo = s.indice + 1
        _state.value = if (proximo >= s.preguntas.size) {
            s.copy(terminado = true)
        } else {
            s.copy(indice = proximo, eleccion = null)
        }
    }

    fun practicarDeNuevo() {
        _state.update { it.copy(indice = 0, eleccion = null, correctas = 0, terminado = false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PracticaViewModel(contenedor().repositorio) }
        }
    }
}
