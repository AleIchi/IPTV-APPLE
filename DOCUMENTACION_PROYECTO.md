# Documentación del Proyecto IPTV Fiber Z

Este documento describe el proyecto Android/Kotlin de IPTV Fiber Z de forma completa, detallada y concisa. Está organizado por arquitectura, flujo funcional y catálogo de clases/funciones para que sea fácil ubicar qué hace cada parte del código.

## 1. Resumen general

IPTV Fiber Z es una aplicación Android desarrollada en Kotlin con Jetpack Compose. Soporta interfaz móvil y Android TV, autenticación por Xtream Codes, carga de listas M3U, reproducción con Media3/ExoPlayer, favoritos, historial, preferencias persistentes con DataStore, base de datos local con Room y activación por QR para TV.

Capas principales:

- `datos`: API, modelos, repositorios, DataStore, Room, utilidades de red y M3U.
- `interfaz`: pantallas móviles, navegación, reproductor, tema, componentes y ViewModels.
- `tv`: pantallas, navegación y componentes optimizados para Android TV.
- `servicio`: servicio auxiliar para imagen en imagen.
- `res`: imágenes, temas, strings, configuración XML y recursos gráficos.

## 2. Flujo principal de la aplicación

1. `AplicacionIPTV` inicializa configuración global de Coil/OkHttp para carga de imágenes, incluyendo soporte SSL tolerante.
2. `ActividadPrincipal` o `ActividadTV` decide si usa experiencia móvil o TV y construye repositorios, base de datos y ViewModels.
3. `ModeloVistaAutenticacion` gestiona inicio de sesión Xtream, inicio por M3U, inicio automático y cierre de sesión.
4. `RepositorioAutenticacion` valida credenciales, normaliza URL, guarda sesión en `GestorPreferencias` y expone el servidor activo.
5. `ModeloVistaContenido` carga categorías, canales, EPG, favoritos e historial usando `RepositorioContenido`.
6. `RepositorioContenido` consulta Xtream o cache M3U, normaliza logos, mantiene cache de canales y usa DAOs para favoritos/historial.
7. Las pantallas móviles y TV consumen los estados de los ViewModels y permiten buscar, filtrar, reproducir, marcar favoritos y ajustar preferencias.
8. `ActividadReproductor`, `PantallaReproductor`, `GestorReproductorCompartido` y `MiniReproductorTV` administran reproducción de video con ExoPlayer.

## 3. Capa de datos

### `datos/api/ConfiguracionRed.kt`

- `ConfiguracionRed`: objeto de constantes de red. Centraliza valores compartidos de conectividad/API si el resto del proyecto necesita tiempos, URLs base o ajustes globales.

### `datos/api/ClienteApi.kt`

- `ClienteApi`: fábrica de clientes Retrofit/OkHttp para consumir Xtream Codes.
- `crearApi()`: crea una instancia de `ApiXtreamCodes` con la configuración base por defecto.
- `crearApiParaServidor(urlBase)`: crea una instancia de API apuntando al servidor recibido. Se usa cuando el usuario ingresa una URL concreta.

### `datos/api/ApiXtreamCodes.kt`

- `ApiXtreamCodes`: interfaz Retrofit con endpoints dinámicos de Xtream Codes.
- `autenticar(url, usuario, contrasena)`: llama `player_api.php` y devuelve el cuerpo crudo para validar credenciales y extraer `user_info`.
- `obtenerInfoUsuario(url, usuario, contrasena)`: obtiene información de cuenta modelada como `RespuestaApi<InfoUsuario>`.
- `obtenerCategoriasEnVivo(url, usuario, contrasena, accion)`: consulta categorías de canales en vivo con `get_live_categories`.
- `obtenerCanalesEnVivo(url, usuario, contrasena, accion, idCategoria)`: consulta canales en vivo, opcionalmente filtrados por categoría.
- `obtenerGuiaProgramacion(url, usuario, contrasena, accion, idTransmision)`: obtiene EPG corta para un canal o todos los canales disponibles.
- `obtenerUrlTransmision(url, usuario, contrasena, accion, tipo, id)`: endpoint auxiliar para resolver una URL de transmisión.

### `datos/modelo/RespuestaApi.kt`

- `RespuestaApi<T>`: contenedor genérico para respuestas Xtream con `user_info`, `server_info` u otros datos.
- `InfoUsuario`: datos de la cuenta autenticada: usuario, contraseña, mensaje, autenticación, estado, expiración y conexiones.
- `InfoServidor`: metadatos del servidor: URL, puerto, HTTPS, zona horaria y hora actual.
- `Categoria`: categoría de contenido en vivo; contiene id, nombre e id padre.
- `Canal`: modelo de canal en vivo; contiene id, nombre, logo, categoría, URL directa M3U y categorías adicionales.
- `EPG`: entrada de guía de programación con título, descripción, inicio, fin y metadatos.

### `datos/modelo/ConfiguracionServidor.kt`

- `ConfiguracionServidor`: representa un servidor configurado o una lista M3U activa. Incluye id, URL, usuario, contraseña, nombre, estado activo y fecha de creación.

### `datos/local/GestorPreferencias.kt`

- `Context.almacenDatos`: extensión que crea el DataStore `ajustes`.
- `GestorPreferencias`: administra preferencias persistentes.
- `establecerTema(nuevoTema)`: guarda el tema seleccionado.
- `establecerBloqueoCaptura(activo)`: activa/desactiva bloqueo de capturas.
- `establecerControlParental(activo)`: activa/desactiva control parental.
- `establecerPinParental(pin)`: guarda el PIN parental.
- `establecerIdServidorActivo(idServidor)`: guarda el servidor activo.
- `establecerCalidadVideo(calidad)`: guarda calidad preferida de reproducción.
- `guardarCredenciales(url, usuario, contrasena)`: persiste credenciales de acceso.
- `guardarDetallesCuenta(expiracion, maxCon, estado)`: guarda datos de vencimiento, conexiones máximas y estado.
- `limpiarCredenciales()`: elimina credenciales y detalles de cuenta.
- `obtenerOGenerarMacVirtual()`: devuelve una MAC virtual existente o genera una nueva para activación TV/QR.
- `guardarUltimaActualizacionQR(fecha)`: guarda el último `updateTime` procesado desde Firestore.
- `obtenerUltimaActualizacionQR()`: lee el último `updateTime` QR guardado.

### `datos/local/base_datos/BaseDatosIPTV.kt`

- `BaseDatosIPTV`: base de datos Room de la aplicación.
- `obtenerBaseDatos()`: devuelve una instancia singleton de Room para acceder a favoritos e historial.

### `datos/local/base_datos/DaoFavorito.kt`

- `DaoFavorito`: DAO Room para favoritos.
- `obtenerFavoritos(idServidor)`: observa todos los favoritos de un servidor.
- `obtenerFavoritoPorId(id)`: busca un favorito puntual.
- `obtenerFavoritosPorTipo(idServidor, tipo)`: observa favoritos filtrados por tipo.
- `insertarFavorito(favorito)`: inserta o reemplaza un favorito.
- `eliminarFavorito(favorito)`: elimina una entidad favorita.
- `eliminarFavoritoPorId(id)`: elimina favorito por identificador.
- `esFavorito(id)`: indica si un id ya existe como favorito.

### `datos/local/base_datos/DaoSeguirViendo.kt`

- `DaoSeguirViendo`: DAO Room para historial/seguir viendo.
- `obtenerHistorial(idServidor)`: observa historial por servidor.
- `obtenerHistorialPorId(id)`: busca una entrada concreta del historial.
- `insertarEnHistorial(entrada)`: inserta o actualiza una entrada de historial.
- `eliminarDeHistorial(entrada)`: elimina una entrada.
- `eliminarDeHistorialPorId(id)`: elimina una entrada por id.
- `recortarHistorial(idServidor, limite)`: conserva solo las entradas más recientes hasta el límite indicado.
- `limpiarHistorial(idServidor)`: borra todo el historial del servidor activo.

### `datos/local/base_datos/Favorito.kt`

- `Favorito`: entidad Room para un favorito. Guarda id compuesto, tipo, nombre, id de transmisión, logo, servidor y fecha.

### `datos/local/base_datos/SeguirViendo.kt`

- `SeguirViendo`: entidad Room para historial. Guarda canal/contenido, posición, duración, logo, servidor y fecha de acceso.

### `datos/repositorio/RepositorioServidor.kt`

- `RepositorioServidor`: repositorio simple en memoria para administrar servidores configurados.
- `agregarServidor(servidor)`: agrega un servidor y, si corresponde, lo marca como activo.
- `eliminarServidor(idServidor)`: elimina un servidor por id y ajusta el activo si era necesario.
- `establecerServidorActivo(idServidor)`: marca un servidor como activo y desactiva los demás.
- `obtenerServidorActivo()`: devuelve el servidor activo o `null`.
- `obtenerServidor(idServidor)`: busca un servidor específico.

### `datos/repositorio/RepositorioAutenticacion.kt`

- `RepositorioAutenticacion`: administra sesión Xtream/M3U y servidor actual.
- `esUsuarioListaM3U(usuario)`: identifica credenciales internas de modo M3U.
- `construirUrlTransmisionXtream(urlServidor, usuario, contrasena, tipo, id)`: arma URL reproducible para Xtream.
- `normalizarUrlBase(url)`: limpia espacios, agrega esquema HTTP/HTTPS y quita barra final.
- `autenticar(urlServidor, usuario, contrasena)`: valida credenciales Xtream, parsea `user_info`, guarda sesión y detalles de cuenta.
- `traducirError(mensaje)`: convierte errores técnicos o del servidor a mensajes claros en español.
- `descargarM3UConOkHttp(url)`: descarga una lista M3U con OkHttp estándar y reintenta con SSL tolerante si falla.
- `obtenerClienteOkHttpToleranteSSL()`: construye un cliente OkHttp que acepta certificados SSL no confiables.
- `checkClientTrusted(...)`: callback SSL tolerante para certificados de cliente.
- `checkServerTrusted(...)`: callback SSL tolerante para certificados de servidor.
- `getAcceptedIssuers()`: devuelve emisores aceptados por el trust manager tolerante.
- `autenticarM3U(urlLista)`: descarga y analiza lista M3U; guarda sesión como `LISTA_M3U`.
- `intentarAutoInicioSesion()`: recupera credenciales guardadas y restaura sesión, recargando M3U si aplica.
- `cerrarSesion()`: limpia estado de autenticación y borra credenciales.
- `establecerSesionActiva(urlServidor, usuario, contrasena, nombreServidor)`: restaura sesión cuando las credenciales llegan por `Intent`.
- `normalizarUrl(url)`: delega normalización de URL base.
- `construirUrlAutenticacion(urlBase)`: genera endpoint `player_api.php`.
- `generarIdServidor(url, usuario)`: crea id estable por URL/usuario.
- `construirUrlTransmision(urlServidor, usuario, contrasena, tipo, id)`: wrapper público para URL Xtream.

### `datos/repositorio/RepositorioContenido.kt`

- `RepositorioContenido`: administra categorías, canales, EPG, favoritos, historial y caches.
- `actualizarCache(canales)`: actualiza lista y mapa global de canales.
- `establecerDatosM3U(datos)`: guarda canales M3U agrupados por categoría y actualiza cache.
- `obtenerSiguienteCanal(idTransmisionActual)`: busca el siguiente canal usando contexto de reproducción o cache.
- `obtenerCanalAnterior(idTransmisionActual)`: busca el canal anterior.
- `establecerContextoReproduccion(canales)`: define la lista sobre la que se harán cambios anterior/siguiente.
- `obtenerCanalCompleto(idTransmision, respaldo)`: devuelve canal cacheado completo o el respaldo recibido.
- `establecerCategoriasCache(categorias)`: guarda categorías en memoria.
- `obtenerCanalesRecomendados(idTransmisionActual)`: devuelve canales de la misma categoría o contexto actual.
- `obtenerNombreCategoria(idCategoria)`: obtiene nombre visible de una categoría.
- `obtenerConteoCanalesCategoria(idCategoria)`: cuenta canales cacheados de una categoría.
- `limpiarCache()`: borra canales, mapa, contexto, M3U y categorías cacheadas.
- `obtenerServidor()`: lee servidor actual desde el repositorio de autenticación.
- `construirUrlApi(servidor)`: genera endpoint `player_api.php` del servidor.
- `procesarLogotipos(canales)`: normaliza URLs de logos de canales.
- `normalizarUrlImagen(url, urlPublica)`: convierte rutas relativas y reemplaza IPs privadas por URL pública.
- `parsearJsonParcial<T>(jsonTexto)`: parsea arrays JSON completos o parcialmente malformados, omitiendo objetos inválidos.
- `obtenerCategoriasEnVivo()`: carga categorías desde M3U o Xtream.
- `obtenerCanalesEnVivo(idCategoria)`: carga canales desde M3U o Xtream y filtra por categoría si se indica.
- `obtenerGuiaProgramacion(idTransmision)`: expone EPG como `Flow<Result<Map<String, List<EPG>>>>`.
- `obtenerFavoritos(tipo)`: observa favoritos del servidor activo.
- `esFavorito(id)`: consulta si un canal está marcado.
- `alternarFavorito(canal)`: inserta o elimina favorito y devuelve el nuevo estado.
- `obtenerHistorial()`: observa historial del servidor activo.
- `agregarAlHistorial(canal)`: registra canal reproducido y recorta historial.
- `limpiarHistorial()`: borra historial del servidor activo.

### `datos/utilidades/AnalizadorM3U.kt`

- `AnalizadorM3U`: parser de listas M3U.
- `procesar(contenidoM3U)`: convierte texto M3U en `Map<String, List<Canal>>`, agrupando por categoría.
- `extraerAtributos(linea)`: extrae atributos `tvg-*`, `group-title`, logo y otros metadatos de una línea `#EXTINF`.

### `datos/utilidades/MonitorRed.kt`

- `MonitorRed`: observa estado de conectividad del dispositivo.
- `onAvailable(network)`: marca conexión disponible cuando Android reporta red activa.
- `onLost(network)`: marca desconexión cuando se pierde la red.

## 4. ViewModels

### `interfaz/modelovista/ModeloVistaAutenticacion.kt`

- `ModeloVistaAutenticacion`: coordina autenticación desde la UI.
- `iniciarSesion(urlServidor, usuario, contrasena)`: ejecuta login Xtream y actualiza estado.
- `iniciarSesionConIdDispositivo(urlServidor, idDispositivo)`: inicia sesión usando identificador de dispositivo cuando aplique.
- `iniciarSesionConM3U(urlLista)`: inicia sesión cargando una playlist M3U.
- `cerrarSesion()`: cierra sesión en repositorio y reinicia estado.
- `limpiarError()`: vuelve de error a estado inactivo.
- `EstadoAutenticacion`: estados `Inactivo`, `Cargando`, `Exito(infoUsuario)` y `Error(mensaje)`.

### `interfaz/modelovista/ModeloVistaContenido.kt`

- `ModeloVistaContenido`: estado central de categorías, canales, búsqueda, favoritos, historial y panel principal.
- `cargarCategoriasEnVivo()`: solicita categorías y actualiza estado.
- `cargarCanalesEnVivo(idCategoria)`: solicita canales, actualiza cache, filtros y panel.
- `buscar(consulta)`: actualiza texto de búsqueda y recalcula filtrado.
- `seleccionarCategoria(id)`: cambia categoría seleccionada y recalcula lista visible.
- `actualizarFiltrado()`: combina búsqueda y categoría para producir canales filtrados.
- `generarDatosPanelPrincipal()`: crea secciones resumidas para inicio/dashboard.
- `cargarEPG(idTransmision)`: carga guía de programación.
- `cargarFavoritos(tipo)`: observa favoritos y actualiza estado.
- `alternarFavorito(canal)`: marca o desmarca un canal.
- `cargarHistorial()`: observa historial.
- `agregarAlHistorial(canal)`: registra canal visto.
- `iniciarObservacionDatosUsuario()`: observa datos de cuenta guardados.
- `limpiarError()`: limpia mensaje de error del contenido.
- `establecerContextoReproduccion(canales)`: define lista usada por anterior/siguiente.
- `obtenerCanalCompleto(idTransmision, canalBase)`: recupera versión completa desde cache.
- `limpiarHistorial()`: borra historial.
- `limpiarCache()`: reinicia cache global del repositorio.

## 5. Interfaz móvil

### `interfaz/principal/ActividadPrincipal.kt`

- `ActividadPrincipal`: entrada principal móvil.
- `onCreate(estadoGuardado)`: inicializa tema, repositorios, base de datos, ViewModels y contenido Compose.

### `interfaz/principal/NavegacionPrincipal.kt`

- `PantallaPrincipal(...)`: decide entre login y navegación autenticada; coordina pantallas principales móviles.

### `interfaz/principal/InicioPanelPrincipal.kt`

- `InicioPanelPrincipal(...)`: pantalla inicial tipo panel con resumen de contenido.
- `ContenidoInicioPanelPrincipal(...)`: renderiza secciones, canales destacados y accesos principales.
- `CabeceraSeccion(titulo, coda)`: cabecera reutilizable para secciones del panel.
- `construirLambdaReproduccionCanal(...)`: crea callback que obtiene URL, guarda historial y abre el reproductor.

### `interfaz/principal/PantallaTvEnVivo.kt`

- `PantallaTvEnVivo(...)`: pantalla móvil de canales en vivo con categorías, búsqueda y lista.
- `CategoriasDesplazables(...)`: chips horizontales para seleccionar categoría.

### `interfaz/principal/PantallasSecundarias.kt`

- `PantallaFavoritos(modeloVista, repositorioAuth)`: lista favoritos y permite reproducirlos.
- `PantallaHistorial(modeloVista, repositorioAuth)`: lista historial y permite reproducir entradas.
- `construirLambdaReproduccion(...)`: callback común para abrir reproductor desde favoritos/historial.
- `PantallaListaCanales(...)`: lista genérica de canales reutilizable.

### `interfaz/inicio_sesion/PantallaInicioSesion.kt`

- `PantallaInicioSesion(...)`: formulario móvil para Xtream o M3U.
- `validarYEntrar()`: valida campos visibles y dispara autenticación.
- `CampoTextoPremium(...)`: campo de texto estilizado para login.

### `interfaz/ajustes/PantallaAjustes.kt`

- `PantallaAjustes(...)`: pantalla móvil de preferencias, cuenta, control parental y sesión.

### `interfaz/ajustes/ComponentesAjustesMovil.kt`

- `FilaAjuste(...)`: fila reutilizable de ajuste con icono, texto y acción/control.
- `EncabezadoSeccion(titulo, icono, colorIcono)`: título visual de sección de ajustes.
- `DialogoPin(...)`: diálogo móvil para introducir o configurar PIN parental.

### `interfaz/componentes/ComponentesComunes.kt`

- `IconoTvPorDefecto(modificador)`: placeholder visual cuando un canal no tiene logo.
- `calcularProgresoEpg(epg)`: calcula progreso 0..1 del programa según inicio y fin.

### `interfaz/componentes/ComponentesPanel.kt`

- `TarjetaPanelPrincipal(...)`: tarjeta del dashboard móvil para resúmenes o accesos.
- `BarraLateralModerna(...)`: navegación lateral móvil/tablet.
- `CabeceraPanelPrincipal(...)`: cabecera del panel con saludo, cuenta o acciones.

### `interfaz/componentes/TarjetaCanal.kt`

- `TarjetaCanal(...)`: tarjeta básica de canal para listas móviles.

### `interfaz/componentes/TarjetaCanalPrincipal.kt`

- `TarjetaCanalPrincipal(...)`: tarjeta destacada de canal para panel/inicio.
- `TarjetaCanalEstandar(...)`: variante estándar para listas o grids.

### `interfaz/tema/GestorTema.kt`

- `TemaApp`: enum de temas disponibles.
- `obtenerEsquemaColorTema(tema)`: devuelve `ColorScheme` según tema.
- `TemaIPTVFiberBase(...)`: aplica tema Material base.

### `interfaz/tema/Tema.kt`

- `TemaIPTVFiber(...)`: tema principal de la app; aplica colores, tipografía y advertencia de red.
- `AdvertenciaConexionRed(estaConectado)`: banner/aviso cuando no hay conexión.

### `interfaz/tema/Color.kt`

- Contiene constantes de color y degradados compartidos: tonos premium, estados de carga, colores por sección y paletas alternativas.

### `interfaz/tema/Tipografia.kt`

- Define tipografía Material usada por Compose.

## 6. Reproductor

### `interfaz/reproductor/ActividadReproductor.kt`

- `ActividadReproductor`: actividad de reproducción móvil/TV con ExoPlayer, PiP y cambio de canal.
- `onCreate(savedInstanceState)`: lee datos del canal, crea reproductor, repositorios y UI Compose.
- `inicializarExo()`: prepara instancia ExoPlayer inicial.
- `liberarExo()`: libera instancia ExoPlayer.
- `desconectarExo()`: separa el player de la vista sin liberar necesariamente todos los datos.
- `onStart()`: reanuda o inicializa recursos al entrar.
- `onResume()`: recupera reproducción al volver a primer plano.
- `onUserLeaveHint()`: intenta entrar en Picture-in-Picture si está habilitado.
- `onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)`: responde a cambios PiP modernos.
- `onPause()`: pausa o conserva reproducción según PiP/estado.
- `onStop()`: libera o pausa recursos al salir.
- `onDestroy()`: limpieza final del reproductor.
- `inicializarReproductor()`: crea/prepara reproductor con URL actual.
- `liberarReproductor()`: libera player y recursos asociados.
- `cargarSiguienteCanal()`: cambia al siguiente canal del contexto.
- `cargarCanalAnterior()`: cambia al canal anterior del contexto.
- `cambiarCanal(canal)`: actualiza URL, metadatos, historial e instancia de reproducción.
- `abrirEnReproductorExt()`: lanza un intent para reproducir con una app externa.
- `onNewIntent(intent)`: permite cambiar canal con nuevos datos enviados a la actividad.
- `onPictureInPictureModeChanged(isInPictureInPictureMode)`: compatibilidad con firma PiP anterior.

### `interfaz/reproductor/PantallaReproductor.kt`

- `PantallaReproductor(...)`: UI Compose del reproductor; muestra video, controles, lista lateral, favoritos y ajustes.
- `onPlaybackStateChanged(estado)`: listener interno para estado de buffer/listo/error.
- `onIsPlayingChanged(reproduciendo)`: actualiza UI según reproducción activa.
- `onRenderedFirstFrame()`: marca que el primer frame ya fue renderizado.

### `interfaz/reproductor/ControlesReproductor.kt`

- `SuperposicionControlesReproductor(...)`: controles flotantes de reproducción, información de canal y acciones.

### `interfaz/reproductor/HojaAjustesReproductor.kt`

- `HojaAjustesReproductor(...)`: bottom sheet con escalado, velocidad, reporte por WhatsApp y opciones del player.

### `interfaz/reproductor/ComponentesReproductor.kt`

- `ItemCanalListaReproductor(...)`: fila/tarjeta de canal dentro de listas del reproductor.

### `interfaz/reproductor/ClavesReproductor.kt`

- `ClavesReproductor`: objeto con constantes de extras para intents del reproductor: URL, nombre, logo, categoría, credenciales y metadatos.

## 7. Interfaz Android TV

### `tv/ActividadTV.kt`

- `ActividadTV`: entrada principal para Android TV.
- `onCreate(savedInstanceState)`: configura repositorios, tema TV, ViewModels y navegación TV.
- `onDestroy()`: libera recursos TV al cerrar.

### `tv/NavegacionTV.kt`

- `NavegacionTV(...)`: navegación principal TV entre inicio, en vivo, favoritos, historial, ajustes, login y activación QR.

### `tv/pantallas/PantallaInicioSesionTV.kt`

- `PantallaInicioSesionTV(...)`: formulario TV para login Xtream/M3U, adaptado a control remoto y teclado.
- `actualizarCampoEditando(campo)`: registra qué campo está en edición.
- `ocultarTeclado()`: oculta teclado virtual y limpia foco de edición.
- `activarEdicion(...)`: enfoca un campo y muestra teclado.
- `finalizarEdicionYSaltar(...)`: finaliza edición actual y mueve el foco al siguiente campo/acción.

### `tv/pantallas/PantallaActivacionQR.kt`

- `PantallaActivacionQR(...)`: pantalla TV que muestra QR con MAC virtual y consulta Firestore para activar dispositivo.
- `PasoInstruccion(numero, texto)`: fila visual de instrucciones numeradas.
- `generarCodigoQR(contenido, ancho, alto)`: genera bitmap QR con ZXing.

### `tv/pantallas/InicioTV.kt`

- `InfoBannerTV`: modelo local para banners promocionales.
- `InicioTV(...)`: inicio TV con banners, destacados, estadísticas, favoritos y accesos rápidos.

### `tv/pantallas/TvEnVivoTV.kt`

- `TvEnVivoTV(...)`: pantalla TV de canales en vivo con panel de canales, mini reproductor y categorías.

### `tv/pantallas/FavoritosTV.kt`

- `FavoritosTV(...)`: pantalla TV de favoritos; permite reproducir y quitar favoritos.

### `tv/pantallas/HistorialTV.kt`

- `HistorialTV(...)`: pantalla TV de historial; permite reabrir canales vistos y alternar favoritos.

### `tv/pantallas/AjustesTV.kt`

- `AjustesTV(...)`: pantalla TV de ajustes, cuenta, control parental, PiP, sesión y datos de suscripción.

### `tv/ajustes/ComponentesAjustesTV.kt`

- `ItemAccionTV(...)`: fila de acción enfocables para TV.
- `CodigoQRPremium(...)`: representación visual de QR/placeholder premium.
- `SeccionAjustesTV(titulo, icono, contenido)`: bloque de ajustes con título e icono.
- `ItemSwitchTV(...)`: fila de ajuste binario con switch.
- `GuiaControlTV(...)`: guía visual de controles remotos.
- `FilaInfoSuscripcion(...)`: fila con dato de cuenta/suscripción.

### `tv/componentes/ComponentesBaseTV.kt`

- `FondoPantallaTV(...)`: fondo común para pantallas TV.
- `EncabezadoPantallaTV(...)`: cabecera con título, subtítulo y acciones.
- `PanelTV(...)`: contenedor visual de sección TV.
- `EstadoVacioTV(...)`: estado vacío con icono, título, mensaje y acción opcional.
- `Modifier.tvClickableWithLongClick(...)`: modificador para click y long click compatible con foco TV.

### `tv/componentes/ComponentesInicioTV.kt`

- `SeccionTituloTV(titulo, paddingSuperior)`: título de sección en inicio TV.
- `ImagenPromocionalTV(...)`: banner/imagen promocional con foco y acción.
- `EstadisticaRapida(valor, etiqueta)`: métrica compacta para inicio TV.

### `tv/componentes/ComponentesInicioSesionTV.kt`

- `recordarTecladoVisibleTV()`: estado Compose que detecta si el teclado está visible.
- `CampoInicioSesionTV`: enum de campos editables del login TV.
- `CampoTextoPremiumTV(...)`: campo de texto premium enfocable para TV.
- `cerrarEdicion()`: helper local para salir del modo edición.
- `finalizarEdicion()`: helper local para confirmar texto y cerrar edición.

### `tv/componentes/ComponentesTvEnVivoTV.kt`

- `EncabezadoTvEnVivo(...)`: cabecera para pantalla de TV en vivo.
- `ChipCategoriaTV(...)`: chip enfocable de categoría.
- `EstadoPantallaTV(...)`: estado de carga/error/vacío para TV en vivo.
- `FilaCanalTV(...)`: fila de canal con logo, nombre, EPG y favorito.

### `tv/componentes/BarraInfoCanalTV.kt`

- `BarraInfoCanalTV(...)`: barra inferior/superpuesta con información del canal y acciones de reproducción.
- `BotonIconoTV(...)`: botón circular enfocable con icono.
- `InfoTransmisionTV(reproductorExo)`: muestra resolución, audio y estado técnico del stream.
- `FilaInfoTransmision(etiqueta, valor)`: fila interna para datos técnicos.

### `tv/componentes/PanelCanalesTV.kt`

- `PanelCanalesTV(...)`: panel lateral/lista de canales para TV.
- `ItemCanalTV(...)`: item interno de canal con foco y selección.

### `tv/componentes/MiniReproductorTV.kt`

- `MiniReproductorTV(...)`: reproductor compacto en pantallas TV.
- `onPlaybackStateChanged(playbackState)`: listener interno para carga/listo/error.
- `onRenderedFirstFrame()`: listener interno para saber cuándo se ve video.

### `tv/componentes/GestorReproductorCompartido.kt`

- `GestorReproductorCompartido`: singleton para compartir ExoPlayer entre mini reproductor y pantalla completa TV.
- `obtenerOInicializar(contexto, url)`: devuelve player existente o crea uno para la URL.
- `checkClientTrusted(...)`: callback SSL tolerante del player compartido.
- `checkServerTrusted(...)`: callback SSL tolerante del player compartido.
- `getAcceptedIssuers()`: emisores aceptados del trust manager.
- `onPlaybackStateChanged(playbackState)`: listener para reintentos y estados.
- `onPlayerError(error)`: maneja errores del player compartido.
- `obtenerSiEsMismaUrl(url)`: devuelve player si coincide con la URL actual.
- `tomarPosesion()`: transfiere el player para que otra pantalla lo use.
- `liberar()`: libera player compartido y limpia estado.

### `tv/componentes/MenuLateralTV.kt`

- `OpcionMenuTV`: modelo de opción del menú lateral.
- `MenuLateralTV(...)`: menú de navegación lateral para TV.
- `ItemMenuTV(...)`: opción enfocable del menú.

### `tv/componentes/TarjetaTV.kt`

- `TarjetaTV(...)`: tarjeta genérica para contenido TV.
- `PlaceholderCanalTV(nombre, modifier)`: placeholder con iniciales/nombre.
- `TarjetaCanalGrandeTV(...)`: tarjeta destacada grande.
- `TarjetaCanalMedianaTV(...)`: tarjeta mediana para grids.
- `BadgeEnVivoTV(...)`: indicador visual de contenido en vivo.

### `tv/componentes/DialogoConfirmacionTV.kt`

- `DialogoConfirmacionTV(...)`: diálogo de confirmación enfocable para TV.
- `BotonDialogoTV(...)`: botón interno del diálogo.

### `tv/componentes/ControlParentalTV.kt`

- `esContenidoAdultoTV(...)`: detecta contenido adulto por nombre/categoría usando palabras clave.

### `tv/componentes/TemaTV.kt`

- `TemaTV`: objeto con colores, dimensiones o constantes visuales específicas de TV.

### `tv/dialogos/DialogoPinTV.kt`

- `DialogoPinTV(...)`: diálogo TV para pedir PIN parental.
- `TeclaPinTV(...)`: tecla numérica/acción del teclado PIN.

### `tv/dialogos/DialogoLegalTV.kt`

- `DialogoLegalTV(...)`: diálogo con términos, legalidad o aviso de responsabilidad.

## 8. Servicios y aplicación

### `AplicacionIPTV.kt`

- `AplicacionIPTV`: clase `Application` global.
- `onCreate()`: configura `ImageLoader` de Coil con OkHttp personalizado para imágenes remotas.
- `checkClientTrusted(...)`: acepta certificados de cliente en el trust manager global.
- `checkServerTrusted(...)`: acepta certificados de servidor en el trust manager global.
- `getAcceptedIssuers()`: devuelve emisores aceptados por el trust manager global.

### `servicio/ServicioImagenEnImagen.kt`

- `ServicioImagenEnImagen`: servicio Android auxiliar relacionado con PiP.
- `onBind(intent)`: devuelve `null` porque el servicio no expone binding.

## 9. Recursos XML y gráficos

- `AndroidManifest.xml`: declara aplicación, actividades, permisos, configuración TV/móvil, PiP y proveedores si aplica.
- `res/xml/network_security_config.xml`: configuración de seguridad de red.
- `res/xml/file_paths.xml`: rutas compartibles mediante `FileProvider`.
- `res/xml/backup_rules.xml` y `data_extraction_rules.xml`: reglas de backup/extracción.
- `res/values/colors.xml`: colores XML base.
- `res/values/themes.xml` y `values-night/themes.xml`: temas Android.
- `res/values/strings.xml`: textos globales de la app.
- `res/drawable`: logos, promociones e imágenes usadas en UI.
- `res/mipmap-*`: iconos launcher.

## 10. Flujo de autenticación

### Xtream Codes

1. UI llama a `ModeloVistaAutenticacion.iniciarSesion`.
2. ViewModel llama a `RepositorioAutenticacion.autenticar`.
3. Repositorio normaliza URL, llama `ApiXtreamCodes.autenticar`, parsea respuesta y valida `auth == 1`.
4. Si es correcto, guarda credenciales y detalles de cuenta en `GestorPreferencias`.
5. `servidorActual` y `estaAutenticado` se actualizan como `StateFlow`.

### M3U

1. UI llama a `iniciarSesionConM3U`.
2. Repositorio descarga lista con `descargarM3UConOkHttp`.
3. `AnalizadorM3U.procesar` genera categorías y canales.
4. `RepositorioContenido.establecerDatosM3U` guarda cache global.
5. La sesión se marca como `LISTA_M3U`.

### Activación QR TV

1. `PantallaActivacionQR` obtiene/genera MAC virtual.
2. Genera un QR con URL web incluyendo la MAC.
3. Consulta Firestore buscando credenciales asociadas a esa MAC.
4. Cuando detecta cambios nuevos, extrae modo, URL, usuario y contraseña.
5. Llama al flujo de autenticación correspondiente y borra/procesa la solicitud.

## 11. Flujo de contenido

1. `ModeloVistaContenido.cargarCategoriasEnVivo` carga categorías.
2. `cargarCanalesEnVivo` carga canales desde Xtream o M3U.
3. `RepositorioContenido` actualiza cache global de canales.
4. `buscar` y `seleccionarCategoria` filtran la lista visible.
5. Al reproducir, se construye la URL con `RepositorioAutenticacion.construirUrlTransmision`.
6. Se abre `ActividadReproductor` con extras definidos en `ClavesReproductor`.
7. El canal se registra con `agregarAlHistorial`.

## 12. Flujo de favoritos e historial

- Favoritos usan `Favorito` + `DaoFavorito`.
- Historial usa `SeguirViendo` + `DaoSeguirViendo`.
- Ambos se separan por `idServidor`, así cada servidor/lista mantiene datos independientes.
- `alternarFavorito` crea ids compuestos: `servidor_canal_id`.
- `agregarAlHistorial` registra el canal y llama `recortarHistorial` para limitar a 50 entradas.

## 13. Flujo de reproducción

- La app reproduce con Media3/ExoPlayer.
- En móvil, `ActividadReproductor` controla ciclo de vida, PiP, cambio de canal y player.
- En TV, `GestorReproductorCompartido` permite reutilizar el mismo player entre mini reproductor y pantalla completa.
- `RepositorioContenido` mantiene `contextoReproduccion` para navegar canal anterior/siguiente.
- Las URLs Xtream se forman como:

```text
{urlServidor}/{tipo}/{usuario}/{contrasena}/{id}.ts
```

Para `live`, se agrega `.ts`; para otros tipos se deja el id sin extensión.

## 14. Notas de mantenimiento

- La app mezcla soporte Xtream y M3U. Antes de cambiar contenido, revisar siempre si el flujo usa servidor real o `LISTA_M3U`.
- Hay caches globales en `RepositorioContenido.companion object`; si se agregan nuevos flujos de sesión conviene llamar `limpiarCache`.
- La normalización de logos corrige IPs privadas. Es importante mantenerla si algunos proveedores devuelven logos con `192.168.*`, `10.*` o `172.16-31.*`.
- Los clientes SSL tolerantes ayudan con proveedores IPTV con certificados defectuosos, pero implican menos seguridad. Si se endurece seguridad, revisar `AplicacionIPTV`, `RepositorioAutenticacion` y `GestorReproductorCompartido`.
- La experiencia TV depende mucho del foco. Al modificar componentes TV, verificar navegación con control remoto/d-pad.
- El árbol de trabajo actual contiene cambios no confirmados; esta documentación fue agregada como archivo independiente para no alterar lógica existente.

