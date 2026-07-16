package com.iptv.fiber.servicio

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Servicio marcador de posición para la funcionalidad de imagen en imagen (PiP).
 * El PiP real se gestiona directamente en ActividadReproductor.
 */
class ServicioImagenEnImagen : Service() {
    
    /**
     * Indica que este servicio no expone un binder para clientes externos.
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

