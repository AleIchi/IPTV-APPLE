package com.iptv.fiber.datos.utilidades

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MonitorRed {
    val estaConectado: Flow<Boolean> = flowOf(true)
}
