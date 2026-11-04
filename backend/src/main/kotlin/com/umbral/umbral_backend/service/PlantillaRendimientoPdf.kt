package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.ReporteRendimientoDTO
import org.apache.pdfbox.pdmodel.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.*
import org.springframework.stereotype.Component
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

/** HU41: plantilla académica A4 con identidad UMBRAL, indicadores, tablas y paginación real. */
@Component
class PlantillaRendimientoPdf {
    /** Genera únicamente texto y trazos; el contenido académico nunca se interpreta como HTML o código PDF. */
    fun generar(reporte:ReporteRendimientoDTO):ByteArray = PDDocument().use { doc ->
        doc.documentInformation.title="UMBRAL - Reporte de rendimiento - ${reporte.seccionNombre}"
        doc.documentInformation.author="UMBRAL"
        val dibujo=Dibujo(doc)
        try {
            dibujo.pagina()
            dibujo.texto("Reporte de rendimiento académico",19f,true)
            dibujo.texto("Sección: ${reporte.seccionNombre}",12f,true)
            dibujo.texto("Docente responsable: ${reporte.docenteEmail}")
            dibujo.texto("Generado: ${reporte.generadoEn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))} (Lima)")
            val seleccionado=reporte.simulacros.firstOrNull{it.id==reporte.simulacroId}
            dibujo.texto("Selección: ${if(reporte.simulacroId==null)"Todos los simulacros" else seleccionado?.nombre ?: "Simulacro #${reporte.simulacroId}"}")
            dibujo.titulo("Resumen académico")
            val r=reporte.resumen
            dibujo.texto("Resultados registrados: ${r.resultados}   |   Alumnos con resultados: ${r.alumnos}")
            dibujo.texto("Promedio por resultado: ${numero(r.promedio)}",12f,true)
            dibujo.texto("Puntaje mínimo: ${numero(r.minimo)}   |   Puntaje máximo: ${numero(r.maximo)}")
            dibujo.titulo("Resultados de simulacros")
            if(reporte.resultados.isEmpty()) dibujo.texto("La sección no tiene resultados registrados para la selección indicada.")
            else dibujo.tabla(listOf("Alumno","Simulacro","Puntaje","Temas por reforzar"),listOf(145f,165f,65f,140f),reporte.resultados.map { fila ->
                val fecha=reporte.simulacros.firstOrNull{it.id==fila.simulacroId}?.fecha?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                listOf(fila.alumno ?: "Alumno #${fila.alumnoId}",
                    (fila.simulacro ?: "Simulacro #${fila.simulacroId}")+(fecha?.let{"\n$it"} ?: ""),
                    numero(fila.puntaje),fila.temasPorReforzar.joinToString(", ").ifEmpty{"Sin errores registrados"})
            })
            dibujo.titulo("Temas que requieren refuerzo")
            if(reporte.temas.isEmpty()) dibujo.texto(if(r.resultados==0)"Sin información de temas para esta selección." else "No hay temas incorrectos registrados en estos resultados.")
            else dibujo.tabla(listOf("Tema","Errores registrados"),listOf(385f,130f),reporte.temas.map{listOf(it.tema,it.cantidadErrores.toString())})
            dibujo.texto("Los indicadores corresponden a los resultados registrados de la selección.",9f)
        } finally { dibujo.cerrar() }
        // Se conoce el total al finalizar las tablas; cada página recibe su número definitivo.
        for(i in 0 until doc.numberOfPages) PDPageContentStream(doc,doc.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true).use { out ->
            out.setNonStrokingColor(Color(72,94,80))
            out.beginText();out.setFont(dibujo.normal,8f);out.newLineAtOffset(40f,28f)
            out.showText("UMBRAL - Coordinación académica");out.endText()
            out.beginText();out.setFont(dibujo.normal,8f);out.newLineAtOffset(454f,28f)
            out.showText("Página ${i+1} de ${doc.numberOfPages}");out.endText()
        }
        ByteArrayOutputStream().use{out->doc.save(out);out.toByteArray()}
    }

    private fun numero(n:Double?)=n?.let{String.format(Locale.ROOT,"%.2f",it)} ?: "Sin datos"

    /** Lleva el cursor vertical y repite encabezados cuando una tabla continúa en otra página. */
    private class Dibujo(private val doc:PDDocument) {
        val normal=PDType1Font(Standard14Fonts.FontName.HELVETICA)
        private val negrita=PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        private val verde=Color(21,63,53)
        private var out:PDPageContentStream?=null
        private var y=0f
        private val ancho=515f

        fun cerrar(){out?.close();out=null}
        fun pagina(){
            cerrar()
            val page=PDPage(PDRectangle.A4);doc.addPage(page)
            out=PDPageContentStream(doc,page)
            out!!.setNonStrokingColor(verde);out!!.addRect(0f,760f,page.mediaBox.width,82f);out!!.fill()
            escribir("UMBRAL",40f,797f,23f,true,Color.WHITE)
            escribir("PREPARACIÓN PREUNIVERSITARIA",40f,778f,8f,false,Color(222,233,214))
            y=730f
        }
        private fun asegurar(alto:Float):Boolean {if(y-alto<55f){pagina();return true};return false}

        /** Conserva español y reemplaza caracteres que Helvetica no puede codificar, sin abortar el documento. */
        private fun seguro(texto:String)=buildString {
            texto.codePoints().forEach{cp ->
                val char=String(Character.toChars(cp))
                append(when{cp==10->"\n";Character.isISOControl(cp)->" ";else->try{normal.encode(char);char}catch(_:IllegalArgumentException){"?"}})
            }
        }
        private fun lineas(texto:String,maximo:Float,tamano:Float,bold:Boolean=false):List<String> = buildList {
            val font=if(bold)negrita else normal
            seguro(texto).split('\n').forEach{parte ->
                var actual=""
                parte.forEach{c->
                    if(actual.isNotEmpty() && font.getStringWidth(actual+c)/1000f*tamano>maximo){add(actual);actual=""}
                    actual+=c
                }
                add(actual)
            }
        }
        private fun escribir(texto:String,x:Float,y:Float,size:Float,bold:Boolean=false,color:Color=verde){
            out!!.setNonStrokingColor(color);out!!.beginText();out!!.setFont(if(bold)negrita else normal,size)
            out!!.newLineAtOffset(x,y);out!!.showText(texto);out!!.endText()
        }
        fun texto(texto:String,size:Float=10f,bold:Boolean=false){
            lineas(texto,ancho,size,bold).forEach{linea->asegurar(size+9f);escribir(linea,40f,y,size,bold);y-=size+6f}
            y-=4f
        }
        fun titulo(texto:String){asegurar(60f);y-=14f;this.texto(texto,13f,true)}

        fun tabla(cabeceras:List<String>,anchos:List<Float>,filas:List<List<String>>){
            fun encabezado(){
                asegurar(30f)
                out!!.setNonStrokingColor(verde);out!!.addRect(40f,y-25f,ancho,25f);out!!.fill()
                var x=40f
                cabeceras.forEachIndexed{i,c->escribir(c,x+7f,y-16f,8f,true,Color.WHITE);x+=anchos[i]}
                y-=25f
            }
            asegurar(65f);encabezado()
            filas.forEachIndexed{index,fila->
                val columnas=fila.mapIndexed{i,c->lineas(c,anchos[i]-14f,9f)}
                val cantidad=columnas.maxOf{it.size}
                var desde=0
                // Una celda extensa también puede continuar: no se corta ni se pierde texto entre páginas.
                while(desde<cantidad){
                    if(asegurar(40f))encabezado()
                    val disponibles=((y-55f-14f)/12f).toInt().coerceAtLeast(1)
                    val lote=minOf(cantidad-desde,disponibles,30)
                    val alto=lote*12f+14f
                    out!!.setNonStrokingColor(if(index%2==0)Color(242,246,239)else Color.WHITE)
                    out!!.addRect(40f,y-alto,ancho,alto);out!!.fill()
                    var x=40f
                    columnas.forEachIndexed{i,lines->
                        for(j in 0 until lote)lines.getOrNull(desde+j)?.let{escribir(it,x+7f,y-14f-j*12f,9f)}
                        x+=anchos[i]
                    }
                    out!!.setStrokingColor(Color(217,226,214));out!!.setLineWidth(.5f)
                    out!!.moveTo(40f,y-alto);out!!.lineTo(555f,y-alto);out!!.stroke()
                    y-=alto;desde+=lote
                }
            }
            y-=12f
        }
    }
}
