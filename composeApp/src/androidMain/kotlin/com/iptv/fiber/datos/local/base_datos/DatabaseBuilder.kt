package com.iptv.fiber.datos.local.base_datos

import androidx.room.Room
import androidx.room.RoomDatabase
import com.iptv.fiber.AplicacionIPTV

actual fun getDatabaseBuilder(): RoomDatabase.Builder<BaseDatosIPTV> {
    val context = AplicacionIPTV.instancia
    val dbFile = context.getDatabasePath("iptv_database.db")
    return Room.databaseBuilder(
        context = context,
        name = dbFile.absolutePath,
        factory = { BaseDatosIPTV_Impl() } // Note: Room KSP generates this class
    )
}
