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

    companion object {
        val secret = ByteArray(48).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        @JvmStatic @DynamicPropertySource fun config(r: DynamicPropertyRegistry) { r.add("umbral.jwt.secret") { secret } }
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
}
