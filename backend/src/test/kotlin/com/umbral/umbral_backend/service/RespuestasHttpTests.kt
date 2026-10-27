package com.umbral.umbral_backend.service

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.*
import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.*
import java.time.Instant
import java.util.Date
import java.security.SecureRandom

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RespuestasHttpTests {
    @LocalServerPort var port: Int = 0
    @Autowired lateinit var preguntas: PreguntaRepository
    @Autowired lateinit var respuestas: RespuestaPracticaRepository
    @Autowired lateinit var soluciones: SolucionarioRepository
    @Autowired lateinit var dudas: DudaRepository
    @Autowired lateinit var grupos: GrupoDudasRepository
    @Autowired lateinit var respuestasGrupo: RespuestaGrupoRepository
    @Autowired lateinit var historialDudas: HistorialDudaRepository
    @Autowired lateinit var resultados: ResultadoSimulacroRepository
    @Autowired lateinit var resumenes: ResumenSemanalRepository
    @Autowired lateinit var programador: ProgramadorResumen
    @Autowired lateinit var estadisticas: EstadisticasSemanaService
    @Autowired lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate
    @Autowired lateinit var correo: CorreoResumen

    companion object {
        val secret = ByteArray(48).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        @JvmStatic @DynamicPropertySource fun config(r: DynamicPropertyRegistry) {
            r.add("umbral.jwt.secret") { secret }
            r.add("umbral.resumen.enabled") { "true" }
        }
    }

    fun token(user: Long, type: String = "access", expires: Instant = Instant.now().plusSeconds(600)): String {
        val claims = JWTClaimsSet.Builder().claim("user_id", user.toString()).claim("token_type", type)
            .expirationTime(Date.from(expires)).build()
        return SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims).also { it.sign(MACSigner(secret)) }.serialize()
    }
    fun request(method: String, path: String, user: Long? = 2, body: String? = null, bearer: String? = null): HttpResponse<String> {
        val b = HttpRequest.newBuilder(URI("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
        (bearer ?: user?.let { token(it) })?.let { b.header("Authorization", "Bearer $it") }
        return HttpClient.newHttpClient().send(b.method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) }
            ?: HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString())
    }

    fun pregunta(estado: EstadoPregunta = EstadoPregunta.APROBADA, docenteId: Long = 1): Pregunta = preguntas.saveAndFlush(Pregunta(
        enunciado = "2x + 6 = 14", tema = "HTTP", dificultad = Dificultad.FACIL,
        opciones = listOf("2","4"), respuestaCorrecta = "4", estado = estado, docenteId = docenteId))

    @Test fun `registra respuestas usando identidad verificada y valida permisos y opciones`() {
        val p = pregunta()
        val body = """{"preguntaId":${p.id},"respuesta":"4"}"""
        val ok = request("POST", "/api/practicas/respuestas", body = body)
        assertEquals(201, ok.statusCode(), ok.body())
        assertTrue(respuestas.existsByAlumnoIdAndPreguntaId(2, p.id))
        assertTrue(ok.body().contains("\"correcta\":true"))
        assertEquals(401, request("POST", "/api/practicas/respuestas", user = null, body = body).statusCode())
        assertEquals(403, request("POST", "/api/practicas/respuestas", user = 1, body = body).statusCode())
        assertEquals(403, request("POST", "/api/practicas/respuestas", user = 4, body = body).statusCode())
        assertEquals(400, request("POST", "/api/practicas/respuestas", body = body.replace("\"4\"", "\"9\"")).statusCode())
        assertEquals(400, request("POST", "/api/practicas/respuestas", body = """{"preguntaId":0,"respuesta":" "}""").statusCode())
        val pending = pregunta(EstadoPregunta.PENDIENTE)
        assertEquals(404, request("POST", "/api/practicas/respuestas", body = body.replace(p.id.toString(),pending.id.toString())).statusCode())
    }
    @Test fun `rechaza firma invalida tokens expirados y refresh`() {
        for (jwt in listOf(token(2,"refresh"),token(2,expires=Instant.now().minusSeconds(120)),token(2).dropLast(10)+"aaaaaaaaaa"))
            assertEquals(401, request("POST","/api/practicas/respuestas",bearer=jwt,body="{}").statusCode())
    }

    @Test fun `solucion aprobada exige intento propio y bloquea rutas docentes alternativas`() {
        val p = pregunta()
        val ruta = "/api/practicas/preguntas/${p.id}/solucionario"
        assertEquals(403,request("GET",ruta).statusCode())
        assertEquals(403,request("GET","/api/docente/preguntas?docenteId=1").statusCode())
        assertEquals(403,request("GET","/api/docente/preguntas?docenteId=1",user=5).statusCode())
        assertEquals(200,request("GET","/api/docente/preguntas?docenteId=1",user=1).statusCode())
        val s = soluciones.saveAndFlush(Solucionario(pregunta=p,pasoAPaso="Restar 6 y dividir entre 2"))
        request("POST","/api/practicas/respuestas",body="""{"preguntaId":${p.id},"respuesta":"2"}""")
        assertEquals(404,request("GET",ruta).statusCode())
        s.estado=EstadoSolucionario.APROBADO
        soluciones.saveAndFlush(s)
        val ok=request("GET",ruta)
        assertEquals(200,ok.statusCode(),ok.body())
        assertTrue(ok.body().contains("\"respuestaCorrecta\":\"4\""))
        assertTrue(ok.body().contains("Restar 6"))
        assertEquals(403,request("GET",ruta,user=4).statusCode())
        assertEquals(403,request("GET",ruta,user=1).statusCode())
        assertEquals(403,request("PUT","/api/docente/solucionarios/${s.id}/aprobar").statusCode())
        assertEquals(403,request("PUT","/api/docente/preguntas/${p.id}/aprobar",user=5,body="{}").statusCode())
    }

    @Test fun `dudas persisten y se agrupan por pregunta o tema con aislamiento academico`() {
        val p=pregunta()
        val body="""{"docenteId":1,"seccionId":1,"preguntaId":${p.id},"texto":"No entiendo el despeje"}"""
        val antes=dudas.count()
        assertEquals(201,request("POST","/api/alumno/dudas",body=body).statusCode())
        assertEquals(201,request("POST","/api/alumno/dudas",body=body.replace("despeje","primer paso")).statusCode())
        val g=grupos.findAll().single { it.preguntaId==p.id }
        assertEquals(2,dudas.findByGrupoIdOrderByFechaAscIdAsc(g.id).size)
        assertEquals(antes+2,dudas.count())
        assertEquals(403,request("GET","/api/docente/dudas/grupos/${g.id}",user=5).statusCode())
        assertEquals(200,request("GET","/api/docente/dudas/grupos/${g.id}",user=1).statusCode())
        assertEquals(403,request("POST","/api/alumno/dudas",user=4,body=body).statusCode())
        assertEquals(400,request("POST","/api/alumno/dudas",body=body.replace("No entiendo el despeje"," ")).statusCode())
        assertEquals(400,request("POST","/api/alumno/dudas",body="""{"docenteId":1,"seccionId":1,"texto":"Duda"}""").statusCode())
        val tema="""{"docenteId":1,"seccionId":1,"tema":"Tema HTTP Á","texto":"No entiendo"}"""
        assertEquals(201,request("POST","/api/alumno/dudas",body=tema).statusCode())
        assertEquals(201,request("POST","/api/alumno/dudas",body=tema.replace("Tema HTTP Á","tema http a")).statusCode())
        val tg=grupos.findAll().single { it.tema=="Tema HTTP Á" }
        assertEquals(2,dudas.findByGrupoIdOrderByFechaAscIdAsc(tg.id).size)
        assertEquals(200,request("GET","/api/alumno/dudas").statusCode())
        assertFalse(request("GET","/api/alumno/dudas",user=4).body().contains("No entiendo"))
    }

    @Test fun `respuesta agrupada se vincula a dudas existentes y conserva respuestas anteriores`() {
        val p=pregunta()
        val body="""{"docenteId":1,"seccionId":1,"preguntaId":${p.id},"texto":"Explicar despeje"}"""
        repeat(2) { assertEquals(201,request("POST","/api/alumno/dudas",body=body).statusCode()) }
        val grupo=grupos.findAll().single { it.preguntaId==p.id }
        val ruta="/api/docente/dudas/grupos/${grupo.id}/respuestas"
        assertEquals(403,request("POST",ruta,body="""{"texto":"Respuesta"}""").statusCode())
        assertEquals(403,request("POST",ruta,user=5,body="""{"texto":"Respuesta"}""").statusCode())
        assertEquals(400,request("POST",ruta,user=1,body="""{"texto":" "}""").statusCode())
        val primera=request("POST",ruta,user=1,body="""{"texto":"Restar seis y dividir entre dos"}""")
        assertEquals(201,primera.statusCode(),primera.body())
        val existentes=dudas.findByGrupoIdOrderByFechaAscIdAsc(grupo.id)
        val respuestaId=existentes.first().respuestaId
        assertNotNull(respuestaId)
        assertTrue(existentes.all { it.respuestaId==respuestaId })
        assertEquals(409,request("POST",ruta,user=1,body="""{"texto":"Duplicada"}""").statusCode())
        val consulta="/api/alumno/dudas/${existentes.first().id}/respuesta"
        assertEquals(200,request("GET",consulta).statusCode())
        assertEquals(403,request("GET",consulta,user=4).statusCode())
        assertEquals(201,request("POST","/api/alumno/dudas",body=body).statusCode())
        assertEquals(201,request("POST",ruta,user=1,body="""{"texto":"Segunda explicación"}""").statusCode())
        assertEquals(2,respuestasGrupo.findByGrupoIdOrderByFechaAscIdAsc(grupo.id).size)
        assertEquals(respuestaId,dudas.findById(existentes.first().id).orElseThrow().respuestaId)
        assertEquals(200,request("GET",ruta,user=1).statusCode())
    }

    @Test fun `resuelve dudas respondidas filtra estados y conserva historial sin duplicar eventos`() {
        val p=pregunta()
        val body="""{"docenteId":1,"seccionId":1,"preguntaId":${p.id},"texto":"Duda a resolver"}"""
        assertEquals(201,request("POST","/api/alumno/dudas",body=body).statusCode())
        val grupo=grupos.findAll().single { it.preguntaId==p.id }
        val d=dudas.findByGrupoIdOrderByFechaAscIdAsc(grupo.id).single()
        val ruta="/api/alumno/dudas/${d.id}/resolver"
        assertEquals(409,request("PUT",ruta).statusCode())
        assertEquals(403,request("PUT",ruta,user=4).statusCode())
        assertEquals(403,request("PUT","/api/docente/dudas/${d.id}/resolver",user=5).statusCode())
        assertEquals(201,request("POST","/api/docente/dudas/grupos/${grupo.id}/respuestas",user=1,
            body="""{"texto":"Explicación verificada por docente"}""").statusCode())
        assertEquals(200,request("PUT",ruta).statusCode())
        assertEquals(200,request("PUT",ruta).statusCode())
        val eventos=historialDudas.findByDudaIdOrderByFechaAscIdAsc(d.id)
        assertEquals(listOf("REGISTRADA","RESPONDIDA","RESUELTA"),eventos.map { it.evento })
        assertEquals(listOf(2L,1L,2L),eventos.map { it.actorId })
        assertTrue(request("GET","/api/alumno/dudas?estado=RESUELTA").body().contains("Duda a resolver"))
        assertFalse(request("GET","/api/alumno/dudas?estado=PENDIENTE").body().contains("Duda a resolver"))
        assertEquals(400,request("GET","/api/alumno/dudas?estado=INVALIDO").statusCode())
        assertEquals(200,request("GET","/api/alumno/dudas/${d.id}/historial").statusCode())
        assertEquals(403,request("GET","/api/alumno/dudas/${d.id}/historial",user=4).statusCode())
        assertEquals(200,request("GET","/api/docente/dudas/${d.id}/historial",user=1).statusCode())
    }

    @Test fun `calendario del hijo valida relacion matricula rol y rango`() {
        val ruta="/api/apoderado/alumnos/2/simulacros?desde=2026-11-01&hasta=2026-11-03"
        val ok=request("GET",ruta,user=3)
        assertEquals(200,ok.statusCode(),ok.body())
        assertTrue(ok.body().contains("Simulacro demo"))
        assertFalse(ok.body().contains("Otro curso"))
        assertFalse(ok.body().contains("Cancelado"))
        assertEquals(403,request("GET",ruta,user=2).statusCode())
        assertEquals(403,request("GET",ruta.replace("/alumnos/2/","/alumnos/4/"),user=3).statusCode())
        assertEquals(400,request("GET",ruta.replace("desde=2026-11-01","desde=2026-11-04"),user=3).statusCode())
        assertEquals("[]",request("GET", "/api/apoderado/alumnos/2/simulacros?desde=2027-01-01&hasta=2027-01-02",user=3).body())
    }

    @Test fun `descarga pdf de rendimiento real persistido y verifica apertura y renderizado`() {
        resultados.saveAllAndFlush(listOf(
            ResultadoSimulacro(simulacroId=7,alumnoId=4,seccionId=2,puntaje=16.0,temasIncorrectosJson="""["Álgebra"]"""),
            ResultadoSimulacro(simulacroId=8,alumnoId=4,seccionId=2,puntaje=20.0,temasIncorrectosJson="[]")))
        val ruta="/api/docente/secciones/2/rendimiento.pdf"
        val req=HttpRequest.newBuilder(URI("http://localhost:$port$ruta"))
            .header("Authorization","Bearer ${token(5)}").GET().build()
        val res=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(200,res.statusCode())
        assertEquals("application/pdf",res.headers().firstValue("Content-Type").orElseThrow())
        assertTrue(res.headers().firstValue("Content-Disposition").orElseThrow().contains("attachment"))
        java.nio.file.Files.write(java.nio.file.Path.of("target/hu39-rendimiento.pdf"),res.body())
        org.apache.pdfbox.Loader.loadPDF(res.body()).use { pdf ->
            val texto=org.apache.pdfbox.text.PDFTextStripper().getText(pdf)
            assertTrue(texto.contains("Promedio por resultado: 18.00"),texto)
            assertTrue(texto.contains("16.00"))
            assertTrue(texto.contains("Álgebra"))
            val imagen=org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImage(0)
            assertTrue(imagen.width>0)
            javax.imageio.ImageIO.write(imagen,"png",java.io.File("target/hu39-rendimiento.png"))
        }
        assertEquals(403,request("GET",ruta,user=1).statusCode())
        assertEquals(403,request("GET",ruta,user=2).statusCode())
    }

    @Test fun `resumen semanal usa intervalo real y programador simula envio una sola vez`() {
        val semana=estadisticas.semanaAnterior()
        val inicio=semana.atStartOfDay()
        val fin=inicio.plusWeeks(1)
        fun guardar(fecha: java.time.LocalDateTime,docente: Long=5)=preguntas.saveAndFlush(Pregunta(
            enunciado="Pregunta semanal",tema="Semana",dificultad=Dificultad.FACIL,
            opciones=listOf("4"),respuestaCorrecta="4",estado=EstadoPregunta.APROBADA,docenteId=docente,fechaCreacion=fecha))
        val primera=guardar(inicio)
        guardar(fin.minusNanos(1000))
        guardar(inicio.minusSeconds(1))
        guardar(fin)
        guardar(inicio,docente=1)
        respuestas.saveAndFlush(RespuestaPractica(preguntaId=primera.id,alumnoId=4,respuesta="4",correcta=true,fecha=inicio))
        respuestas.saveAndFlush(RespuestaPractica(preguntaId=primera.id,alumnoId=4,respuesta="2",correcta=false,fecha=fin))
        val actor=com.umbral.umbral_backend.security.Actor(5,"DOCENTE","otro-docente@example.invalid")
        val stats=estadisticas.calcular(actor,semana)
        assertEquals(2,stats.preguntasCreadas)
        assertEquals(2,stats.preguntasCreadasActualmenteAprobadas)
        assertEquals(1,stats.intentosPractica)
        assertEquals(1,stats.aciertosPractica)
        assertEquals(1,stats.alumnosPractica)
        val ok=request("GET","/api/docente/resumen-semanal?semana=$semana",user=5)
        assertEquals(200,ok.statusCode(),ok.body())
        assertTrue(ok.body().contains("\"preguntasCreadas\":2"))
        assertEquals(403,request("GET","/api/docente/resumen-semanal",user=2).statusCode())
        assertEquals(400,request("GET","/api/docente/resumen-semanal?semana=${semana.plusDays(1)}",user=5).statusCode())
        assertTrue(correo is CorreoResumenSimulado)
        programador.ejecutar()
        val resumen=resumenes.findByDocenteIdAndSemanaInicio(5,semana)!!
        assertEquals("SIMULADO",resumen.estado)
        assertEquals(1,resumen.intentos)
        assertTrue(resumen.cuerpo.contains("Preguntas creadas: 2"))
        programador.ejecutar()
        assertEquals(1,resumenes.findByDocenteIdAndSemanaInicio(5,semana)!!.intentos)
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM resumenes_semanales WHERE docente_id=5 AND semana_inicio=?",Long::class.java,semana))
    }

    @Test fun `baja de docente matricula y vinculo revoca acceso inmediatamente`() {
        val p=pregunta()
        val respuesta="""{"preguntaId":${p.id},"respuesta":"4"}"""
        try {
            jdbc.update("UPDATE auth_user SET is_active=false WHERE id=1")
            assertEquals(403,request("POST","/api/practicas/respuestas",body=respuesta).statusCode())
            assertEquals(401,request("GET","/api/docente/preguntas?docenteId=1",user=1).statusCode())
        } finally { jdbc.update("UPDATE auth_user SET is_active=true WHERE id=1") }
        try {
            jdbc.update("UPDATE adm_matricula SET activa=false WHERE id=1")
            assertEquals(403,request("POST","/api/practicas/respuestas",body=respuesta).statusCode())
            assertEquals("[]",request("GET","/api/apoderado/alumnos/2/simulacros?desde=2026-11-01&hasta=2026-11-03",user=3).body())
        } finally { jdbc.update("UPDATE adm_matricula SET activa=true WHERE id=1") }
        try {
            jdbc.update("DELETE FROM adm_apoderado_hijos WHERE id=1")
            assertEquals(403,request("GET","/api/apoderado/alumnos/2/simulacros",user=3).statusCode())
        } finally { jdbc.update("INSERT INTO adm_apoderado_hijos VALUES(1,1,2)") }
    }
}
