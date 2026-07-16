package com.iptv.fiber.datos.local.base_datos

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<BaseDatosIPTV>
