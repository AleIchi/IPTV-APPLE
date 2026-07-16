package com.iptv.fiber.datos.modelo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable

/** Contenedor genérico de la respuesta principal de la API de Xtream Codes. */
@Immutable
@Serializable
data class RespuestaApi<T>(
    @SerialName("user_info")
    val infoUsuario: InfoUsuario? = null,
    @SerialName("server_info")
    val infoServidor: InfoServidor? = null,
    val categorias: List<Categoria>? = null,
    @SerialName("available_channels")
    val canalesDisponibles: List<Canal>? = null,
    @SerialName("epg_listings")
    val listadoEpg: Map<String, List<EPG>>? = null
)

/** Datos de la cuenta del usuario: estado, fecha de expiración y límite de conexiones. */
@Immutable
@Serializable
data class InfoUsuario(
    @SerialName("username")
    val usuario: String = "",
    @SerialName("password")
    val contrasena: String = "",
    @SerialName("message")
    val mensaje: String? = null,
    val auth: Int = 0,
    @SerialName("status")
    val estado: String = "",
    @SerialName("exp_date")
    val fechaExpiracion: String? = null,
    @SerialName("is_trial")
    val esPrueba: String? = null,
    @SerialName("active_cons")
    val conexionesActivas: String? = null,
    @SerialName("created_at")
    val creadoEn: String? = null,
    @SerialName("max_connections")
    val conexionesMaximas: String? = null,
    @SerialName("allowed_output_formats")
    val formatosSalidaPermitidos: List<String>? = null
)

/** Datos técnicos del servidor IPTV: URL, puertos y zona horaria. */
@Immutable
@Serializable
data class InfoServidor(
    val url: String = "",
    val puerto: String = "",
    @SerialName("https_port")
    val puertoHttps: String? = null,
    @SerialName("server_protocol")
    val protocoloServidor: String? = null,
    @SerialName("rtmp_port")
    val puertoRtmp: String? = null,
    @SerialName("timezone")
    val zonaHoraria: String? = null,
    @SerialName("timestamp_now")
    val marcaTiempoActual: Long? = null,
    @SerialName("time_now")
    val horaActual: String? = null
)

/** Categoría de contenido (Deportes, Películas, etc.) devuelta por la API. */
@Immutable
@Serializable
data class Categoria(
    @SerialName("category_id") val id_categoria: String,
    @SerialName("category_name") val nombre_categoria: String,
    @SerialName("parent_id") val id_padre: String? = null
)

/** Canal de TV en vivo con su identificador, nombre, logo y categoría. */
@Immutable
@Serializable
data class Canal(
    @SerialName("num") val numero: String? = null,
    @SerialName("name") val nombre: String = "",
    @SerialName("stream_type") val tipo_transmision: String = "live",
    @SerialName("stream_id") val id_transmision: Int = 0,
    @SerialName("stream_icon") val icono_transmision: String? = null,
    @SerialName("epg_channel_id") val id_canal_epg: String? = null,
    @SerialName("added") val agregadoEn: String? = null,
    @SerialName("category_id") val id_categoria: String = "",
    @SerialName("category_ids") val ids_categorias: List<Int>? = null,
    @SerialName("custom_sid") val sid_personalizado: String? = null,
    @SerialName("tv_archive") val archivo_tv: String? = null,
    @SerialName("direct_source") val fuenteDirecta: String? = null,
    @SerialName("tv_archive_duration") val duracion_archivo_tv: String? = null
)

/** Entrada de la guía de programación electrónica (EPG) con título, inicio y fin del programa. */
@Immutable
@Serializable
data class EPG(
    val id: String,
    @SerialName("epg_id") val id_epg: String,
    @SerialName("title") val titulo: String,
    @SerialName("lang") val idioma: String? = null,
    @SerialName("start") val inicio: String,
    @SerialName("end") val fin: String,
    @SerialName("description") val descripcion: String? = null,
    @SerialName("channel_id") val id_canal: String? = null,
    @SerialName("start_timestamp") val marca_tiempo_inicio: String? = null,
    @SerialName("stop_timestamp") val marca_tiempo_fin: String? = null,
    @SerialName("now_playing") val reproduciendo_ahora: String? = null,
    @SerialName("has_archive") val tiene_archivo: Int = 0
)
