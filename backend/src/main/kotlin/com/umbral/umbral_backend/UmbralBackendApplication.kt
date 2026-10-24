package com.umbral.umbral_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Aplicación principal que Spring Boot usa para iniciar el backend y su contexto. */
@SpringBootApplication
class UmbralBackendApplication

/** Delega el arranque de la aplicación a Spring Boot con los argumentos recibidos. */
fun main(args: Array<String>) {
	runApplication<UmbralBackendApplication>(*args)
}
