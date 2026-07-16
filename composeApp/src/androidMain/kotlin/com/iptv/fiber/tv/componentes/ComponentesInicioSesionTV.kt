package com.iptv.fiber.tv.componentes

// removed android import: import android.content.Context
import android.graphics.Rect
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Devuelve `true` si el teclado software está visible, detectado tanto por WindowInsets como por el layout del árbol de vistas. */
@Composable
fun recordarTecladoVisibleTV(): Boolean {
    val densidad = LocalDensity.current
    val vistaActual = LocalView.current
    val imeVisible = WindowInsets.ime.getBottom(densidad) > 0
    var tecladoVisiblePorLayout by remember { mutableStateOf(false) }

    DisposableEffect(vistaActual) {
        val rect = Rect()
        val oyente = ViewTreeObserver.OnGlobalLayoutListener {
            vistaActual.getWindowVisibleDisplayFrame(rect)
            val altoRaiz = vistaActual.rootView.height
            val altoVisible = rect.height()
            val diferenciaAltura = altoRaiz - altoVisible
            tecladoVisiblePorLayout = altoRaiz > 0 && diferenciaAltura > altoRaiz * 0.18f
        }

        vistaActual.viewTreeObserver.addOnGlobalLayoutListener(oyente)
        oyente.onGlobalLayout()

        onDispose {
            if (vistaActual.viewTreeObserver.isAlive) {
                vistaActual.viewTreeObserver.removeOnGlobalLayoutListener(oyente)
            }
        }
    }

    return imeVisible || tecladoVisiblePorLayout
}

enum class CampoInicioSesionTV {
    Url,
    Usuario,
    Contrasena
}

/** Campo de texto premium para TV que alterna entre modo navegación D-Pad y edición con teclado software. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CampoTextoPremiumTV(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    marcadorPosicion: String = "",
    esContrasena: Boolean = false,
    campo: CampoInicioSesionTV,
    campoEditando: CampoInicioSesionTV?,
    alEditarCampo: (CampoInicioSesionTV?) -> Unit,
    alCambiarFoco: (CampoInicioSesionTV, Boolean) -> Unit,
    ignorarPerdidaFoco: Boolean,
    requeridorFoco: FocusRequester,
    siguienteRequeridorFoco: FocusRequester?,
    siguienteCampo: CampoInicioSesionTV?,
    alActivarEdicion: (CampoInicioSesionTV, FocusRequester, BringIntoViewRequester) -> Unit,
    alFinalizarEdicion: (FocusRequester?, CampoInicioSesionTV?) -> Unit,
    modifier: Modifier = Modifier
) {
    val requeridorTraerAVista = remember { BringIntoViewRequester() }
    val controladorTeclado = LocalSoftwareKeyboardController.current
    val vistaActual = LocalView.current
    val gestorFoco = LocalFocusManager.current
    val gestorMetodoEntrada = remember(vistaActual) {
        vistaActual.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val estaEditando = campoEditando == campo
    var okInicioEdicion by remember { mutableStateOf(false) }
    var contrasenaVisible by remember { mutableStateOf(false) }

    val fuenteInteraccionIcono = remember { MutableInteractionSource() }
    val iconoEnFoco by fuenteInteraccionIcono.collectIsFocusedAsState()

    /** Cierra edición y oculta el teclado software. */
    fun cerrarEdicion() {
        alEditarCampo(null)
        controladorTeclado?.hide()
        gestorMetodoEntrada.hideSoftInputFromWindow(vistaActual.windowToken, 0)
    }

    /** Avanza al siguiente campo o cierra el teclado si es el último. */
    fun finalizarEdicion() {
        alFinalizarEdicion(siguienteRequeridorFoco, siguienteCampo)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            label = { Text(etiqueta) },
            placeholder = { Text(marcadorPosicion, color = TemaTV.TextoTenue) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(requeridorFoco)
                .bringIntoViewRequester(requeridorTraerAVista)
                .onFocusChanged { estadoFoco ->
                    alCambiarFoco(campo, estadoFoco.isFocused)
                    when {
                        !estadoFoco.isFocused && estaEditando && !ignorarPerdidaFoco -> cerrarEdicion()
                        estadoFoco.isFocused && !estaEditando && !ignorarPerdidaFoco -> {
                            controladorTeclado?.hide()
                            gestorMetodoEntrada.hideSoftInputFromWindow(vistaActual.windowToken, 0)
                        }
                    }
                }
                .onPreviewKeyEvent { evento ->
                    val esTeclaOk = evento.key == Key.DirectionCenter ||
                        evento.key == Key.Enter ||
                        evento.key == Key.NumPadEnter
                    val esOkPresionado = evento.type == KeyEventType.KeyDown && esTeclaOk
                    val esOkSoltado = evento.type == KeyEventType.KeyUp && esTeclaOk
                    val esAtras = evento.type == KeyEventType.KeyUp && evento.key == Key.Back
                    val esNavegacion = evento.type == KeyEventType.KeyUp &&
                        (evento.key == Key.DirectionUp ||
                            evento.key == Key.DirectionDown ||
                            evento.key == Key.DirectionLeft ||
                            evento.key == Key.DirectionRight)

                    when {
                        esOkPresionado && !estaEditando -> {
                            okInicioEdicion = true
                            alActivarEdicion(campo, requeridorFoco, requeridorTraerAVista)
                            true
                        }
                        esOkSoltado && okInicioEdicion -> {
                            okInicioEdicion = false
                            true
                        }
                        esOkSoltado && estaEditando -> {
                            finalizarEdicion()
                            true
                        }
                        (esAtras || esNavegacion) && estaEditando -> {
                            cerrarEdicion()
                            true
                        }
                        else -> false
                    }
                },
            singleLine = true,
            visualTransformation = if (esContrasena && !contrasenaVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finalizarEdicion() }),
            textStyle = TextStyle(color = TemaTV.TextoPrincipal, fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TemaTV.Superficie.copy(alpha = 0.86f),
                unfocusedContainerColor = TemaTV.Superficie.copy(alpha = 0.58f),
                focusedBorderColor = TemaTV.AcentoClaro,
                unfocusedBorderColor = TemaTV.Linea,
                cursorColor = TemaTV.AcentoClaro,
                focusedTextColor = TemaTV.TextoPrincipal,
                unfocusedTextColor = TemaTV.TextoSecundario,
                focusedLabelColor = TemaTV.AcentoClaro,
                unfocusedLabelColor = TemaTV.TextoSecundario
            ),
            shape = RoundedCornerShape(TemaTV.RedondeoControl)
        )

        if (esContrasena) {
            Spacer(modifier = Modifier.width(12.dp))
            val escalaIcono by animateFloatAsState(targetValue = if (iconoEnFoco) 1.05f else 1f)

            IconButton(
                onClick = { contrasenaVisible = !contrasenaVisible },
                interactionSource = fuenteInteraccionIcono,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = escalaIcono
                        scaleY = escalaIcono
                    }
                    .background(
                        if (iconoEnFoco) TemaTV.Superficie.copy(alpha = 0.86f) else TemaTV.Superficie.copy(alpha = 0.58f),
                        RoundedCornerShape(TemaTV.RedondeoControl)
                    )
                    .border(
                        width = if (iconoEnFoco) 1.5.dp else 1.dp,
                        color = if (iconoEnFoco) TemaTV.AcentoClaro else TemaTV.Linea,
                        shape = RoundedCornerShape(TemaTV.RedondeoControl)
                    )
            ) {
                Icon(
                    imageVector = if (contrasenaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (contrasenaVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = if (iconoEnFoco) TemaTV.AcentoClaro else TemaTV.TextoSecundario
                )
            }
        }
    }
}
