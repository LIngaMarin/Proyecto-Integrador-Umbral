package com.umbral.umbral_backend.service

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.*
import com.umbral.umbral_backend.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.*
import java.time.Instant
import java.util.Date
import javax.crypto.spec.SecretKeySpec

/** HU41: el fallo se fuerza solo en pruebas; el portal no puede recibir éxito o un archivo vacío por error. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties=["spring.datasource.url=jdbc:h2:mem:fallo-reporte-hu41;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"])
@ActiveProfiles("test")
class FalloReportePdfHttpTests {
    @LocalServerPort var port:Int=0
    @Autowired lateinit var key:SecretKeySpec
    @MockitoBean lateinit var plantilla:PlantillaRendimientoPdf
    @Test fun `fallo de generacion devuelve 503 sin contenido PDF ni detalles internos`() {
        // Mockito devuelve null al registrar el matcher; el valor vacío evita el chequeo no nulo de Kotlin.
        val matcher=Mockito.any(ReporteRendimientoDTO::class.java) ?: ReporteRendimientoDTO(1,"","",java.time.OffsetDateTime.now(),
            null,IndicadoresReporteDTO(0,0,null,null,null),emptyList(),emptyList(),emptyList())
        Mockito.`when`(plantilla.generar(matcher))
            .thenThrow(IllegalStateException("detalle interno reservado"))
        val jwt=SignedJWT(JWSHeader(JWSAlgorithm.HS256),JWTClaimsSet.Builder().claim("user_id","1").claim("token_type","access")
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).build()).also{it.sign(MACSigner(key.encoded))}.serialize()
        val request=HttpRequest.newBuilder(URI("http://127.0.0.1:$port/api/docente/secciones/1/rendimiento.pdf"))
            .header("Accept","application/pdf")
            .header("Authorization","Bearer $jwt").GET().build()
        val response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString())
        assertEquals(503,response.statusCode())
        assertFalse(response.headers().firstValue("Content-Disposition").isPresent)
        assertFalse(response.body().contains("detalle interno reservado"))
        assertFalse(response.body().contains("%PDF-"))
    }
}
