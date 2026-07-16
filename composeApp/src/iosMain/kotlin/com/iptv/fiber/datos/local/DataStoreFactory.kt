package com.iptv.fiber.datos.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

// Nota: En un entorno de producción iOS, se implementa con PreferenceDataStoreFactory 
// y las librerías de Path. Para que el proyecto compile inicialmente y permita trabajar 
// en Android, dejamos la firma.
actual fun createDataStore(): DataStore<Preferences> {
    throw NotImplementedError("DataStore para iOS requiere configuración de dependencias de okio en build.gradle")
}
