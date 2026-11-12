package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.*

/** HU41: comprueba texto, estructura A4 y renderizado de la plantilla sin depender de una base externa. */
class PlantillaRendimientoPdfTests {
    private fun reporte(rows:List<FilaRendimientoDTO>)=ReporteRendimientoDTO(1,"Sección de preparación A","docente@example.invalid",
        OffsetDateTime.of(2026,11,4,10,30,0,0,ZoneOffset.ofHours(-5)),null,
        IndicadoresReporteDTO(rows.size,rows.map{it.alumnoId}.distinct().size,if(rows.isEmpty())null else 18.0,if(rows.isEmpty())null else 18.0,if(rows.isEmpty())null else 18.0),
        listOf(SimulacroReporteDTO(1,"Simulacro de noviembre",LocalDate.of(2026,11,1))),rows,listOf(TemaDebilDTO("Álgebra",rows.size)))

    @Test fun `plantilla incluye identidad fecha indicadores y datos reales de filas`() {
        val data=reporte(listOf(FilaRendimientoDTO(1,2,"alumno@example.invalid",1,"Simulacro de noviembre",18.0,listOf("Álgebra"))))
        Loader.loadPDF(PlantillaRendimientoPdf().generar(data)).use{pdf ->
            assertEquals(1,pdf.numberOfPages)
            val text=PDFTextStripper().getText(pdf)
            for(value in listOf("UMBRAL","Sección de preparación A","docente@example.invalid","04/11/2026 10:30","alumno@example.invalid","Promedio por resultado: 18.00","Álgebra","Página 1 de 1"))assertTrue(text.contains(value),text)
            assertTrue(PDFRenderer(pdf).renderImage(0).width>0)
            assertEquals("UMBRAL",pdf.documentInformation.author)
        }
    }
    @Test fun `sin notas muestra ausencia de datos en vez de promedio inventado`() {
        Loader.loadPDF(PlantillaRendimientoPdf().generar(reporte(emptyList()))).use{pdf ->
            val text=PDFTextStripper().getText(pdf)
            assertTrue(text.contains("Promedio por resultado: Sin datos"))
            assertTrue(text.contains("no tiene resultados registrados"))
        }
    }
    @Test fun `tablas extensas conservan filas y repiten encabezado y numeracion`() {
        val rows=(1..85).map{FilaRendimientoDTO(it.toLong(),2,"alumno-$it@example.invalid",1,"Simulacro de noviembre",18.0,listOf("W".repeat(350)+" 😀"))}
        Loader.loadPDF(PlantillaRendimientoPdf().generar(reporte(rows))).use{pdf ->
            assertTrue(pdf.numberOfPages>3)
            val text=PDFTextStripper().getText(pdf)
            assertTrue(text.contains("alumno-85@example.invalid"))
            assertTrue(text.contains("Página ${pdf.numberOfPages} de ${pdf.numberOfPages}"))
            assertTrue(text.split("UMBRAL").size>pdf.numberOfPages)
            assertTrue(PDFRenderer(pdf).renderImage(pdf.numberOfPages-1).height>0)
        }
    }
}
