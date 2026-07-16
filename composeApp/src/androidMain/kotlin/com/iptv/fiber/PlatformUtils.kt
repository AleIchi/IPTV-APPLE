package com.iptv.fiber

import java.util.UUID

@androidx.compose.runtime.Composable
actual fun EfectoBloqueoCaptura(bloquear: Boolean) {
    val vista = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.LaunchedEffect(bloquear) {
        val ventana = (vista.context as? android.app.Activity)?.window
        if (bloquear) {
            ventana?.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            ventana?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

actual fun generarUUID(): String {
    return UUID.randomUUID().toString()
}

actual suspend fun descargarM3U(url: String): String {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val clienteEstandar = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val solicitud = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            clienteEstandar.newCall(solicitud).execute().use { respuesta ->
                if (!respuesta.isSuccessful) {
                    throw java.io.IOException("Error de respuesta del servidor: ${respuesta.code}")
                }
                return@withContext respuesta.body?.string() ?: ""
            }
        } catch (e: javax.net.ssl.SSLException) {
            android.util.Log.w("AuthRepo", "Fallo SSL estándar. Reintentando con cliente tolerante...", e)
            
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                object : javax.net.ssl.X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )

            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            val clienteTolerante = okhttp3.OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            clienteTolerante.newCall(solicitud).execute().use { respuesta ->
                if (!respuesta.isSuccessful) {
                    throw java.io.IOException("Error de respuesta del servidor: ${respuesta.code}")
                }
                return@withContext respuesta.body?.string() ?: ""
            }
        }
    }
}
