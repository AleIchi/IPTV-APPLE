package com.iptv.fiber.datos.utilidades

import com.iptv.fiber.datos.modelo.Canal


/** 
 * EL TRADUCTOR DE LISTAS (M3U).
 * Algunos usuarios no tienen "Xtream Codes", sino que tienen un archivo de texto llamado ".m3u"
 * que contiene todos los canales. Esta clase lee ese texto gigante línea por línea y lo
 * convierte en la lista de "Canales" que entiende nuestra app.
 */
class AnalizadorM3U {
    companion object {
        // EL BUSCADOR DE PALABRAS CLAVE:
        // Las listas M3U tienen etiquetas escondidas como tvg-logo="foto.jpg" o group-title="Deportes".
        // Esta "Fórmula Regex" es un escáner diseñado para extraer exactamente esos textos sin importar
        // si el creador de la lista usó comillas simples, dobles o ninguna.
        // Se compila una sola vez para que la app no se vuelva lenta.
        private val REGEX_ATRIBUTOS = Regex("""([a-zA-Z0-9_\-:]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")
    }
    
    /** 
     * LECTURA LÍNEA POR LÍNEA:
     * Toma el texto gigante de la lista M3U y lo empieza a leer de arriba hacia abajo.
     * Busca las líneas que empiezan con "#EXTINF" (donde está el nombre y logo)
     * y las líneas normales (donde está el link del video).
     */
    fun procesar(contenidoM3U: String): Map<String, List<Canal>> {
        val canales = mutableListOf<Canal>()
        
        var nombreActual: String? = null
        var logoActual: String? = null
        var grupoActual: String? = null
        
        val lineas = contenidoM3U.lineSequence().iterator()
        
        while (lineas.hasNext()) {
            val lineaCruda = lineas.next()
            val linea = lineaCruda.trim()

            if (linea.startsWith("#EXTINF", ignoreCase = true)) {
                // Procesar metadatos
                // Ejemplo: #EXTINF:-1 tvg-id="" tvg-name="" tvg-logo="http://..." group-title="Noticias",BBC News
                
                val atributos = extraerAtributos(linea)
                
                // Extraer Nombre (todo después de la última coma)
                val indiceComa = linea.lastIndexOf(',')
                var nombreExtraido = if (indiceComa != -1) {
                    linea.substring(indiceComa + 1).trim()
                } else {
                    ""
                }
                
                // Fallback si el nombre extraído está vacío o no se encontró coma
                if (nombreExtraido.isEmpty()) {
                    nombreExtraido = atributos["tvg-name"]
                        ?: atributos["tvg-id"]
                        ?: atributos["name"]
                        ?: "Canal Desconocido"
                }
                nombreActual = nombreExtraido

                // Extraer logotipo con múltiples variantes y tolerancia de mayúsculas/comillas
                logoActual = atributos["tvg-logo"]
                    ?: atributos["logo"]
                    ?: atributos["url-logo"]
                    ?: atributos["icon"]
                    ?: atributos["tvg-icon"]
                    ?: atributos["tvg-logo-url"]
                
                // Extraer Grupo
                grupoActual = atributos["group-title"]
                    ?: atributos["group-titulo"]
                    ?: atributos["tvg-group"]
                    ?: grupoActual // Preservar si ya fue extraído por #EXTGRP
                    ?: "Sin categoría"
                
            } else if (linea.startsWith("#EXTIMG", ignoreCase = true)) {
                val indiceColon = linea.indexOf(':')
                if (indiceColon != -1) {
                    val imgUrl = linea.substring(indiceColon + 1).trim()
                    if (imgUrl.isNotEmpty()) {
                        logoActual = imgUrl
                    }
                }
            } else if (linea.startsWith("#EXTGRP", ignoreCase = true)) {
                val indiceColon = linea.indexOf(':')
                if (indiceColon != -1) {
                    val grupo = linea.substring(indiceColon + 1).trim()
                    if (grupo.isNotEmpty()) {
                        grupoActual = grupo
                    }
                }
            } else if (!linea.startsWith("#") && linea.isNotEmpty()) {
                // ¡BINGO! Encontramos el enlace del video (la URL).
                // Como ya tenemos guardado el nombre y el logo de la línea anterior (#EXTINF),
                // ahora juntamos todo y creamos el objeto "Canal".
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
        }
        
        // Agrupar canales por categoría inmediatamente para facilitar su uso
        return canales.groupBy { it.id_categoria }
    }
    
    /** Extrae todos los atributos clave-valor de una línea de manera robusta. */
    private fun extraerAtributos(linea: String): Map<String, String> {
        val atributos = mutableMapOf<String, String>()
        // Expresión regular robusta y correcta para atributos M3U
        // Permite comillas dobles (que pueden contener comillas simples), comillas simples o valores sin comillas
        val coincidencias = REGEX_ATRIBUTOS.findAll(linea)
        
        for (coincidencia in coincidencias) {
            val clave = coincidencia.groupValues[1].lowercase()
            // Determinar qué grupo capturó el valor (dobles, simples, o sin comillas)
            val valor = coincidencia.groupValues[2].takeIf { it.isNotEmpty() }
                ?: coincidencia.groupValues[3].takeIf { it.isNotEmpty() }
                ?: coincidencia.groupValues[4]
                
            if (valor.isNotBlank()) {
                atributos[clave] = valor.trim()
            }
        }
        return atributos
    }
}
