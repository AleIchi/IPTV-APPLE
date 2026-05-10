package com.iptv.fiber.datos.api

import com.iptv.fiber.datos.modelo.RespuestaApi
import com.iptv.fiber.datos.modelo.Categoria
import com.iptv.fiber.datos.modelo.Canal
import com.iptv.fiber.datos.modelo.EPG
import com.iptv.fiber.datos.modelo.Pelicula
import com.iptv.fiber.datos.modelo.Radio
import com.iptv.fiber.datos.modelo.Serie
import com.iptv.fiber.datos.modelo.InfoUsuario
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/** Interfaz que define todas las llamadas a la API de Xtream Codes. */
interface ApiXtreamCodes {

    @GET
    suspend fun autenticar(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String
    ): Response<ResponseBody>

    @GET
    suspend fun obtenerInfoUsuario(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String
    ): Response<RespuestaApi<InfoUsuario>>

    @GET
    suspend fun obtenerCategoriasEnVivo(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_live_categories"
    ): Response<okhttp3.ResponseBody>

    @GET
    suspend fun obtenerCanalesEnVivo(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_live_streams",
        @Query("category_id") idCategoria: String? = null
    ): Response<okhttp3.ResponseBody>

    @GET
    suspend fun obtenerCategoriasPeliculas(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_vod_categories"
    ): Response<List<Categoria>>

    @GET
    suspend fun obtenerPeliculas(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_vod_streams",
        @Query("category_id") idCategoria: String? = null
    ): Response<List<Pelicula>>

    @GET
    suspend fun obtenerCategoriasSeries(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_series_categories"
    ): Response<List<Categoria>>

    @GET
    suspend fun obtenerSeries(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_series",
        @Query("category_id") idCategoria: String? = null
    ): Response<List<Serie>>

    @GET
    suspend fun obtenerInfoSerie(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_series_info",
        @Query("series_id") idSerie: Int
    ): Response<Serie>

    @GET
    suspend fun obtenerCategoriasRadio(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_live_categories"
    ): Response<List<Categoria>>

    @GET
    suspend fun obtenerCanalesRadio(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_live_streams",
        @Query("category_id") idCategoria: String? = null
    ): Response<List<Radio>>

    @GET
    suspend fun obtenerGuiaProgramacion(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_short_epg",
        @Query("stream_id") idStream: Int? = null
    ): Response<Map<String, List<EPG>>>

    @GET
    suspend fun obtenerUrlStream(
        @Url url: String,
        @Query("username") usuario: String,
        @Query("password") contrasena: String,
        @Query("action") accion: String = "get_short_epg",
        @Query("type") tipo: String,
        @Query("id") id: Int
    ): Response<String>
}
