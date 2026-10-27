package com.umbral.umbral_backend.service

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Component

interface CorreoResumen {
    val simulado: Boolean
    fun enviar(destinatario: String,asunto: String,cuerpo: String)
}

@Component @Profile("demo","test")
class CorreoResumenSimulado : CorreoResumen {
    override val simulado=true
    override fun enviar(destinatario: String,asunto: String,cuerpo: String) {
        LoggerFactory.getLogger(javaClass).info("Correo simulado preparado: {}. No se conecta a SMTP.",asunto)
    }
}

@Component @Profile("!demo & !test")
class CorreoResumenSmtp(private val env: Environment) : CorreoResumen {
    override val simulado=false
    override fun enviar(destinatario: String,asunto: String,cuerpo: String) {
        val host=env.getProperty("UMBRAL_SMTP_HOST")?.takeIf { it.isNotBlank() }
            ?: error("Falta configurar servidor SMTP")
        val from=env.getProperty("UMBRAL_SMTP_FROM")?.takeIf { it.isNotBlank() }
            ?: error("Falta configurar remitente")
        val sender=JavaMailSenderImpl().apply {
            this.host=host
            port=env.getProperty("UMBRAL_SMTP_PORT",Int::class.java,587)
            username=env.getProperty("UMBRAL_SMTP_USERNAME")
            password=env.getProperty("UMBRAL_SMTP_PASSWORD")
            defaultEncoding="UTF-8"
            javaMailProperties.setProperty("mail.smtp.auth",(!username.isNullOrBlank()).toString())
            javaMailProperties.setProperty("mail.smtp.starttls.enable",env.getProperty("UMBRAL_SMTP_STARTTLS","true"))
            javaMailProperties.setProperty("mail.smtp.starttls.required",env.getProperty("UMBRAL_SMTP_STARTTLS","true"))
            for (opcion in listOf("connectiontimeout","timeout","writetimeout"))
                javaMailProperties.setProperty("mail.smtp.$opcion","10000")
        }
        sender.send(SimpleMailMessage().apply {
            setFrom(from);setTo(destinatario);setSubject(asunto);setText(cuerpo)
        })
    }
}
