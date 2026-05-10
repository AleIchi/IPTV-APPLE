package com.iptv.fiber.datos.utilidades

import com.iptv.fiber.datos.modelo.Canal
import java.io.BufferedReader
import java.io.StringReader

/** Utilidad para procesar archivos de lista de reproducción M3U. */
class AnalizadorM3U {
    /** Procesa el contenido de un M3U y devuelve los canales agrupados por categoría. */
    fun procesar(contenidoM3U: String): Map<String, List<Canal>> {
        val canales = mutableListOf<Canal>()
        val lector = BufferedReader(StringReader(contenidoM3U))
        var linea = lector.readLine()
        
        var nombreActual: String? = null
        var logoActual: String? = null
        var grupoActual: String? = null
        
        while (linea != null) {
            linea = linea.trim()
            if (linea.startsWith("#EXTINF:")) {
                // Procesar metadatos
                // Ejemplo: #EXTINF:-1 tvg-id="" tvg-nombre="" tvg-logo="http://..." group-titulo="Noticias",BBC News
                
                // Extraer Nombre (todo después de la última coma)
                val indiceComa = linea.lastIndexOf(',')
                if (indiceComa != -1) {
                    nombreActual = linea.substring(indiceComa + 1).trim()
                } else {
                    nombreActual = "Canal Desconocido"
                }

                // Extraer Logo
                logoActual = extraerAtributo(linea, "tvg-logo")
                
                // Extraer Grupo
                grupoActual = extraerAtributo(linea, "group-titulo") ?: "Sin categoría"
                
            } else if (!linea.startsWith("#") && linea.isNotEmpty()) {
                // Esta es la URL de la transmisión
                if (nombreActual != null) {
                    val url = linea
                    val id = url.hashCode() // Generar ID basado en el hash de la URL
                    
                    canales.add(Canal(
                        id_transmision = id,
                        nombre = nombreActual,
                        icono_transmision = logoActual,
                        id_categoria = grupoActual ?: "Sin categoría",
                        fuenteDirecta = url,
                        tipo_transmision = "live"
                    ))
                }
                // Reiniciar para la siguiente entrada
                nombreActual = null
                logoActual = null
                grupoActual = null
            }
            
            linea = lector.readLine()
        }
        
        // Agrupar canales por categoría inmediatamente para facilitar su uso
        return canales.groupBy { it.id_categoria }
    }
    
    /** Extrae el valor de un atributo específico (ej: tvg-logo="...") de una línea M3U. */
    private fun extraerAtributo(linea: String, atributo: String): String? {
        val patron = "$atributo=\"([^\"]*)\""
        val regex = Regex(patron)
        val coincidencia = regex.find(linea)
        return coincidencia?.groupValues?.get(1)
    }
}
