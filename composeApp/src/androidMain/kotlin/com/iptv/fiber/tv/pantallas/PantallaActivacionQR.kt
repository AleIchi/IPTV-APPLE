package com.iptv.fiber.tv.pantallas

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.iptv.fiber.R
import com.iptv.fiber.datos.local.GestorPreferencias
import com.iptv.fiber.interfaz.modelovista.ModeloVistaAutenticacion
import com.iptv.fiber.tv.componentes.PanelTV
import com.iptv.fiber.tv.componentes.TemaTV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Pantalla de activación por QR: genera el código QR de la MAC virtual y muestra instrucciones paso a paso. */
@Composable
fun PantallaActivacionQR(
    modeloVista: ModeloVistaAutenticacion,
    gestorPreferencias: GestorPreferencias,
    alVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var macAddress by remember { mutableStateOf("Cargando...") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var estadoConexion by remember { mutableStateOf("Esperando subida de lista...") }
    var estaCargando by remember { mutableStateOf(false) }

    val portalWebUrl = "https://web-iptv-50785.web.app" // URL de Hosting de Firebase

    // Obtener / Generar MAC y Generar QR
    LaunchedEffect(Unit) {
        val mac = gestorPreferencias.obtenerOGenerarMacVirtual()
        macAddress = mac

        // Incluir la MAC en la URL para que el portal web la pre-llene automáticamente
        val urlConMac = "$portalWebUrl?mac=$mac"

        // Generar QR en segundo plano con la URL que incluye la MAC
        coroutineScope.launch(Dispatchers.Default) {
            val bitmap = generarCodigoQR(urlConMac, 380, 380)
            withContext(Dispatchers.Main) {
                qrBitmap = bitmap
            }
        }
    }

    // Polling de Firestore mediante API REST
    LaunchedEffect(macAddress) {
        if (macAddress == "Cargando...") return@LaunchedEffect
        
        val cliente = OkHttpClient()
        val urlRest = "https://firestore.googleapis.com/v1/projects/web-iptv-50785/databases/(default)/documents/devices/$macAddress"

        while (true) {
            delay(5000) // Verificar cada 5 segundos
            
            try {
                val peticion = Request.Builder()
                    .url(urlRest)
                    .build()
                
                withContext(Dispatchers.IO) {
                    cliente.newCall(peticion).execute().use { respuesta ->
                        if (respuesta.isSuccessful) {
                            val cuerpo = respuesta.body?.string()
                            if (!cuerpo.isNullOrEmpty()) {
                                val json = JSONObject(cuerpo)
                                val updateTime = json.optString("updateTime", "")
                                val ultimaActualizacion = gestorPreferencias.obtenerUltimaActualizacionQR()

                                if (updateTime.isEmpty() || updateTime != ultimaActualizacion) {
                                    val fields = json.optJSONObject("fields")
                                    if (fields != null) {
                                        val mode = fields.optJSONObject("mode")?.optString("stringValue")
                                        val url = fields.optJSONObject("url")?.optString("stringValue")

                                        if (!url.isNullOrEmpty()) {
                                            gestorPreferencias.guardarUltimaActualizacionQR(updateTime)
                                            withContext(Dispatchers.Main) {
                                            estadoConexion = "¡Lista detectada! Conectando..."
                                            estaCargando = true
                                        }

                                        // 1. Eliminar el documento de Firestore para que no se vuelva a cargar si el usuario cierra sesión
                                        try {
                                            val peticionBorrado = Request.Builder()
                                                .url(urlRest)
                                                .delete()
                                                .build()
                                            cliente.newCall(peticionBorrado).execute().close()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }

                                        // 2. Iniciar sesión
                                        if (mode == "XTREAM") {
                                            val user = fields.optJSONObject("username")?.optString("stringValue") ?: ""
                                            val pass = fields.optJSONObject("password")?.optString("stringValue") ?: ""
                                            withContext(Dispatchers.Main) {
                                                modeloVista.iniciarSesion(url, user, pass)
                                            }
                                        } else {
                                            // Modo M3U
                                            withContext(Dispatchers.Main) {
                                                modeloVista.iniciarSesionConM3U(url)
                                            }
                                        }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler {
        alVolver()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TemaTV.FondoPrincipal)
    ) {
        // Fondo decorativo con gradiente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(TemaTV.Acento.copy(alpha = 0.22f), Color.Transparent),
                        radius = 1100f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // Sección izquierda: Instrucciones
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logotipo_fiber_z),
                    contentDescription = "Fiber Z Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(28.dp))
                
                Text(
                    text = "Activación del Dispositivo",
                    color = TemaTV.TextoPrincipal,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "Configura tu lista de canales IPTV de forma remota siguiendo estos sencillos pasos:",
                    color = TemaTV.TextoSecundario,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Pasos numerados
                PasoInstruccion(numero = "1", texto = "Escanea el código QR de la derecha o ingresa desde tu móvil o PC a: $portalWebUrl")
                Spacer(modifier = Modifier.height(10.dp))
                PasoInstruccion(numero = "2", texto = "Ingresa el ID del Dispositivo (MAC Virtual) que aparece abajo.")
                Spacer(modifier = Modifier.height(10.dp))
                PasoInstruccion(numero = "3", texto = "Ingresa tu lista M3U o credenciales Xtream Codes y presiona Subir.")
                
                Spacer(modifier = Modifier.height(30.dp))

                // Botón de Volver
                val interactionSourceVolver = remember { MutableInteractionSource() }
                val volverEnFoco by interactionSourceVolver.collectIsFocusedAsState()
                val escalaVolver by animateFloatAsState(targetValue = if (volverEnFoco) 1.05f else 1f)

                Button(
                    onClick = alVolver,
                    interactionSource = interactionSourceVolver,
                    modifier = Modifier
                        .height(46.dp)
                        .graphicsLayer {
                            scaleX = escalaVolver
                            scaleY = escalaVolver
                        }
                        .border(
                            width = if (volverEnFoco) 1.5.dp else 0.dp,
                            color = if (volverEnFoco) TemaTV.AcentoClaro.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(TemaTV.RedondeoControl)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (volverEnFoco) TemaTV.Acento.copy(alpha = 0.4f) else TemaTV.Superficie,
                        contentColor = TemaTV.TextoPrincipal
                    ),
                    shape = RoundedCornerShape(TemaTV.RedondeoControl)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Volver al Login Manual", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Sección derecha: Panel de Activación QR y ID
            PanelTV(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Contenedor QR
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Código QR de activación",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(color = TemaTV.Acento, strokeWidth = 3.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "ID DEL DISPOSITIVO (MAC):",
                        color = TemaTV.TextoSecundario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    // ID / MAC
                    Text(
                        text = macAddress,
                        color = TemaTV.AcentoClaro,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .background(TemaTV.Superficie.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Estado Polling
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (estaCargando) {
                            CircularProgressIndicator(
                                color = TemaTV.AcentoClaro,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = TemaTV.TextoSecundario,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = estadoConexion,
                            color = if (estaCargando) TemaTV.AcentoClaro else TemaTV.TextoSecundario,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Fila de instrucción numerada con chip de número y texto descriptivo. */
@Composable
private fun PasoInstruccion(numero: String, texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(TemaTV.Acento.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .border(1.dp, TemaTV.AcentoClaro.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = numero,
                color = TemaTV.AcentoClaro,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = texto,
            color = TemaTV.TextoSecundario,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

/**
 * Genera codigo qr para presentarlo o reutilizarlo.
 */
private fun generarCodigoQR(contenido: String, ancho: Int, alto: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto)
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.RGB_565)
        for (x in 0 until ancho) {
            for (y in 0 until alto) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
