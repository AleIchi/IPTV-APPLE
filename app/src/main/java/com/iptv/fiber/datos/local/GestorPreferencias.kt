package com.iptv.fiber.datos.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.almacenDatos: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

/** Gestiona las preferencias persistentes del usuario usando DataStore. */
class GestorPreferencias(private val contexto: Context) {

    companion object {
        val CLAVE_TEMA              = stringPreferencesKey("tema")
        val CLAVE_BLOQUEAR_CAPTURA  = booleanPreferencesKey("bloquear_captura")
        val CLAVE_CONTROL_PARENTAL  = booleanPreferencesKey("control_parental_activo")
        val CLAVE_PIN_PARENTAL      = stringPreferencesKey("pin_parental")
        val CLAVE_ID_SERVIDOR_ACTIVO = stringPreferencesKey("id_servidor_activo")

        // Credenciales de autenticación
        val CLAVE_URL_SERVIDOR      = stringPreferencesKey("url_servidor")
        val CLAVE_USUARIO           = stringPreferencesKey("usuario")
        val CLAVE_CONTRASENA        = stringPreferencesKey("contrasena")
        
        // Detalles de cuenta
        val CLAVE_FECHA_EXPIRACION  = stringPreferencesKey("fecha_expiracion")
        val CLAVE_MAX_CONEXIONES    = stringPreferencesKey("max_conexiones")
        val CLAVE_ESTADO_CUENTA     = stringPreferencesKey("estado_cuenta")
    }

    val tema: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_TEMA] ?: "clasico"
    }

    suspend fun establecerTema(nuevoTema: String) {
        contexto.almacenDatos.edit { prefs -> prefs[CLAVE_TEMA] = nuevoTema }
    }

    val bloquearCaptura: Flow<Boolean> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_BLOQUEAR_CAPTURA] ?: false
    }

    suspend fun establecerBloqueoCaptura(activo: Boolean) {
        contexto.almacenDatos.edit { prefs -> prefs[CLAVE_BLOQUEAR_CAPTURA] = activo }
    }

    val controlParentalActivo: Flow<Boolean> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_CONTROL_PARENTAL] ?: false
    }

    suspend fun establecerControlParental(activo: Boolean) {
        contexto.almacenDatos.edit { prefs -> prefs[CLAVE_CONTROL_PARENTAL] = activo }
    }

    val pinParental: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_PIN_PARENTAL] ?: ""
    }

    suspend fun establecerPinParental(pin: String) {
        contexto.almacenDatos.edit { prefs -> prefs[CLAVE_PIN_PARENTAL] = pin }
    }

    val idServidorActivo: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_ID_SERVIDOR_ACTIVO] ?: ""
    }

    suspend fun establecerIdServidorActivo(idServidor: String) {
        contexto.almacenDatos.edit { prefs -> prefs[CLAVE_ID_SERVIDOR_ACTIVO] = idServidor }
    }

    val urlServidor: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_URL_SERVIDOR] ?: ""
    }

    val usuario: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_USUARIO] ?: ""
    }

    val contrasena: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_CONTRASENA] ?: ""
    }
    
    val fechaExpiracion: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_FECHA_EXPIRACION] ?: ""
    }
    
    val maxConexiones: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_MAX_CONEXIONES] ?: ""
    }
    
    val estadoCuenta: Flow<String> = contexto.almacenDatos.data.map { prefs ->
        prefs[CLAVE_ESTADO_CUENTA] ?: ""
    }

    suspend fun guardarCredenciales(url: String, usuario: String, contrasena: String) {
        contexto.almacenDatos.edit { prefs ->
            prefs[CLAVE_URL_SERVIDOR] = url
            prefs[CLAVE_USUARIO]      = usuario
            prefs[CLAVE_CONTRASENA]   = contrasena
        }
    }
    
    suspend fun guardarDetallesCuenta(expiracion: String, maxCon: String, estado: String) {
        contexto.almacenDatos.edit { prefs ->
            prefs[CLAVE_FECHA_EXPIRACION] = expiracion
            prefs[CLAVE_MAX_CONEXIONES]   = maxCon
            prefs[CLAVE_ESTADO_CUENTA]    = estado
        }
    }

    suspend fun limpiarCredenciales() {
        contexto.almacenDatos.edit { prefs ->
            prefs.remove(CLAVE_URL_SERVIDOR)
            prefs.remove(CLAVE_USUARIO)
            prefs.remove(CLAVE_CONTRASENA)
            prefs.remove(CLAVE_FECHA_EXPIRACION)
            prefs.remove(CLAVE_MAX_CONEXIONES)
            prefs.remove(CLAVE_ESTADO_CUENTA)
        }
    }
}
