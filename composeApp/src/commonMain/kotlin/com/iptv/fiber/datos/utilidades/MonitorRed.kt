package com.iptv.fiber.datos.utilidades

// removed android import: import android.content.Context
// removed android import: import android.net.ConnectivityManager
// removed android import: import android.net.Network
// removed android import: import android.net.NetworkCapabilities
// removed android import: import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitor reactivo de conexión de red para Android.
 * Proporciona un Flow de tipo Boolean que emite true cuando hay conexión a internet y false en caso contrario.
 */
class MonitorRed(contexto: Context) {
    private val connectivityManager =
        contexto.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val estaConectado: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Estado inicial de la conexión
        val activeNetwork = connectivityManager.activeNetwork
        val conectadoInicial = activeNetwork != null
        trySend(conectadoInicial)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
