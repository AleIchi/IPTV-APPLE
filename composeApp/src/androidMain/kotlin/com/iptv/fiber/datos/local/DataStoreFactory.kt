package com.iptv.fiber.datos.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.iptv.fiber.AplicacionIPTV

actual fun createDataStore(): DataStore<Preferences> {
    // Falta configurar la creación con Okio para KMP completo,
    // pero para Android podemos usar el viejo Property Delegate de context
    // o construirlo a través del Context global.
    val context = AplicacionIPTV.instancia
    return context.almacenDatos
}
