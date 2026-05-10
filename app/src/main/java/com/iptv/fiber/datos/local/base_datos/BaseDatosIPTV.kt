package com.iptv.fiber.datos.local.base_datos

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iptv.fiber.AplicacionIPTV

/** Base de datos local de la aplicación usando Room. */
@Database(entities = [Favorito::class, SeguirViendo::class], version = 2, exportSchema = false)
abstract class BaseDatosIPTV : RoomDatabase() {

    abstract fun daoFavorito(): DaoFavorito
    abstract fun daoSeguirViendo(): DaoSeguirViendo

    companion object {
        @Volatile private var INSTANCIA: BaseDatosIPTV? = null

        fun obtenerBaseDatos(): BaseDatosIPTV {
            return INSTANCIA
                    ?: synchronized(this) {
                        val instancia =
                                Room.databaseBuilder(
                                                AplicacionIPTV.instancia,
                                                BaseDatosIPTV::class.java,
                                                "iptv_database"
                                        )
                                        .fallbackToDestructiveMigration() // Migración automática al
                                        // cambiar versión
                                        .build()
                        INSTANCIA = instancia
                        instancia
                    }
        }
    }
}
