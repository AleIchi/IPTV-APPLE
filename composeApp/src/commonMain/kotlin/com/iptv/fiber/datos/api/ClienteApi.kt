package com.iptv.fiber.datos.api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP usando Ktor para KMP.
 */
class ClienteApi {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 30000
        }
    }

    private val apisPorServidor = mutableMapOf<String, ApiXtreamCodes>()

    companion object {
        private val INSTANCIA by lazy { ClienteApi() }
        fun obtener(): ClienteApi = INSTANCIA
    }

    fun crearApi(): ApiXtreamCodes {
        return ApiXtreamCodes(httpClient)
    }

    fun crearApiParaServidor(urlBase: String): ApiXtreamCodes {
        val urlLimpia = if (urlBase.endsWith("/")) urlBase else "$urlBase/"
        return apisPorServidor.getOrPut(urlLimpia) {
            ApiXtreamCodes(httpClient)
        }
    }
}
