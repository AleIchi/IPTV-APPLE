package com.iptv.fiber.datos.local.base_datos

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual fun getDatabaseBuilder(): RoomDatabase.Builder<BaseDatosIPTV> {
    val dbFile = NSHomeDirectory() + "/iptv_database.db"
    return Room.databaseBuilder<BaseDatosIPTV>(
        name = dbFile,
        factory = { BaseDatosIPTV_Impl() }
    )
}
