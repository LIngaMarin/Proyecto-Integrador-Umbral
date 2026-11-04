package com.umbral.umbral_backend.service

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.*
import com.umbral.umbral_backend.model.ResultadoSimulacro
import com.umbral.umbral_backend.repository.ResultadoSimulacroRepository
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.*
import java.time.Instant
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import tools.jackson.databind.ObjectMapper

/** HU41: servidor HTTP real y base aislada; valida consulta, exportación, filtros y acceso. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties=["spring.datasource.url=jdbc:h2:mem:reportes-hu41;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"])
@ActiveProfiles("test")
class ReporteRendimientoHttpTests {
    @LocalServerPort var port:Int=0
    @Autowired lateinit var key:SecretKeySpec
    @Autowired lateinit var resultados:ResultadoSimulacroRepository
    @Autowired lateinit var json:ObjectMapper

    /** Se limpia solamente la tabla de resultados de esta base exclusiva de pruebas. */
    @BeforeEach fun preparar(){
        resultados.deleteAll()
        resultados.saveAllAndFlush(listOf(
            ResultadoSimulacro(simulacroId=1,alumnoId=2,seccionId=1,puntaje=16.0,temasIncorrectosJson="""["Álgebra","Geometría"]"""),
            ResultadoSimulacro(simulacroId=99,alumnoId=2,seccionId=1,puntaje=20.0,temasIncorrectosJson="""["Álgebra"]""")))
    }
    private fun token(id:Long):String=SignedJWT(JWSHeader(JWSAlgorithm.HS256),JWTClaimsSet.Builder()
        .claim("user_id",id.toString()).claim("token_type","access").expirationTime(Date.from(Instant.now().plusSeconds(300))).build())
        .also{it.sign(MACSigner(key.encoded))}.serialize()
    private fun get(path:String,user:Long?=1):HttpResponse<ByteArray>{
        val b=HttpRequest.newBuilder(URI("http://127.0.0.1:$port/api/docente/secciones/$path"))
            .header("Accept",if(path.contains(".pdf"))"application/pdf" else "application/json")
        user?.let{b.header("Authorization","Bearer ${token(it)}")}
        return HttpClient.newHttpClient().send(b.GET().build(),HttpResponse.BodyHandlers.ofByteArray())
    }
    @Test fun `consulta y PDF contienen resultados persistidos e indicadores consistentes`() {
        val response=get("1/rendimiento")
        assertEquals(200,response.statusCode())
        val data=json.readTree(response.body())
        assertEquals("Demo A",data.get("seccionNombre").asString())
        assertEquals("docente@example.invalid",data.get("docenteEmail").asString())
        assertEquals(18.0,data.get("resumen").get("promedio").asDouble())
        assertEquals(2,data.get("resumen").get("resultados").asInt())
        assertEquals(1,data.get("resumen").get("alumnos").asInt())
        assertTrue(data.get("resultados").toString().contains("alumno@example.invalid"))
        assertTrue(data.get("simulacros").toString().contains("Simulacro demo"))
        assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"))
        val archivo=get("1/rendimiento.pdf")
        assertEquals(200,archivo.statusCode())
        assertEquals("application/pdf",archivo.headers().firstValue("Content-Type").orElseThrow())
        assertEquals("attachment; filename=\"rendimiento-seccion-1.pdf\"",archivo.headers().firstValue("Content-Disposition").orElseThrow())
        assertEquals(archivo.body().size.toLong(),archivo.headers().firstValueAsLong("Content-Length").orElseThrow())
        assertEquals("nosniff",archivo.headers().firstValue("X-Content-Type-Options").orElseThrow())
        assertTrue(String(archivo.body().take(5).toByteArray()).startsWith("%PDF-"))
        Loader.loadPDF(archivo.body()).use{pdf ->
            val text=PDFTextStripper().getText(pdf)
            for(value in listOf("Demo A","docente@example.invalid","alumno@example.invalid","Promedio por resultado: 18.00","Álgebra","16.00","20.00"))assertTrue(text.contains(value),text)
            java.nio.file.Files.write(java.nio.file.Path.of("target/hu41-reporte-verificado.pdf"),archivo.body())
            javax.imageio.ImageIO.write(org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImage(0),"png",java.io.File("target/hu41-reporte-verificado.png"))
        }
    }
    @Test fun `filtro por simulacro aplica a detalle indicadores temas y exportacion`() {
        val response=get("1/rendimiento?simulacroId=1")
        val data=json.readTree(response.body())
        assertEquals(200,response.statusCode())
        assertEquals(16.0,data.get("resumen").get("promedio").asDouble())
        assertEquals(1,data.get("resultados").size())
        assertEquals(2,data.get("simulacros").size())
        Loader.loadPDF(get("1/rendimiento.pdf?simulacroId=1").body()).use{pdf ->
            val text=PDFTextStripper().getText(pdf)
            assertTrue(text.contains("Promedio por resultado: 16.00"))
            assertFalse(text.contains("20.00"))
        }
        assertEquals(400,get("1/rendimiento?simulacroId=0").statusCode())
        assertEquals(400,get("1/rendimiento.pdf?simulacroId=abc").statusCode())
    }
    @Test fun `roles y secciones ajenas o inexistentes no exponen notas ni archivos`() {
        for(suffix in listOf("rendimiento","rendimiento.pdf")){
            assertEquals(401,get("1/$suffix",null).statusCode())
            for(user in listOf(2L,3L,5L)){
                val denied=get("1/$suffix",user)
                assertEquals(403,denied.statusCode())
                assertFalse(denied.headers().firstValue("Content-Disposition").isPresent)
            }
            // Se conserva 403 para no revelar a un docente si una sección no asignada existe.
            assertEquals(403,get("999999/$suffix").statusCode())
        }
    }
    @Test fun `seccion y filtro vacios generan reporte valido sin inventar promedio`() {
        for(path in listOf("2/rendimiento","1/rendimiento?simulacroId=999999")){
            val user=if(path.startsWith("2/"))5L else 1L
            val response=get(path,user)
            assertEquals(200,response.statusCode())
            val data=json.readTree(response.body())
            assertEquals(0,data.get("resultados").size())
            assertTrue(data.get("resumen").get("promedio").isNull)
            val file=get(path.replace("rendimiento","rendimiento.pdf"),user)
            assertEquals(200,file.statusCode())
            Loader.loadPDF(file.body()).use{pdf ->
                assertTrue(PDFTextStripper().getText(pdf).contains("Promedio por resultado: Sin datos"))
            }
        }
    }
    @Test fun `datos academicos corruptos devuelven error sin entregar un PDF falso`() {
        resultados.saveAndFlush(ResultadoSimulacro(simulacroId=1,alumnoId=2,seccionId=1,puntaje=10.0,temasIncorrectosJson="no es JSON"))
        for(path in listOf("1/rendimiento","1/rendimiento.pdf")){
            val response=get(path)
            assertEquals(422,response.statusCode())
            assertFalse(response.headers().firstValue("Content-Disposition").isPresent)
            assertFalse(String(response.body()).contains("%PDF-"))
        }
    }
}
