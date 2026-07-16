package com.iptv.fiber.tv.componentes

import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.Categoria

private val palabrasAdultasTV = listOf(
    "adult", "adulto", "adultos", "+18", "18+", "xxx", "porn", "porno",
    "erotic", "erotico", "erotica", "hot", "sexy", "sex", "sexo",
    "playboy", "private", "hustler", "penthouse", "venus", "sextreme"
)

/**
 * Determina si contenido adulto tv cumple la condicion esperada.
 */
fun esContenidoAdultoTV(
    canal: Canal,
    categorias: List<Categoria> = emptyList()
): Boolean {
    val categoria = categorias.firstOrNull { it.id_categoria == canal.id_categoria }?.nombre_categoria.orEmpty()
    val texto = "${canal.nombre} $categoria".lowercase()
    return palabrasAdultasTV.any { palabra -> texto.contains(palabra) }
}
