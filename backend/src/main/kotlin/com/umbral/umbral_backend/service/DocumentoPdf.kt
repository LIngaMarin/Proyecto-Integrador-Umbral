package com.umbral.umbral_backend.service

import org.apache.pdfbox.pdmodel.*
import org.apache.pdfbox.pdmodel.font.*
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

/** Texto sin marcado ejecutable, con ajuste de líneas y paginación. */
@Component
class DocumentoPdf {
    /** Construye el PDF en memoria, ajusta las líneas y distribuye el contenido en páginas. */
    fun generar(lineas: List<String>): ByteArray = PDDocument().use { doc ->
        val font=PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val texto=lineas.flatMap { original ->
            val seguro=buildString {
                original.codePoints().forEach { cp ->
                    val s=String(Character.toChars(cp))
                    // Sustituye caracteres que la fuente no puede representar para permitir la descarga.
                    append(try { font.encode(s); s } catch (_: IllegalArgumentException) { "?" })
                }
            }
            // Mide el ancho real: una línea de W ocupa más espacio que una de i.
            buildList {
                var linea=""
                seguro.forEach { c ->
                    if(linea.isNotEmpty() && font.getStringWidth(linea+c)/1000f*11f>532f) {
                        add(linea)
                        linea=""
                    }
                    linea+=c
                }
                add(linea)
            }
        }
        texto.ifEmpty { listOf("Sin datos") }.chunked(44).forEach { pagina ->
            val page=PDPage()
            doc.addPage(page)
            PDPageContentStream(doc,page).use { stream ->
                stream.beginText()
                stream.setFont(font,11f)
                stream.setLeading(16f)
                stream.newLineAtOffset(40f,750f)
                pagina.forEach { stream.showText(it);stream.newLine() }
                stream.endText()
            }
        }
        ByteArrayOutputStream().use { output -> doc.save(output);output.toByteArray() }
    }
}
