package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.ResumenSemanal
import com.umbral.umbral_backend.repository.ResumenSemanalRepository
import com.umbral.umbral_backend.security.*
import jakarta.mail.internet.InternetAddress
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*

@Service
class ResumenSemanalService(private val repository: ResumenSemanalRepository,
    private val estadisticas: EstadisticasSemanaService,private val identidad: IdentidadAcademica,
    private val correo: CorreoResumen,private val clock: Clock) {
    /** Valida el correo y guarda un único resumen por docente y semana. */
    @Transactional
    fun preparar(docente: Actor,semana: LocalDate): ResumenSemanal {
        repository.findByDocenteIdAndSemanaInicio(docente.id,semana)?.let { return it }
        InternetAddress(docente.email,true).validate()
        require(docente.email.isNotBlank() && docente.email.length<=254)
        val datos=estadisticas.calcular(docente,semana)
        return repository.saveAndFlush(ResumenSemanal(docenteId=docente.id,semanaInicio=semana,
            destinatario=docente.email,asunto="UMBRAL: resumen semanal $semana",cuerpo=datos.texto(),creado=LocalDateTime.now(clock)))
    }

    /** Procesa el envío, conserva los fallos para reintentar y distingue el correo real del simulado. */
    @Transactional
    fun enviar(id: Long) {
        val r=repository.bloquear(id) ?: return
        // Evita reenviar un resumen que ya fue enviado o procesado en modo simulado.
        if(r.estado=="ENVIADO" || r.estado=="SIMULADO") return
        r.intentos++
        try {
            val actual=identidad.docentes().singleOrNull { it.id==r.docenteId }
            check(actual!=null && actual.email==r.destinatario) { "Destinatario inactivo o modificado" }
            correo.enviar(r.destinatario,r.asunto,r.cuerpo)
            r.estado=if(correo.simulado) "SIMULADO" else "ENVIADO"
            r.enviado=LocalDateTime.now(clock)
            r.ultimoError=null
        } catch (ex: Exception) {
            r.estado="ERROR"
            // No persiste mensajes SMTP que puedan contener información sensible.
            r.ultimoError=ex.javaClass.simpleName.take(255)
        }
        repository.save(r)
    }
}
