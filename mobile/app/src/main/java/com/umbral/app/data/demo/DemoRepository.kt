package com.umbral.app.data.demo

import com.umbral.app.domain.AlumnoSeccion
import com.umbral.app.domain.AvisoInasistencia
import com.umbral.app.domain.Clase
import com.umbral.app.domain.Comparativo
import com.umbral.app.domain.CuentaDemo
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.Duda
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.domain.EstadoAsistencia
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.MarcasPractica
import com.umbral.app.domain.PreguntaPractica
import com.umbral.app.domain.ProgresoCurso
import com.umbral.app.domain.RegistroAsistencia
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.domain.Rol
import com.umbral.app.domain.Sesion
import com.umbral.app.domain.SimulacroProgramado
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.Usuario
import com.umbral.app.domain.UmbralRepository
import kotlinx.coroutines.delay

private const val SEDE = "Sede Miraflores"
private const val SECCION_A = "Ciclo Ordinario · A"

private const val TEMA_FRACCIONES = "Fracciones"
private const val TEMA_ECUACIONES = "Ecuaciones lineales"
private const val TEMA_TRIANGULOS = "Triángulos"
private const val TEMA_RAZONAMIENTO = "Razonamiento lógico"
private const val TEMA_CINEMATICA = "Cinemática"
private const val TEMA_LECTURA = "Comprensión lectora"

/** Ids de las preguntas que forman la práctica semanal (un tema distinto por pregunta). */
private val IDS_SEMANA = listOf(2, 5, 9, 14, 17, 22)

private fun pregunta(
    id: Int,
    tema: String,
    enunciado: String,
    opciones: List<String>,
    correcta: Int,
    solucionario: List<String>,
    dificultad: Dificultad,
): PreguntaPractica = PreguntaPractica(
    id = id,
    tema = tema,
    enunciado = enunciado,
    opciones = opciones,
    correcta = correcta,
    solucionario = solucionario,
    dificultad = dificultad,
)

/** Clave de todas las cuentas de demostracion. */
const val CLAVE_DEMO = "umbral123"

/** Cuentas que el login muestra como "entrar rapido" en modo demo. */
val CUENTAS_DEMO: List<CuentaDemo> = listOf(
    CuentaDemo("camila@umbral.pe", CLAVE_DEMO, Usuario(1, "Camila Rojas", Rol.ALUMNO, "Ciclo Ordinario · Sección A")),
    CuentaDemo("marcos@umbral.pe", CLAVE_DEMO, Usuario(2, "Marcos Quispe", Rol.DOCENTE, "Matemática y Razonamiento")),
    CuentaDemo("rosa@umbral.pe", CLAVE_DEMO, Usuario(3, "Rosa Medina", Rol.APODERADO, "Apoderada de Camila y Mateo")),
)

/**
 * Implementación en memoria de [UmbralRepository] para la demo.
 * Solo las dudas y los avisos cambian de estado; el resto son listas inmutables.
 */
class DemoRepository(
    /** Espera simulada de "red" para que se vea el estado Cargando. En pruebas se usa 0. */
    private val retardoMs: Long = 300,
) : UmbralRepository {

    private val listaHorario: List<Clase> = listOf(
        Clase(1, "Aritmética", "A", "A-201", SEDE, 1, "08:00", "09:30", "Marcos Quispe"),
        Clase(2, "Razonamiento Verbal", "A", "A-203", SEDE, 1, "09:45", "11:15", "Lucía Huamán"),
        Clase(3, "Física", "A", "A-105", SEDE, 2, "08:00", "09:30", "Jorge Vilca"),
        Clase(4, "Álgebra", "A", "A-201", SEDE, 2, "09:45", "11:15", "Marcos Quispe"),
        Clase(5, "Química", "A", "L-102", SEDE, 3, "08:00", "09:30", "Patricia Ccama"),
        Clase(6, "Geometría", "A", "A-201", SEDE, 3, "09:45", "11:15", "Marcos Quispe"),
        Clase(7, "Lenguaje", "A", "A-203", SEDE, 4, "08:00", "09:30", "Lucía Huamán"),
        Clase(8, "Aritmética", "A", "A-201", SEDE, 4, "09:45", "11:15", "Marcos Quispe"),
        Clase(9, "Física", "A", "A-105", SEDE, 5, "08:00", "09:30", "Jorge Vilca"),
        Clase(10, "Álgebra", "A", "A-201", SEDE, 6, "08:00", "09:30", "Marcos Quispe"),
    )

    private val listaTemas: List<String> = listOf(
        TEMA_FRACCIONES,
        TEMA_ECUACIONES,
        TEMA_TRIANGULOS,
        TEMA_RAZONAMIENTO,
        TEMA_CINEMATICA,
        TEMA_LECTURA,
    )

    private val listaPreguntas: List<PreguntaPractica> = listOf(
        // Fracciones
        pregunta(
            1, TEMA_FRACCIONES, "¿Cuánto es 1/2 + 1/3?",
            listOf("5/6", "2/5", "1/6", "2/6"), 0,
            listOf(
                "El mínimo común múltiplo de 2 y 3 es 6.",
                "Convertimos: 1/2 = 3/6 y 1/3 = 2/6.",
                "Sumamos: 3/6 + 2/6 = 5/6.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            2, TEMA_FRACCIONES, "¿Cuánto es 3/4 × 2/9?",
            listOf("1/6", "5/13", "6/13", "2/3"), 0,
            listOf(
                "Multiplicamos numeradores: 3 × 2 = 6.",
                "Multiplicamos denominadores: 4 × 9 = 36.",
                "Simplificamos 6/36 dividiendo entre 6: 1/6.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            3, TEMA_FRACCIONES, "¿Qué número multiplicado por 2/3 da 4/9?",
            listOf("2/3", "8/27", "3/2", "1/3"), 0,
            listOf(
                "Planteamos: x × 2/3 = 4/9.",
                "Despejamos: x = 4/9 ÷ 2/3 = 4/9 × 3/2.",
                "Operamos: 12/18 = 2/3.",
            ),
            Dificultad.DIFICIL,
        ),
        // Ecuaciones lineales
        pregunta(
            4, TEMA_ECUACIONES, "Resuelve: 2x + 5 = 17",
            listOf("6", "11", "4", "12"), 0,
            listOf(
                "Restamos 5 en ambos lados: 2x = 12.",
                "Dividimos entre 2: x = 6.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            5, TEMA_ECUACIONES, "Resuelve: 3(x − 2) = 2x + 4",
            listOf("10", "2", "6", "-10"), 0,
            listOf(
                "Distribuimos: 3x − 6 = 2x + 4.",
                "Restamos 2x: x − 6 = 4.",
                "Sumamos 6: x = 10.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            6, TEMA_ECUACIONES, "Resuelve: x/4 + x/2 = 9",
            listOf("8", "12", "18", "9"), 1,
            listOf(
                "Sumamos las fracciones: x/4 + 2x/4 = 3x/4.",
                "Igualamos: 3x/4 = 9.",
                "Multiplicamos por 4 y dividimos entre 3: x = 12.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            7, TEMA_ECUACIONES, "Resuelve: 5x − 7 = 2x + 8",
            listOf("5", "3", "-5", "15"), 0,
            listOf(
                "Restamos 2x: 3x − 7 = 8.",
                "Sumamos 7: 3x = 15.",
                "Dividimos entre 3: x = 5.",
            ),
            Dificultad.FACIL,
        ),
        // Triángulos
        pregunta(
            8, TEMA_TRIANGULOS, "Dos ángulos de un triángulo miden 50° y 60°. ¿Cuánto mide el tercer ángulo?",
            listOf("70°", "80°", "60°", "90°"), 0,
            listOf(
                "La suma de los ángulos interiores de un triángulo es 180°.",
                "50° + 60° = 110°.",
                "180° − 110° = 70°.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            9, TEMA_TRIANGULOS, "Un triángulo rectángulo tiene catetos de 6 cm y 8 cm. ¿Cuánto mide la hipotenusa?",
            listOf("12 cm", "14 cm", "10 cm", "7 cm"), 2,
            listOf(
                "Teorema de Pitágoras: h² = 6² + 8².",
                "h² = 36 + 64 = 100.",
                "h = 10 cm.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            10, TEMA_TRIANGULOS, "Un triángulo isósceles tiene dos lados iguales de 5 cm y base de 6 cm. ¿Cuál es su perímetro?",
            listOf("16 cm", "15 cm", "17 cm", "11 cm"), 0,
            listOf(
                "El perímetro es la suma de los tres lados.",
                "5 + 5 + 6 = 16 cm.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            11, TEMA_TRIANGULOS, "Un ángulo exterior de un triángulo mide 120° y uno de los interiores no adyacentes mide 45°. ¿Cuánto mide el otro interior no adyacente?",
            listOf("75°", "65°", "85°", "165°"), 0,
            listOf(
                "El ángulo exterior es igual a la suma de los dos interiores no adyacentes.",
                "120° = 45° + x.",
                "x = 75°.",
            ),
            Dificultad.DIFICIL,
        ),
        // Razonamiento lógico
        pregunta(
            12, TEMA_RAZONAMIENTO, "Completa la serie: 2, 4, 8, 16, ...",
            listOf("32", "24", "20", "30"), 0,
            listOf(
                "Cada término se obtiene multiplicando el anterior por 2.",
                "16 × 2 = 32.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            13, TEMA_RAZONAMIENTO, "Ana es mayor que Beto. Beto es mayor que Carla. ¿Quién es la menor?",
            listOf("Ana", "Carla", "Beto", "No se puede saber"), 1,
            listOf(
                "Ordenamos de mayor a menor: Ana > Beto > Carla.",
                "La última posición corresponde a Carla.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            14, TEMA_RAZONAMIENTO, "Si hoy es miércoles, ¿qué día será dentro de 10 días?",
            listOf("Sábado", "Viernes", "Domingo", "Lunes"), 0,
            listOf(
                "Una semana tiene 7 días, así que 10 = 7 + 3.",
                "Avanzar 7 días deja el mismo día; quedan 3 días.",
                "Miércoles + 3 días = sábado.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            15, TEMA_RAZONAMIENTO, "Completa la serie: 3, 6, 11, 18, 27, ...",
            listOf("36", "38", "40", "45"), 1,
            listOf(
                "Diferencias entre términos consecutivos: 3, 5, 7, 9.",
                "La siguiente diferencia es 11.",
                "27 + 11 = 38.",
            ),
            Dificultad.DIFICIL,
        ),
        // Cinemática
        pregunta(
            16, TEMA_CINEMATICA, "Un móvil se mueve con velocidad constante de 20 m/s durante 5 s. ¿Qué distancia recorre?",
            listOf("100 m", "4 m", "25 m", "50 m"), 0,
            listOf(
                "En MRU, d = v × t.",
                "d = 20 m/s × 5 s = 100 m.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            17, TEMA_CINEMATICA, "Un auto parte del reposo y acelera uniformemente a 4 m/s² durante 6 s. ¿Cuál es su velocidad final?",
            listOf("24 m/s", "10 m/s", "36 m/s", "40 m/s"), 0,
            listOf(
                "En MRUV, v = v0 + a·t.",
                "Con v0 = 0: v = 4 m/s² × 6 s = 24 m/s.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            18, TEMA_CINEMATICA, "Un cuerpo parte del reposo con aceleración constante de 2 m/s². ¿Qué distancia recorre en 5 s?",
            listOf("25 m", "10 m", "50 m", "5 m"), 0,
            listOf(
                "Usamos d = v0·t + (1/2)·a·t².",
                "Con v0 = 0: d = (1/2) · 2 · 5² = 25 m.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            19, TEMA_CINEMATICA, "Un móvil frena desde 30 m/s hasta detenerse en 6 s con aceleración constante. ¿Qué distancia recorre durante la frenada?",
            listOf("90 m", "180 m", "5 m", "45 m"), 0,
            listOf(
                "La aceleración es a = (0 − 30) / 6 = −5 m/s².",
                "d = v0·t + (1/2)·a·t² = 30 · 6 + (1/2) · (−5) · 36 = 180 − 90.",
                "d = 90 m.",
            ),
            Dificultad.DIFICIL,
        ),
        // Comprensión lectora
        pregunta(
            20, TEMA_LECTURA, "En un texto, ¿qué se entiende por idea principal?",
            listOf("El último párrafo del texto", "La idea central que el autor quiere transmitir", "Un dato secundario de apoyo", "La palabra más repetida"), 1,
            listOf(
                "La idea principal resume el mensaje central del texto.",
                "Los datos secundarios apoyan la idea, pero no la resumen.",
            ),
            Dificultad.FACIL,
        ),
        pregunta(
            21, TEMA_LECTURA, "¿Cuál es el sinónimo de 'efímero'?",
            listOf("Pasajero", "Eterno", "Inmenso", "Robusto"), 0,
            listOf(
                "'Efímero' significa de duración muy breve.",
                "'Pasajero' comparte ese sentido de duración limitada.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            22, TEMA_LECTURA, "¿Cuál es el antónimo de 'diligente'?",
            listOf("Negligente", "Aplicado", "Veloz", "Atento"), 0,
            listOf(
                "'Diligente' significa cuidadoso y activo en lo que hace.",
                "'Negligente' significa descuidado: es lo contrario.",
            ),
            Dificultad.MEDIA,
        ),
        pregunta(
            23, TEMA_LECTURA, "En la oración 'Aunque llovió, el partido se jugó', la palabra 'aunque' expresa una relación de:",
            listOf("Concesión", "Causa", "Finalidad", "Condición"), 0,
            listOf(
                "'Aunque' introduce un obstáculo que no impide el hecho.",
                "Esa relación se llama concesiva, así que la respuesta es concesión.",
            ),
            Dificultad.DIFICIL,
        ),
    )

    private val listaProgreso: List<ProgresoCurso> = listOf(
        ProgresoCurso("Aritmética", 14, 20),
        ProgresoCurso("Álgebra", 9, 16),
        ProgresoCurso("Geometría", 6, 12),
        ProgresoCurso("Razonamiento Verbal", 11, 15),
    )

    private val listaTemasDebiles: List<TemaDebil> = listOf(
        TemaDebil("Fracciones", 48),
        TemaDebil("Ecuaciones lineales", 58),
        TemaDebil("Cinemática", 33),
        TemaDebil("Triángulos", 62),
    )

    private val comparativoDemo = Comparativo(miPromedio = 72, promedioSeccion = 68)

    private var dudasEstado: List<Duda> = listOf(
        Duda(1, "Camila Rojas", TEMA_ECUACIONES, "¿Por qué al pasar un número al otro lado de la igualdad cambia de signo?", false, null),
        Duda(2, "Diego Ancajima", TEMA_CINEMATICA, "¿Cómo sé si conviene usar d = v·t o la fórmula con aceleración?", false, null),
        Duda(
            3, "Valeria Soto", TEMA_FRACCIONES,
            "¿Por qué debo buscar el mínimo común múltiplo al sumar fracciones?",
            true,
            "Porque así obtenemos denominadores iguales y podemos sumar los numeradores.",
        ),
        Duda(
            4, "Camila Rojas", TEMA_TRIANGULOS,
            "¿El teorema de Pitágoras sirve para cualquier triángulo?",
            true,
            "Solo para triángulos rectángulos: la relación h² = a² + b² requiere un ángulo de 90°.",
        ),
    )

    private val listaAlumnos: List<AlumnoSeccion> = listOf(
        AlumnoSeccion(1, "Camila Rojas", SECCION_A, 96),
        AlumnoSeccion(2, "Diego Ancajima", SECCION_A, 88),
        AlumnoSeccion(3, "Valeria Soto", SECCION_A, 92),
        AlumnoSeccion(4, "Luis Huamaní", SECCION_A, 74),
        AlumnoSeccion(5, "Fiorella Campos", SECCION_A, 100),
        AlumnoSeccion(6, "Renzo Taipe", SECCION_A, 81),
        AlumnoSeccion(7, "Andrea Quispe", SECCION_A, 85),
        AlumnoSeccion(8, "Sebastián Flores", SECCION_A, 70),
        AlumnoSeccion(9, "María Fernanda Ccori", SECCION_A, 94),
        AlumnoSeccion(10, "Joaquín Paredes", SECCION_A, 77),
        AlumnoSeccion(11, "Brenda Mendoza", SECCION_A, 89),
        AlumnoSeccion(12, "Kevin Condori", SECCION_A, 98),
    )

    private val listaHijos: List<Hijo> = listOf(
        Hijo(1, "Camila Rojas", SECCION_A),
        Hijo(2, "Mateo Rojas", "Ciclo Intensivo · B"),
    )

    private val asistenciaCamila: List<RegistroAsistencia> = listOf(
        RegistroAsistencia("2026-11-03", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-04", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-05", EstadoAsistencia.TARDANZA),
        RegistroAsistencia("2026-11-06", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-09", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-10", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-11", EstadoAsistencia.FALTA),
        RegistroAsistencia("2026-11-12", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-13", EstadoAsistencia.TARDANZA),
        RegistroAsistencia("2026-11-16", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-17", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-18", EstadoAsistencia.PRESENTE),
    )

    private val asistenciaMateo: List<RegistroAsistencia> = listOf(
        RegistroAsistencia("2026-11-03", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-04", EstadoAsistencia.TARDANZA),
        RegistroAsistencia("2026-11-05", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-06", EstadoAsistencia.FALTA),
        RegistroAsistencia("2026-11-09", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-10", EstadoAsistencia.TARDANZA),
        RegistroAsistencia("2026-11-11", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-12", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-13", EstadoAsistencia.PRESENTE),
        RegistroAsistencia("2026-11-16", EstadoAsistencia.TARDANZA),
        RegistroAsistencia("2026-11-17", EstadoAsistencia.FALTA),
        RegistroAsistencia("2026-11-18", EstadoAsistencia.PRESENTE),
    )

    private val resultadosCamila: List<ResultadoSimulacro> = listOf(
        ResultadoSimulacro("Simulacro 1 · Matemática", "2026-09-05", 58),
        ResultadoSimulacro("Simulacro 2 · Razonamiento", "2026-09-26", 63),
        ResultadoSimulacro("Simulacro 3 · Matemática", "2026-10-17", 61),
        ResultadoSimulacro("Simulacro 4 · Integrado", "2026-11-07", 70),
    )

    private val resultadosMateo: List<ResultadoSimulacro> = listOf(
        ResultadoSimulacro("Simulacro 1 · Matemática", "2026-09-05", 49),
        ResultadoSimulacro("Simulacro 2 · Razonamiento", "2026-09-26", 53),
        ResultadoSimulacro("Simulacro 3 · Matemática", "2026-10-17", 57),
        ResultadoSimulacro("Simulacro 4 · Integrado", "2026-11-07", 52),
    )

    private val simulacrosCamila: List<SimulacroProgramado> = listOf(
        SimulacroProgramado("Simulacro 5 · Matemática", "2026-11-28", "09:00", "A-201"),
        SimulacroProgramado("Simulacro 6 · Razonamiento", "2026-12-05", "08:30", "A-203"),
        SimulacroProgramado("Simulacro 7 · Integrado", "2026-12-12", "09:00", "Auditorio"),
    )

    private val simulacrosMateo: List<SimulacroProgramado> = listOf(
        SimulacroProgramado("Simulacro 5 · Matemática", "2026-11-28", "11:00", "B-105"),
        SimulacroProgramado("Simulacro 6 · Razonamiento", "2026-12-05", "11:00", "B-105"),
        SimulacroProgramado("Simulacro 7 · Integrado", "2026-12-12", "11:00", "Auditorio"),
    )

    private var avisosEstado: List<AvisoInasistencia> = listOf(
        AvisoInasistencia(
            1,
            "Mateo faltó a la clase de Física del martes 17 de noviembre. Por favor confirme la asistencia.",
            "2026-11-17",
            false,
        ),
        AvisoInasistencia(
            2,
            "Mateo faltó a la clase del viernes 6 de noviembre.",
            "2026-11-06",
            true,
        ),
    )

    // Lo que el alumno marco en la practica (en memoria)
    private var dificultadesEstado: Map<Int, Dificultad> = emptyMap()
    private var favoritasEstado: Set<Int> = emptySet()

    /** Simula una llamada de red: espera un poco y nunca deja escapar la excepcion. */
    private suspend fun <T> demo(bloque: () -> T): Result<T> {
        if (retardoMs > 0) delay(retardoMs)
        return runCatching(bloque)
    }

    override suspend fun login(correo: String, clave: String): Result<Sesion> = demo {
        val cuenta = CUENTAS_DEMO.firstOrNull { it.correo.equals(correo.trim(), ignoreCase = true) }
        if (cuenta == null || cuenta.clave != clave) throw ErrorUmbral.CredencialesInvalidas()
        Sesion(cuenta.usuario, token = "demo-${cuenta.usuario.id}")
    }

    override suspend fun horarioSemanal(): Result<List<Clase>> = demo { listaHorario }

    override suspend fun preguntasSemana(): Result<List<PreguntaPractica>> = demo {
        IDS_SEMANA.mapNotNull { id -> listaPreguntas.firstOrNull { it.id == id } }
    }

    override suspend fun temas(): Result<List<String>> = demo { listaTemas }

    override suspend fun preguntasPorTema(tema: String): Result<List<PreguntaPractica>> = demo {
        listaPreguntas.filter { it.tema == tema }
    }

    override suspend fun registrarRespuesta(preguntaId: Int, opcion: Int, acerto: Boolean): Result<Unit> = demo { }

    override suspend fun marcasPractica(): Result<MarcasPractica> = demo {
        MarcasPractica(dificultadesEstado, favoritasEstado)
    }

    override suspend fun marcarDificultad(preguntaId: Int, dificultad: Dificultad): Result<Unit> = demo {
        dificultadesEstado = dificultadesEstado + (preguntaId to dificultad)
    }

    override suspend fun marcarFavorita(preguntaId: Int, favorita: Boolean): Result<Unit> = demo {
        favoritasEstado = if (favorita) favoritasEstado + preguntaId else favoritasEstado - preguntaId
    }

    override suspend fun progreso(): Result<List<ProgresoCurso>> = demo { listaProgreso }

    override suspend fun temasDebiles(): Result<List<TemaDebil>> = demo { listaTemasDebiles }

    override suspend fun comparativo(): Result<Comparativo> = demo { comparativoDemo }

    override suspend fun dudas(): Result<List<Duda>> = demo { dudasEstado }

    override suspend fun responderDuda(id: Int, respuesta: String): Result<Unit> = demo {
        dudasEstado = dudasEstado.map { duda ->
            if (duda.id == id) duda.copy(resuelta = true, respuesta = respuesta) else duda
        }
    }

    override suspend fun alumnosSeccion(): Result<List<AlumnoSeccion>> = demo { listaAlumnos }

    override suspend fun hijos(): Result<List<Hijo>> = demo { listaHijos }

    override suspend fun asistenciaHijo(hijoId: Int): Result<List<RegistroAsistencia>> = demo {
        when (hijoId) {
            1 -> asistenciaCamila
            2 -> asistenciaMateo
            else -> emptyList()
        }
    }

    override suspend fun resultadosHijo(hijoId: Int): Result<List<ResultadoSimulacro>> = demo {
        when (hijoId) {
            1 -> resultadosCamila
            2 -> resultadosMateo
            else -> emptyList()
        }
    }

    override suspend fun simulacrosProgramados(hijoId: Int): Result<List<SimulacroProgramado>> = demo {
        when (hijoId) {
            1 -> simulacrosCamila
            2 -> simulacrosMateo
            else -> emptyList()
        }
    }

    override suspend fun avisos(): Result<List<AvisoInasistencia>> = demo { avisosEstado }

    override suspend fun marcarAvisoLeido(id: Int): Result<Unit> = demo {
        avisosEstado = avisosEstado.map { aviso ->
            if (aviso.id == id) aviso.copy(leida = true) else aviso
        }
    }
}
