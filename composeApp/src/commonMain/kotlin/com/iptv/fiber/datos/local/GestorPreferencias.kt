package com.iptv.fiber.datos.local

// removed android import: import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map



/** 
 * EL ARCHIVERO DE CONFIGURACIONES (DataStore).
 * Mientras que Room (BaseDatosIPTV) se usa para guardar miles de canales y listas enormes,
 * el GestorPreferencias se usa para guardar "papelitos" pequeños y rápidos:
 * ¿Qué tema eligió el usuario? ¿Cuál es su contraseña? ¿Tiene activo el PIN parental?
 */
class GestorPreferencias() {

    companion object {
        val CLAVE_TEMA              = stringPreferencesKey("tema")
        val CLAVE_BLOQUEAR_CAPTURA  = booleanPreferencesKey("bloquear_captura")
        val CLAVE_CONTROL_PARENTAL  = booleanPreferencesKey("control_parental_activo")
        val CLAVE_PIN_PARENTAL      = stringPreferencesKey("pin_parental")
        val CLAVE_ID_SERVIDOR_ACTIVO = stringPreferencesKey("id_servidor_activo")
        val CLAVE_MAC_VIRTUAL       = stringPreferencesKey("mac_virtual")
        val CLAVE_ULTIMA_ACTUALIZACION_QR = stringPreferencesKey("ultima_actualizacion_qr")

        // Credenciales de autenticación
        val CLAVE_URL_SERVIDOR      = stringPreferencesKey("url_servidor")
        val CLAVE_USUARIO           = stringPreferencesKey("usuario")
        val CLAVE_CONTRASENA        = stringPreferencesKey("contrasena")
        
        // Detalles de cuenta
        val CLAVE_FECHA_EXPIRACION  = stringPreferencesKey("fecha_expiracion")
        val CLAVE_MAX_CONEXIONES    = stringPreferencesKey("max_conexiones")
        val CLAVE_ESTADO_CUENTA     = stringPreferencesKey("estado_cuenta")

        // Preferencias de reproducción
        val CLAVE_CALIDAD_VIDEO     = stringPreferencesKey("calidad_video") // "Automático", "1080p", "720p", "480p"
    }

    val tema: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_TEMA] ?: "clasico"
    }

    /** Persiste el [nuevoTema] seleccionado por el usuario en DataStore. */
    suspend fun establecerTema(nuevoTema: String) {
        createDataStore().edit { prefs -> prefs[CLAVE_TEMA] = nuevoTema }
    }

    val bloquearCaptura: Flow<Boolean> = createDataStore().data.map { prefs ->
        prefs[CLAVE_BLOQUEAR_CAPTURA] ?: false
    }

    /** Activa o desactiva la protección contra capturas de pantalla (FLAG_SECURE). */
    suspend fun establecerBloqueoCaptura(activo: Boolean) {
        createDataStore().edit { prefs -> prefs[CLAVE_BLOQUEAR_CAPTURA] = activo }
    }

    val controlParentalActivo: Flow<Boolean> = createDataStore().data.map { prefs ->
        prefs[CLAVE_CONTROL_PARENTAL] ?: false
    }

    /** Habilita o deshabilita el control parental (acceso a ajustes protegido por PIN). */
    suspend fun establecerControlParental(activo: Boolean) {
        createDataStore().edit { prefs -> prefs[CLAVE_CONTROL_PARENTAL] = activo }
    }

    val pinParental: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_PIN_PARENTAL] ?: ""
    }

    /** Guarda el [pin] numérico del control parental en DataStore. */
    suspend fun establecerPinParental(pin: String) {
        createDataStore().edit { prefs -> prefs[CLAVE_PIN_PARENTAL] = pin }
    }

    val idServidorActivo: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_ID_SERVIDOR_ACTIVO] ?: ""
    }

    /** Persiste el [idServidor] del servidor seleccionado para restaurarlo al próximo inicio de sesión. */
    suspend fun establecerIdServidorActivo(idServidor: String) {
        createDataStore().edit { prefs -> prefs[CLAVE_ID_SERVIDOR_ACTIVO] = idServidor }
    }

    val urlServidor: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_URL_SERVIDOR] ?: ""
    }

    val usuario: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_USUARIO] ?: ""
    }

    val contrasena: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_CONTRASENA] ?: ""
    }
    
    val fechaExpiracion: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_FECHA_EXPIRACION] ?: ""
    }
    
    val maxConexiones: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_MAX_CONEXIONES] ?: ""
    }
    
    val estadoCuenta: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_ESTADO_CUENTA] ?: ""
    }

    val calidadVideo: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_CALIDAD_VIDEO] ?: "Automático"
    }

    /** Persiste la [calidad] de video preferida (Automático, 1080p, 720p, 480p). */
    suspend fun establecerCalidadVideo(calidad: String) {
        createDataStore().edit { prefs -> prefs[CLAVE_CALIDAD_VIDEO] = calidad }
    }

    /** 
     * AUTO-INICIO DE SESIÓN:
     * Persiste [url], [usuario] y [contrasena] en el archivero.
     * Gracias a esto, la próxima vez que el usuario abra la app,
     * ActividadPrincipal leerá estos datos y saltará la pantalla de Login.
     */
    suspend fun guardarCredenciales(url: String, usuario: String, contrasena: String) {
        createDataStore().edit { prefs ->
            prefs[CLAVE_URL_SERVIDOR] = url
            prefs[CLAVE_USUARIO]      = usuario
            prefs[CLAVE_CONTRASENA]   = contrasena
        }
    }
    
    /** Guarda los detalles de cuenta recibidos del servidor ([expiracion], [maxCon], [estado]) para mostrarlos en ajustes. */
    suspend fun guardarDetallesCuenta(expiracion: String, maxCon: String, estado: String) {
        createDataStore().edit { prefs ->
            prefs[CLAVE_FECHA_EXPIRACION] = expiracion
            prefs[CLAVE_MAX_CONEXIONES]   = maxCon
            prefs[CLAVE_ESTADO_CUENTA]    = estado
        }
    }

    /** Elimina credenciales y detalles de cuenta de DataStore al cerrar sesión. */
    suspend fun limpiarCredenciales() {
        createDataStore().edit { prefs ->
            prefs.remove(CLAVE_URL_SERVIDOR)
            prefs.remove(CLAVE_USUARIO)
            prefs.remove(CLAVE_CONTRASENA)
            prefs.remove(CLAVE_FECHA_EXPIRACION)
            prefs.remove(CLAVE_MAX_CONEXIONES)
            prefs.remove(CLAVE_ESTADO_CUENTA)
        }
    }

    val macVirtual: Flow<String> = createDataStore().data.map { prefs ->
        prefs[CLAVE_MAC_VIRTUAL] ?: ""
    }

    /** 
     * EL TRUCO DE LA MAC VIRTUAL:
     * Algunos servidores antiguos piden una dirección "MAC" (identificador de hardware)
     * para verificar el dispositivo. Como Android moderno prohíbe leer la MAC real por privacidad,
     * esta función inventa una (basada en UUID) y la guarda para siempre, engañando al servidor
     * de forma legal para que nos deje entrar.
     */
    suspend fun obtenerOGenerarMacVirtual(): String {
        var actual = ""
        createDataStore().edit { prefs ->
            val mac = prefs[CLAVE_MAC_VIRTUAL]
            if (mac.isNullOrEmpty()) {
                val uuid = com.iptv.fiber.generarUUID().replace("-", "")
                val sb = StringBuilder()
                for (i in 0..5) {
                    sb.append(uuid.substring(i * 2, i * 2 + 2).uppercase())
                    if (i < 5) sb.append(":")
                }
                val nuevaMac = sb.toString()
                prefs[CLAVE_MAC_VIRTUAL] = nuevaMac
                actual = nuevaMac
            } else {
                actual = mac
            }
        }
        return actual
    }

    /** Persiste la [fecha] de la última activación por QR para controlar la frecuencia de reactivación. */
    suspend fun guardarUltimaActualizacionQR(fecha: String) {
        createDataStore().edit { prefs ->
            prefs[CLAVE_ULTIMA_ACTUALIZACION_QR] = fecha
        }
    }

    /** Devuelve la fecha de la última activación por QR guardada en DataStore. */
    suspend fun obtenerUltimaActualizacionQR(): String {
        var fecha = ""
        createDataStore().edit { prefs ->
            fecha = prefs[CLAVE_ULTIMA_ACTUALIZACION_QR] ?: ""
        }
        return fecha
    }
}
