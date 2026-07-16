package com.iptv.fiber.datos.api

import com.iptv.fiber.datos.modelo.RespuestaApi
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.EPG
import com.iptv.fiber.datos.modelo.InfoUsuario
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ApiXtreamCodes(private val client: HttpClient) {

    suspend fun autenticar(url: String, usuario: String, contrasena: String): HttpResponse {
        return client.get(url) {
            parameter("username", usuario)
            parameter("password", contrasena)
        }
    }

    suspend fun obtenerInfoUsuario(url: String, usuario: String, contrasena: String): RespuestaApi<InfoUsuario>? {
        return try {
            client.get(url) {
                parameter("username", usuario)
                parameter("password", contrasena)
            }.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun obtenerCategoriasEnVivo(url: String, usuario: String, contrasena: String, accion: String = "get_live_categories"): HttpResponse {
        return client.get(url) {
            parameter("username", usuario)
            parameter("password", contrasena)
            parameter("action", accion)
        }
    }

    suspend fun obtenerCanalesEnVivo(url: String, usuario: String, contrasena: String, accion: String = "get_live_streams", idCategoria: String? = null): HttpResponse {
        return client.get(url) {
            parameter("username", usuario)
            parameter("password", contrasena)
            parameter("action", accion)
            if (idCategoria != null) parameter("category_id", idCategoria)
        }
    }

    suspend fun obtenerGuiaProgramacion(url: String, usuario: String, contrasena: String, accion: String = "get_short_epg", idTransmision: Int? = null): Map<String, List<EPG>> {
        return try {
            client.get(url) {
                parameter("username", usuario)
                parameter("password", contrasena)
                parameter("action", accion)
                if (idTransmision != null) parameter("stream_id", idTransmision)
            }.body()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun obtenerUrlTransmision(url: String, usuario: String, contrasena: String, accion: String = "get_short_epg", tipo: String, id: Int): String {
        return try {
            client.get(url) {
                parameter("username", usuario)
                parameter("password", contrasena)
                parameter("action", accion)
                parameter("type", tipo)
                parameter("id", id)
            }.bodyAsText()
        } catch (e: Exception) {
            ""
        }
    }
}
