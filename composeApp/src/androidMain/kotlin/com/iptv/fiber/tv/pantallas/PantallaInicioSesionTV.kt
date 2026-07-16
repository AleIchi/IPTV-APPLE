package com.iptv.fiber.tv.pantallas

// removed android import: import android.content.Context
import android.graphics.Rect
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.tv.componentes.PanelTV
import com.iptv.fiber.tv.componentes.TemaTV
import com.iptv.fiber.tv.componentes.recordarTecladoVisibleTV
import com.iptv.fiber.tv.componentes.CampoInicioSesionTV
import com.iptv.fiber.tv.componentes.CampoTextoPremiumTV
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 
 * EL LOGIN PARA TELEVISORES (Smart TV).
 * A diferencia del celular donde tocas con el dedo, en la TV usas un control remoto (arriba, abajo, OK).
 * Esta pantalla tiene mucha lógica extra para asegurar que el "foco" (el elemento seleccionado)
 * se mueva correctamente entre los botones y las cajas de texto sin quedarse atrapado.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun PantallaInicioSesionTV(
    modeloVista: ModeloVistaAutenticacion,
    alCambiarAPantallaQR: () -> Unit,
    alIniciarSesionExitosamente: () -> Unit
) {
    var urlServidor by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var esModoM3U by remember { mutableStateOf(false) }
    var campoConFoco by remember { mutableStateOf<CampoInicioSesionTV?>(null) }
    var campoEditando by remember { mutableStateOf<CampoInicioSesionTV?>(null) }
    var tecladoFueVisibleDuranteEdicion by remember { mutableStateOf(false) }
    var saltoDeCampoEnProgreso by remember { mutableStateOf(false) }
    val estadoInterfaz by modeloVista.estadoInterfaz.collectAsStateWithLifecycle()
    val estadoDesplazamiento = rememberScrollState()
    val controladorTeclado = LocalSoftwareKeyboardController.current
    val vistaActual = LocalView.current
    val gestorMetodoEntrada = remember(vistaActual) {
        vistaActual.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val tecladoVisible = recordarTecladoVisibleTV()
    val requeridorFocoUrl = remember { FocusRequester() }
    val requeridorFocoUsuario = remember { FocusRequester() }
    val requeridorFocoContrasena = remember { FocusRequester() }
    val requeridorFocoBotonIniciar = remember { FocusRequester() }
    val alcance = rememberCoroutineScope()
    val campoParaDesplazamiento = campoEditando
    val desplazamientoFormulario = when (campoParaDesplazamiento) {
        CampoInicioSesionTV.Url -> (-45).dp
        CampoInicioSesionTV.Usuario -> (-105).dp
        CampoInicioSesionTV.Contrasena -> (-180).dp
        null -> 0.dp
    }

    /** Actualiza el campo en edición y resetea las banderas de teclado y salto. */
    fun actualizarCampoEditando(campo: CampoInicioSesionTV?) {
        campoEditando = campo
        tecladoFueVisibleDuranteEdicion = false
        saltoDeCampoEnProgreso = false
    }

    /** Oculta el teclado software tanto por InputMethodManager como por el controlador de Compose. */
    fun ocultarTeclado() {
        controladorTeclado?.hide()
        gestorMetodoEntrada.hideSoftInputFromWindow(vistaActual.windowToken, 0)
    }

    /** 
     * EL INVOCADOR DEL TECLADO:
     * En Android TV, a veces el teclado en pantalla no quiere aparecer cuando haces clic en una caja de texto.
     * Esta función fuerza agresivamente al sistema operativo (con varios "delays") a mostrar el teclado.
     */
    fun activarEdicion(
        campo: CampoInicioSesionTV,
        requeridorFoco: FocusRequester,
        requeridorTraerAVista: BringIntoViewRequester? = null
    ) {
        saltoDeCampoEnProgreso = true
        campoConFoco = campo
        campoEditando = campo
        tecladoFueVisibleDuranteEdicion = false
        alcance.launch {
            delay(20)
            runCatching { requeridorFoco.requestFocus() }
            gestorMetodoEntrada.showSoftInput(vistaActual, InputMethodManager.SHOW_IMPLICIT)
            requeridorTraerAVista?.bringIntoView()
            delay(30)
            saltoDeCampoEnProgreso = false
        }
    }

    /** Cierra la edición del campo actual y salta al [siguienteCampo], o libera el foco si es el último. */
    fun finalizarEdicionYSaltar(
        siguienteRequeridorFoco: FocusRequester?,
        siguienteCampo: CampoInicioSesionTV?
    ) {
        if (siguienteCampo == null) {
            campoConFoco = null
            actualizarCampoEditando(null)
            ocultarTeclado()
        } else {
            saltoDeCampoEnProgreso = true
            if (siguienteRequeridorFoco != null) {
                activarEdicion(siguienteCampo, siguienteRequeridorFoco)
            }
        }
        if (siguienteRequeridorFoco != null && siguienteCampo == null) {
            alcance.launch {
                delay(40)
                runCatching { siguienteRequeridorFoco.requestFocus() }
            }
        }
        if (siguienteCampo != null) {
            alcance.launch {
                delay(120)
                saltoDeCampoEnProgreso = false
            }
        }
    }

    LaunchedEffect(estadoInterfaz) {
        when (estadoInterfaz) {
            is ModeloVistaAutenticacion.EstadoAutenticacion.Exito -> alIniciarSesionExitosamente()
            is ModeloVistaAutenticacion.EstadoAutenticacion.Error -> {
                delay(1_500)
                modeloVista.limpiarError()
            }
            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        delay(50)
        runCatching { requeridorFocoUrl.requestFocus() }
        ocultarTeclado()
    }

    LaunchedEffect(tecladoVisible, campoEditando, tecladoFueVisibleDuranteEdicion, saltoDeCampoEnProgreso) {
        if (saltoDeCampoEnProgreso) return@LaunchedEffect
        when {
            campoEditando == null -> tecladoFueVisibleDuranteEdicion = false
            tecladoVisible -> tecladoFueVisibleDuranteEdicion = true
            tecladoFueVisibleDuranteEdicion -> {
                campoEditando = null
                tecladoFueVisibleDuranteEdicion = false
            }
        }
    }

    BackHandler(enabled = campoEditando != null) {
        actualizarCampoEditando(null)
        ocultarTeclado()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TemaTV.FondoPrincipal)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(TemaTV.Acento.copy(alpha = 0.18f), Color.Transparent),
                        radius = 980f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    Image(
                        painter = painterResource(id = com.iptv.fiber.R.drawable.logotipo_fiber_z),
                        contentDescription = "Fiber Z TV+",
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .wrapContentHeight(),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(2.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        TemaTV.Acento,
                                        TemaTV.AcentoClaro,
                                        TemaTV.Acento,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Disfruta de la mejor televisión en vivo, tus canales favoritos y transmisiones estables en alta definición.",
                        color = TemaTV.TextoSecundario,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.95f)
                    )
                }
            PanelTV(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxWidth()
                    .offset(y = desplazamientoFormulario)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(estadoDesplazamiento),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Acceso a la cuenta",
                        color = TemaTV.TextoPrincipal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ingresa los datos de tu servicio para continuar.",
                        color = TemaTV.TextoSecundario,
                        fontSize = TemaTV.Subtitulo
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(TemaTV.Superficie.copy(alpha = 0.78f), RoundedCornerShape(TemaTV.RedondeoControl))
                            .padding(4.dp)
                    ) {
                        listOf(false to "Xtream Code", true to "Lista M3U").forEach { (modo, label) ->
                            val interactionSourceModo = remember { MutableInteractionSource() }
                            val modoEnFoco by interactionSourceModo.collectIsFocusedAsState()
                            val escalaModo by animateFloatAsState(targetValue = if (modoEnFoco) 1.05f else 1f)

                            val colorFondo = when {
                                modoEnFoco -> TemaTV.AcentoClaro
                                esModoM3U == modo -> TemaTV.Acento
                                else -> Color.Transparent
                            }
                            val colorTexto = when {
                                modoEnFoco || esModoM3U == modo -> TemaTV.TextoPrincipal
                                else -> TemaTV.TextoSecundario
                            }

                            Button(
                                onClick = { esModoM3U = modo },
                                interactionSource = interactionSourceModo,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = escalaModo
                                        scaleY = escalaModo
                                    }
                                    .border(
                                        width = if (modoEnFoco) 1.5.dp else 0.dp,
                                        color = if (modoEnFoco) TemaTV.AcentoClaro.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorFondo,
                                    contentColor = colorTexto
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Error) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TemaTV.Peligro.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = TemaTV.Peligro, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = (estadoInterfaz as ModeloVistaAutenticacion.EstadoAutenticacion.Error).mensaje,
                                color = TemaTV.Peligro,
                                fontSize = 12.sp
                            )
                        }
                    }

                    CampoTextoPremiumTV(
                        valor = urlServidor,
                        alCambiar = { urlServidor = it },
                        etiqueta = if (esModoM3U) "URL de la lista" else "URL del servidor",
                        marcadorPosicion = if (esModoM3U) "http://ejemplo.com/lista.m3u" else "http://servidor.com:8080",
                        campo = CampoInicioSesionTV.Url,
                        campoEditando = campoEditando,
                        alEditarCampo = { actualizarCampoEditando(it) },
                        alCambiarFoco = { campo, enfocado ->
                            campoConFoco = if (enfocado) campo else campoConFoco.takeUnless { it == campo }
                        },
                        ignorarPerdidaFoco = saltoDeCampoEnProgreso,
                        requeridorFoco = requeridorFocoUrl,
                        siguienteRequeridorFoco = if (esModoM3U) requeridorFocoBotonIniciar else requeridorFocoUsuario,
                        siguienteCampo = if (esModoM3U) null else CampoInicioSesionTV.Usuario,
                        alActivarEdicion = { campo, requeridorFoco, bringIntoViewRequester ->
                            activarEdicion(campo, requeridorFoco, bringIntoViewRequester)
                        },
                        alFinalizarEdicion = { siguiente, siguienteCampo ->
                            finalizarEdicionYSaltar(siguiente, siguienteCampo)
                        }
                    )

                    if (!esModoM3U) {
                        CampoTextoPremiumTV(
                            valor = usuario,
                            alCambiar = { usuario = it },
                            etiqueta = "Usuario",
                            campo = CampoInicioSesionTV.Usuario,
                            campoEditando = campoEditando,
                            alEditarCampo = { actualizarCampoEditando(it) },
                            alCambiarFoco = { campo, enfocado ->
                                campoConFoco = if (enfocado) campo else campoConFoco.takeUnless { it == campo }
                            },
                            ignorarPerdidaFoco = saltoDeCampoEnProgreso,
                            requeridorFoco = requeridorFocoUsuario,
                            siguienteRequeridorFoco = requeridorFocoContrasena,
                            siguienteCampo = CampoInicioSesionTV.Contrasena,
                            alActivarEdicion = { campo, requeridorFoco, bringIntoViewRequester ->
                                activarEdicion(campo, requeridorFoco, bringIntoViewRequester)
                            },
                            alFinalizarEdicion = { siguiente, siguienteCampo ->
                                finalizarEdicionYSaltar(siguiente, siguienteCampo)
                            }
                        )
                        CampoTextoPremiumTV(
                            valor = contrasena,
                            alCambiar = { contrasena = it },
                            etiqueta = "Contraseña",
                            esContrasena = true,
                            campo = CampoInicioSesionTV.Contrasena,
                            campoEditando = campoEditando,
                            alEditarCampo = { actualizarCampoEditando(it) },
                            alCambiarFoco = { campo, enfocado ->
                                campoConFoco = if (enfocado) campo else campoConFoco.takeUnless { it == campo }
                            },
                            ignorarPerdidaFoco = saltoDeCampoEnProgreso,
                            requeridorFoco = requeridorFocoContrasena,
                            siguienteRequeridorFoco = requeridorFocoBotonIniciar,
                            siguienteCampo = null,
                            alActivarEdicion = { campo, requeridorFoco, bringIntoViewRequester ->
                                activarEdicion(campo, requeridorFoco, bringIntoViewRequester)
                            },
                            alFinalizarEdicion = { siguiente, siguienteCampo ->
                                finalizarEdicionYSaltar(siguiente, siguienteCampo)
                            }
                        )
                    }

                    val interactionSourceBoton = remember { MutableInteractionSource() }
                    val botonEnFoco by interactionSourceBoton.collectIsFocusedAsState()
                    val escalaBoton by animateFloatAsState(targetValue = if (botonEnFoco) 1.03f else 1f)

                    Button(
                        onClick = {
                            val urlLimpia = urlServidor.trim()
                            val userLimpia = usuario.trim()
                            val passLimpia = contrasena.trim()
                            if (esModoM3U) modeloVista.iniciarSesionConM3U(urlLimpia)
                            else modeloVista.iniciarSesion(urlLimpia, userLimpia, passLimpia)
                        },
                        interactionSource = interactionSourceBoton,
                        modifier = Modifier
                            .focusRequester(requeridorFocoBotonIniciar)
                            .fillMaxWidth()
                            .height(50.dp)
                            .graphicsLayer {
                                scaleX = escalaBoton
                                scaleY = escalaBoton
                            }
                            .border(
                                width = if (botonEnFoco) 1.5.dp else 0.dp,
                                color = if (botonEnFoco) TemaTV.AcentoClaro.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(TemaTV.RedondeoControl)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (botonEnFoco) TemaTV.AcentoClaro else TemaTV.Acento,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(TemaTV.RedondeoControl),
                        enabled = estadoInterfaz !is ModeloVistaAutenticacion.EstadoAutenticacion.Cargando
                    ) {
                        if (estadoInterfaz is ModeloVistaAutenticacion.EstadoAutenticacion.Cargando) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val interactionSourceBotonQR = remember { MutableInteractionSource() }
                    val botonQREnFoco by interactionSourceBotonQR.collectIsFocusedAsState()
                    val escalaBotonQR by animateFloatAsState(targetValue = if (botonQREnFoco) 1.03f else 1f)

                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = alCambiarAPantallaQR,
                        interactionSource = interactionSourceBotonQR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .graphicsLayer {
                                scaleX = escalaBotonQR
                                scaleY = escalaBotonQR
                            }
                            .border(
                                width = if (botonQREnFoco) 1.5.dp else 1.dp,
                                color = if (botonQREnFoco) TemaTV.AcentoClaro else TemaTV.SuperficieSuave,
                                shape = RoundedCornerShape(TemaTV.RedondeoControl)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (botonQREnFoco) TemaTV.SuperficieSuave else TemaTV.Superficie,
                            contentColor = if (botonQREnFoco) Color.White else TemaTV.TextoSecundario
                        ),
                        shape = RoundedCornerShape(TemaTV.RedondeoControl)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Activación remota (Código QR)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Usa el control remoto para moverte entre campos y presiona Aceptar para seleccionar.",
                        color = TemaTV.TextoTenue,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (campoParaDesplazamiento != null) {
                        Spacer(modifier = Modifier.height(220.dp))
                    }
                }
            }
        }
    }
}


