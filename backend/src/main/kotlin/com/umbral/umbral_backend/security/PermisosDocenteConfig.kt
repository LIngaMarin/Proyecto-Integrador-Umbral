package com.umbral.umbral_backend.security

import com.umbral.umbral_backend.controller.DocenteController
import com.umbral.umbral_backend.repository.*
import jakarta.servlet.http.*
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.*
import org.springframework.web.servlet.config.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** Cierra las rutas docentes preexistentes que exponían respuestas a cualquier visitante. */
@Configuration
class PermisosDocenteConfig(private val identidad: IdentidadAcademica, private val preguntas: PreguntaRepository,
    private val soluciones: SolucionarioRepository) : WebMvcConfigurer {
    /** Agrega la comprobación de identidad y propiedad a las rutas docentes existentes. */
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : HandlerInterceptor {
            override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
                if (handler !is HandlerMethod || handler.beanType != DocenteController::class.java) return true
                val jwt = SecurityContextHolder.getContext().authentication?.principal as? Jwt
                    ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
                val actor = identidad.actor(jwt,"DOCENTE")
                // El identificador recibido no puede representar a otro docente.
                request.getParameter("docenteId")?.let {
                    if (it.toLongOrNull() != actor.id) throw ResponseStatusException(HttpStatus.FORBIDDEN)
                }
                @Suppress("UNCHECKED_CAST")
                val vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<String,String> ?: emptyMap()
                vars["seccionId"]?.toLongOrNull()?.let { identidad.exigirSeccion(actor.id,it) }
                vars["id"]?.toLongOrNull()?.let { id ->
                    // Valida la propiedad de la pregunta, también al acceder mediante su solucionario.
                    val docenteId = if (request.requestURI.contains("/solucionarios/"))
                        soluciones.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }.pregunta.docenteId
                    else preguntas.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }.docenteId
                    if (docenteId != actor.id) throw ResponseStatusException(HttpStatus.FORBIDDEN)
                }
                return true
            }
        }).addPathPatterns("/api/docente/**")
    }
}
