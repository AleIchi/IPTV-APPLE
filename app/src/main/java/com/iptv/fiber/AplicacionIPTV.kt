package com.iptv.fiber

import android.app.Application
import android.content.Context
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient

class AplicacionIPTV : Application() {
    
    companion object {
        lateinit var instancia: AplicacionIPTV
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instancia = this

        // Configurar Coil con User-Agent de navegador para cargar logos
        // de CDNs externos como iptveditor.com que bloquean las apps Android
        val clienteHttp = OkHttpClient.Builder()
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

        val cargadorImagenes = ImageLoader.Builder(this)
            .okHttpClient(clienteHttp)
            .respectCacheHeaders(false)
            .build()

        Coil.setImageLoader(cargadorImagenes)
    }
}
