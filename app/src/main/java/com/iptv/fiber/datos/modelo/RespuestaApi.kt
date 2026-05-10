package com.iptv.fiber.datos.modelo

import com.google.gson.annotations.SerializedName

data class RespuestaApi<T>(
    @SerializedName("user_info")
    val infoUsuario: InfoUsuario? = null,
    @SerializedName("server_info")
    val infoServidor: InfoServidor? = null,
    val categorias: List<Categoria>? = null,
    @SerializedName("available_channels")
    val canalesDisponibles: List<Canal>? = null,
    val peliculas: List<Pelicula>? = null,
    val series: List<Serie>? = null,
    @SerializedName("epg_listings")
    val listadoEpg: Map<String, List<EPG>>? = null
)

data class InfoUsuario(
    val usuario: String = "",
    val contrasena: String = "",
    @SerializedName("message")
    val mensaje: String? = null,
    val auth: Int = 0,
    @SerializedName("status")
    val estado: String = "",
    @SerializedName("exp_date")
    val fechaExpiracion: String? = null,
    @SerializedName("is_trial")
    val esPrueba: String? = null,
    @SerializedName("active_cons")
    val conexionesActivas: String? = null,
    @SerializedName("created_at")
    val creadoEn: String? = null,
    @SerializedName("max_connections")
    val conexionesMaximas: String? = null,
    @SerializedName("allowed_output_formats")
    val formatosSalidaPermitidos: List<String>? = null
)

data class InfoServidor(
    val url: String = "",
    val puerto: String = "",
    @SerializedName("https_port")
    val puertoHttps: String? = null,
    @SerializedName("server_protocol")
    val protocoloServidor: String? = null,
    @SerializedName("rtmp_port")
    val puertoRtmp: String? = null,
    @SerializedName("timezone")
    val zonaHoraria: String? = null,
    @SerializedName("timestamp_now")
    val marcaTiempoActual: Long? = null,
    @SerializedName("time_now")
    val horaActual: String? = null
)

data class Categoria(
    @SerializedName("category_id") val id_categoria: String,
    @SerializedName("category_name") val nombre_categoria: String,
    @SerializedName("parent_id") val id_padre: String? = null
)

data class Canal(
    @SerializedName("num") val numero: String? = null,
    @SerializedName("name") val nombre: String = "",
    @SerializedName("stream_type") val tipo_transmision: String = "live",
    @SerializedName("stream_id") val id_transmision: Int = 0,
    @SerializedName("stream_icon") val icono_transmision: String? = null,
    @SerializedName("epg_channel_id") val id_canal_epg: String? = null,
    @SerializedName("added") val agregadoEn: String? = null,
    @SerializedName("category_id") val id_categoria: String = "",
    @SerializedName("category_ids") val ids_categorias: List<Int>? = null,
    @SerializedName("custom_sid") val sid_personalizado: String? = null,
    @SerializedName("tv_archive") val archivo_tv: String? = null,
    @SerializedName("direct_source") val fuenteDirecta: String? = null,
    @SerializedName("tv_archive_duration") val duracion_archivo_tv: String? = null
)

data class Pelicula(
    @SerializedName("stream_id") val id_transmision: Int,
    @SerializedName("num") val numero: String? = null,
    @SerializedName("name") val nombre: String,
    @SerializedName("title") val titulo: String? = null,
    @SerializedName("year") val anio: String? = null,
    @SerializedName("stream_icon") val icono_transmision: String? = null,
    @SerializedName("rating") val calificacion: String? = null,
    @SerializedName("rating_5based") val calificacion_5: Double? = null,
    @SerializedName("added") val agregadoEn: String? = null,
    @SerializedName("category_id") val id_categoria: String,
    @SerializedName("category_ids") val ids_categorias: List<Int>? = null,
    @SerializedName("container_extension") val extension_contenedor: String? = null,
    @SerializedName("custom_sid") val sid_personalizado: String? = null,
    @SerializedName("direct_source") val fuenteDirecta: String? = null
)

data class Serie(
    @SerializedName("series_id") val id_serie: Int,
    @SerializedName("num") val numero: String? = null,
    @SerializedName("name") val nombre: String,
    @SerializedName("cover") val portada: String? = null,
    @SerializedName("plot") val sinopsis: String? = null,
    @SerializedName("cast") val reparto: String? = null,
    @SerializedName("director") val director: String? = null,
    @SerializedName("genre") val genero: String? = null,
    @SerializedName("releaseDate") val fechaLanzamiento: String? = null,
    @SerializedName("rating") val calificacion: String? = null,
    @SerializedName("rating_5based") val calificacion_5: Double? = null,
    @SerializedName("backdrop_path") val ruta_fondo: List<String>? = null,
    @SerializedName("youtube_trailer") val trailer_youtube: String? = null,
    @SerializedName("episode_run_time") val duracion_episodio: String? = null,
    @SerializedName("category_id") val id_categoria: String,
    @SerializedName("seasons") val temporadas: List<Temporada>? = null
)

data class Temporada(
    @SerializedName("air_date") val fecha_emision: String? = null,
    @SerializedName("episode_count") val conteo_episodios: Int,
    val id: Int,
    @SerializedName("name") val nombre: String,
    @SerializedName("overview") val resumen: String? = null,
    @SerializedName("season_number") val numero_temporada: Int,
    @SerializedName("cover") val portada: String? = null,
    @SerializedName("episodes") val episodios: List<Episodio>? = null
)

data class Episodio(
    @SerializedName("id") val id: String,
    @SerializedName("episode_num") val numero_episodio: Int,
    @SerializedName("title") val titulo: String,
    @SerializedName("container_extension") val extension_contenedor: String? = null,
    @SerializedName("info") val informacion: InfoEpisodio? = null,
    @SerializedName("added") val agregadoEn: String? = null,
    @SerializedName("season") val temporada: Int,
    @SerializedName("direct_source") val fuenteDirecta: String? = null
)

data class InfoEpisodio(
    @SerializedName("plot") val sinopsis: String? = null,
    @SerializedName("duration") val duracion: String? = null,
    @SerializedName("movie_image") val imagen_pelicula: String? = null,
    @SerializedName("bitrate") val tasa_bits: String? = null,
    @SerializedName("rating") val calificacion: String? = null
)

data class EPG(
    val id: String,
    @SerializedName("epg_id") val id_epg: String,
    @SerializedName("title") val titulo: String,
    @SerializedName("lang") val idioma: String? = null,
    @SerializedName("start") val inicio: String,
    @SerializedName("end") val fin: String,
    @SerializedName("description") val descripcion: String? = null,
    @SerializedName("channel_id") val id_canal: String? = null,
    @SerializedName("start_timestamp") val marca_tiempo_inicio: String? = null,
    @SerializedName("stop_timestamp") val marca_tiempo_fin: String? = null,
    @SerializedName("now_playing") val reproduciendo_ahora: String? = null,
    @SerializedName("has_archive") val tiene_archivo: Int = 0
)

data class Radio(
    @SerializedName("num") val numero: String? = null,
    @SerializedName("name") val nombre: String,
    @SerializedName("stream_type") val tipo_transmision: String,
    @SerializedName("stream_id") val id_transmision: Int,
    @SerializedName("stream_icon") val icono_transmision: String? = null,
    @SerializedName("epg_channel_id") val id_canal_epg: String? = null,
    @SerializedName("added") val agregadoEn: String? = null,
    @SerializedName("category_id") val id_categoria: String,
    @SerializedName("direct_source") val fuenteDirecta: String? = null
)

