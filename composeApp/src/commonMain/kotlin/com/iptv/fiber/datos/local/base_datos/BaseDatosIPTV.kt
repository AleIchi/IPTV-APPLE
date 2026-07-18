package com.iptv.fiber.datos.local.base_datos

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Favorito::class, SeguirViendo::class], version = 3, exportSchema = false)
abstract class BaseDatosIPTV : RoomDatabase() {
    abstract fun daoFavorito(): DaoFavorito
    abstract fun daoSeguirViendo(): DaoSeguirViendo

    companion object {
        private val INSTANCIA: BaseDatosIPTV by lazy {
            getDatabaseBuilder()
                .fallbackToDestructiveMigration()
                .build()
        }

        fun obtenerBaseDatos(): BaseDatosIPTV {
            return INSTANCIA
        }
    }
}
