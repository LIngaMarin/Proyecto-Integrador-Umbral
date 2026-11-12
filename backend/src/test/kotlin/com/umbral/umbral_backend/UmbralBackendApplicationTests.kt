package com.umbral.umbral_backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
/** Prueba de integración que carga el contexto de Spring Boot. */
class UmbralBackendApplicationTests {

	/** El escenario pasa si el contexto puede cargarse sin excepciones. */
	@Test
	fun contextLoads() {
	}

}
