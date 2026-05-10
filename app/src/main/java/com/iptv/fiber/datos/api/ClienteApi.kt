package com.iptv.fiber.datos.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Clase que construye el cliente HTTP para comunicarse con la API. */
class ClienteApi {

    private val gson: Gson = GsonBuilder()
        .setLenient() // Permite JSON más flexible
        .serializeNulls() // Serializa valores nulos
        .create()

    // SOLO registra cabeceras en producción. Level.BODY serializa TODOS los bytes
    // de las respuestas (listas de canales pueden tener MBs), causando lag enorme.
    private val interceptorRegistro = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    // Pool de conexiones: reutiliza conexiones TCP activas en lugar de crear nuevas
    // cada vez. Crítico para zapping rápido entre canales del mismo servidor.
    private val poolConexiones = okhttp3.ConnectionPool(
        5,          // Máximo 5 conexiones reutilizables en paralelo
        30,         // Mantener conexión abierta por 30 segundos de inactividad
        TimeUnit.SECONDS
    )

    private val clienteHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "FiberZapp")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(interceptorRegistro)
        .connectionPool(poolConexiones)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** Crea la API con URL base genérica (será reemplazada). */
    fun crearApi(): ApiXtreamCodes {
        return Retrofit.Builder()
            .baseUrl("https://example.com/") // La URL base será reemplazada
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiXtreamCodes::class.java)
    }

    /** Crea la API apuntando directamente a un servidor específico. */
    fun crearApiParaServidor(urlBase: String): ApiXtreamCodes {
        val urlLimpia = if (urlBase.endsWith("/")) urlBase else "$urlBase/"
        return Retrofit.Builder()
            .baseUrl(urlLimpia)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiXtreamCodes::class.java)
    }
}
