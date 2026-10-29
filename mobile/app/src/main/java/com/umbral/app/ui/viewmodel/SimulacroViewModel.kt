package com.umbral.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbral.app.data.local.PreguntasPracticasEntity
import com.umbral.app.data.local.SimulacroResultadoEntity
import com.umbral.app.data.repository.SimulacroRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SimulacroUiState {
    data object Loading : SimulacroUiState
    data class Error(val mensaje: String) : SimulacroUiState
    data class Success(val resultado: SimulacroResultadoEntity) : SimulacroUiState
    data class Resolver(
        val preguntas: List<PreguntasPracticasEntity>,
        val indiceActual: Int = 0,
        val respuestasMap: Map<Long, String> = emptyMap(),
        val tiempoSegundos: Int = 0,
        val enviando: Boolean = false,
        val errorEnvio: String? = null
    ) : SimulacroUiState {
        val preguntaActual: PreguntasPracticasEntity?
            get() = preguntas.getOrNull(indiceActual)
        val esUltimaPregunta: Boolean
            get() = indiceActual == preguntas.size - 1
    }
}

class SimulacroViewModel(
    private val repository: SimulacroRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SimulacroUiState>(SimulacroUiState.Loading)
    val uiState: StateFlow<SimulacroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var simulacroIdActual: Long = 1L

    fun cargarSimulacro(simulacroId: Long, preguntasIniciales: List<PreguntasPracticasEntity>? = null) {
        simulacroIdActual = simulacroId
        viewModelScope.launch {
            _uiState.value = SimulacroUiState.Loading
            val preguntas = if (!preguntasIniciales.isNullOrEmpty()) {
                preguntasIniciales
            } else {
                val rem = repository.cargarPreguntas(simulacroId)
                if (rem.isNotEmpty()) rem else obtenerPreguntasEjemplo()
            }

            _uiState.value = SimulacroUiState.Resolver(
                preguntas = preguntas,
                indiceActual = 0,
                respuestasMap = emptyMap(),
                tiempoSegundos = 0
            )

            iniciarTemporizador()
        }
    }

    private fun iniciarTemporizador() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _uiState.value
                if (currentState is SimulacroUiState.Resolver && !currentState.enviando) {
                    _uiState.value = currentState.copy(tiempoSegundos = currentState.tiempoSegundos + 1)
                }
            }
        }
    }

    fun seleccionarRespuesta(preguntaId: Long, respuesta: String) {
        val currentState = _uiState.value as? SimulacroUiState.Resolver ?: return
        val nuevasRespuestas = currentState.respuestasMap.toMutableMap()
        nuevasRespuestas[preguntaId] = respuesta

        _uiState.value = currentState.copy(respuestasMap = nuevasRespuestas)

        viewModelScope.launch {
            repository.guardarRespuestaTemporal(
                preguntaId = preguntaId,
                simulacroId = simulacroIdActual,
                respuesta = respuesta
            )
        }
    }

    fun irAPregunta(index: Int) {
        val currentState = _uiState.value as? SimulacroUiState.Resolver ?: return
        if (index in currentState.preguntas.indices) {
            _uiState.value = currentState.copy(indiceActual = index)
        }
    }

    fun siguientePregunta() {
        val currentState = _uiState.value as? SimulacroUiState.Resolver ?: return
        if (currentState.indiceActual < currentState.preguntas.size - 1) {
            _uiState.value = currentState.copy(indiceActual = currentState.indiceActual + 1)
        }
    }

    fun anteriorPregunta() {
        val currentState = _uiState.value as? SimulacroUiState.Resolver ?: return
        if (currentState.indiceActual > 0) {
            _uiState.value = currentState.copy(indiceActual = currentState.indiceActual - 1)
        }
    }

    fun enviarSimulacro(alumnoId: Long = 1L) {
        val currentState = _uiState.value as? SimulacroUiState.Resolver ?: return
        if (currentState.enviando) return

        _uiState.value = currentState.copy(enviando = true, errorEnvio = null)

        viewModelScope.launch {
            val res = repository.enviarSimulacro(
                simulacroId = simulacroIdActual,
                alumnoId = alumnoId,
                tiempoSegundos = currentState.tiempoSegundos,
                respuestasMap = currentState.respuestasMap,
                preguntasTotales = currentState.preguntas
            )

            res.onSuccess { resultado ->
                timerJob?.cancel()
                _uiState.value = SimulacroUiState.Success(resultado)
            }.onFailure { ex ->
                _uiState.value = currentState.copy(
                    enviando = false,
                    errorEnvio = ex.message ?: "Error al enviar el simulacro. Inténtalo de nuevo."
                )
            }
        }
    }

    private fun obtenerPreguntasEjemplo(): List<PreguntasPracticasEntity> {
        return listOf(
            PreguntasPracticasEntity(
                id = 1,
                tema = "Álgebra",
                enunciado = "Si 2x + 6 = 14, ¿cuál es el valor de x?",
                opcionA = "3",
                opcionB = "4",
                opcionC = "5",
                opcionD = "6",
                respuestaCorrecta = "B"
            ),
            PreguntasPracticasEntity(
                id = 2,
                tema = "Geometría",
                enunciado = "¿Cuál es la suma de los ángulos internos de un triángulo?",
                opcionA = "90°",
                opcionB = "180°",
                opcionC = "270°",
                opcionD = "360°",
                respuestaCorrecta = "B"
            ),
            PreguntasPracticasEntity(
                id = 3,
                tema = "Aritmética",
                enunciado = "¿Cuál es el MCD de 12 y 18?",
                opcionA = "2",
                opcionB = "3",
                opcionC = "6",
                opcionD = "12",
                respuestaCorrecta = "C"
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
