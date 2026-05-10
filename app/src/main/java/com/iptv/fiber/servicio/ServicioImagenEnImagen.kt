package com.iptv.fiber.servicio

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Servicio placeholder para la funcionalidad de Imagen en Imagen (PiP).
 * El PiP real se gestiona directamente en ActividadReproductor.
 */
class ServicioImagenEnImagen : Service() {
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

