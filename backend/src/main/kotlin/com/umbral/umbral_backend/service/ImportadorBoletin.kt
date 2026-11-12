package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

/** Importa texto real de boletines PDF estructurados; no genera preguntas de reemplazo ni aplica OCR ficticio. */
object ImportadorBoletin {
    fun leer(file:MultipartFile,docenteId:Long):List<Pregunta>{
        if(file.isEmpty || file.size>1048576)throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona un PDF de hasta 1 MB")
        val bytes=file.bytes
        if(!bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray()))throw ResponseStatusException(HttpStatus.BAD_REQUEST,"El archivo debe ser un PDF válido")
        val texto=try { Loader.loadPDF(bytes).use { pdf ->
            if(pdf.numberOfPages>30 || pdf.isEncrypted)throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"El boletín debe tener hasta 30 páginas y no estar protegido")
            PDFTextStripper().getText(pdf)
        } } catch(ex:ResponseStatusException){throw ex}
        catch(_:Exception){throw ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer el archivo PDF")}
        return interpretar(texto,docenteId)
    }

    /** Cada bloque declara tema, dificultad, pregunta, opciones y letra correcta antes de persistirse. */
    fun interpretar(texto:String,docenteId:Long):List<Pregunta>{
        val bloques=texto.replace("\r","").split(Regex("(?m)(?=^Tema:)" )).filter{it.isNotBlank()}
        if(bloques.isEmpty() || bloques.size>100)throw formato()
        return bloques.map{bloque ->
            val lines=bloque.lines().map{it.trim()}.filter{it.isNotEmpty()}
            fun valor(name:String)=lines.firstOrNull{it.startsWith("$name:",ignoreCase=true)}?.substringAfter(':')?.trim() ?: throw formato()
            val tema=valor("Tema")
            val dificultad=try{Dificultad.valueOf(valor("Dificultad").uppercase())}catch(_:IllegalArgumentException){throw formato()}
            val inicio=lines.indexOfFirst{it.startsWith("Pregunta:",true)}
            val opcion=Regex("^([A-H])[).]\\s*(.+)$")
            val primera=lines.indexOfFirst{opcion.matches(it)}
            if(inicio<0 || primera<=inicio)throw formato()
            val enunciado=(listOf(lines[inicio].substringAfter(':'))+lines.subList(inicio+1,primera)).joinToString(" ").trim()
            val opciones=lines.mapNotNull{opcion.matchEntire(it)}
            val correcta=opciones.singleOrNull{it.groupValues[1]==valor("Correcta").uppercase()}?.groupValues?.get(2) ?: throw formato()
            val textos=opciones.map{it.groupValues[2]}
            if(tema.length !in 1..255 || enunciado.length !in 1..4000 || textos.size !in 2..8 || textos.distinct().size!=textos.size || textos.any{it.length>255})throw formato()
            Pregunta(enunciado=enunciado,tema=tema,dificultad=dificultad,opciones=textos,respuestaCorrecta=correcta,docenteId=docenteId)
        }
    }
    private fun formato()=ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"El boletín debe indicar Tema, Dificultad, Pregunta, opciones A) a H) y Correcta con la letra correspondiente")
}
