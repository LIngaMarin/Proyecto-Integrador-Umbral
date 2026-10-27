package com.umbral.umbral_backend.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DocumentoPdfUnitTests {
    @Test fun `pagina textos extensos y genera documento que puede abrirse`() {
        val bytes=DocumentoPdf().generar(List(100) { "Álgebra y geometría " + "x".repeat(150) + " 😀" })
        Loader.loadPDF(bytes).use { pdf ->
            assertTrue(pdf.numberOfPages>=5)
            assertTrue(PDFTextStripper().getText(pdf).contains("Álgebra"))
        }
    }
    @Test fun `documento sin datos sigue siendo un pdf valido`() {
        Loader.loadPDF(DocumentoPdf().generar(emptyList())).use { pdf ->
            assertEquals(1,pdf.numberOfPages)
            assertTrue(PDFTextStripper().getText(pdf).contains("Sin datos"))
        }
    }
}
