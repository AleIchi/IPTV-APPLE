package com.iptv.fiber

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import okhttp3.OkHttpClient
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Clase de aplicación que se crea una sola vez al arrancar.
 * ¡Es lo PRIMERO que se ejecuta cuando el usuario abre la app!
 *
 * Para que el arranque sea RÁPIDO, ya NO construimos aquí (en onCreate) el cargador de
 * imágenes ni el cliente HTTP/SSL (eso era pesado y bloqueaba el primer dibujo de pantalla).
 * En su lugar implementamos [ImageLoaderFactory]: Coil llama a [newImageLoader] de forma
 * diferida, recién la PRIMERA vez que se necesita cargar una imagen, fuera del camino crítico
 * del arranque.
 */
class AplicacionIPTV : Application(), ImageLoaderFactory {

    companion object {
        /** Referencia global a la instancia de la aplicación. */
        lateinit var instancia: AplicacionIPTV
            private set
    }

    /**
     * Se ejecuta al nacer la app. Lo mantenemos LIGERO: solo guardamos la instancia global
     * e instalamos un registrador de fallos. Todo lo pesado (SSL, HTTP, caché de imágenes)
     * se difiere a [newImageLoader].
     */
    override fun onCreate() {
        super.onCreate()
        instancia = this
        instalarRegistradorDeFallos()
        Thread {
            com.iptv.fiber.datos.api.ClienteApi.obtener()
            com.iptv.fiber.datos.local.base_datos.BaseDatosIPTV.obtenerBaseDatos()
        }.start()
    }

    /**
     * RED DE SEGURIDAD: si la app se cierra por un error no controlado, guardamos el detalle
     * (stack trace) en un archivo dentro del almacenamiento de la app: filesDir/ultimo_crash.txt.
     * Así podemos revisar la causa exacta de un cierre sin necesidad de herramientas externas
     * (logcat) en el TV Box. Se respeta el manejador previo para no romper el comportamiento estándar.
     */
    private fun instalarRegistradorDeFallos() {
        val manejadorPrevio = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            try {
                val archivo = java.io.File(filesDir, "ultimo_crash.txt")
                archivo.writeText(
                    "Fecha: ${java.util.Date()}\n" +
                        "Hilo: ${hilo.name}\n\n" +
                        android.util.Log.getStackTraceString(error)
                )
            } catch (_: Throwable) {
                // Si ni siquiera podemos escribir el log, no hacemos nada para no enmascarar el fallo.
            }
            // Delegamos al manejador por defecto para que el sistema cierre la app como siempre.
            manejadorPrevio?.uncaughtException(hilo, error)
        }
    }

    /**
     * Construye el cargador de imágenes (Coil) la primera vez que se necesita.
     * Configura el bypass de SSL (certificados IPTV vencidos), un User-Agent de navegador,
     * y límites de caché adaptados a la RAM disponible (TV Box antiguos).
     */
    override fun newImageLoader(): ImageLoader {
        // --- BYPASS SSL ---
        // Muchos servidores IPTV tienen certificados vencidos o la hora de la TV está mal.
        // Confiamos en todos los certificados para que los logos carguen igual.
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }

        // --- CLIENTE HTTP (camuflado como Chrome) ---
        val clienteHttp = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { cadena ->
                val solicitud = cadena.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .build()
                cadena.proceed(solicitud)
            }
            .build()

        // --- OPTIMIZACIÓN DE MEMORIA ---
        // Si es una TV Box con poca RAM (≤1.2 GB), guardamos menos imágenes para no congelar.
        val administradorActividad = getSystemService(android.app.ActivityManager::class.java)
        val infoMemoria = android.app.ActivityManager.MemoryInfo().also { administradorActividad.getMemoryInfo(it) }
        val esDispositivoBajaRam = infoMemoria.totalMem <= 1_200L * 1024 * 1024 // ≤ 1.2 GB

        val porcentajeCache = if (esDispositivoBajaRam) 0.08 else 0.12
        val limiteDisco = if (esDispositivoBajaRam) 25L * 1024 * 1024 else 40L * 1024 * 1024
        val descargasParalelas = if (esDispositivoBajaRam) 4 else 6

        return ImageLoader.Builder(this)
            .okHttpClient(clienteHttp)
            .respectCacheHeaders(false)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(porcentajeCache)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("logos_coil_cache"))
                    .maxSizeBytes(limiteDisco)
                    .build()
            }
            .dispatcher(kotlinx.coroutines.Dispatchers.IO.limitedParallelism(descargasParalelas))
            .build()
    }
}
