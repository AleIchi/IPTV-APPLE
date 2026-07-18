package com.iptv.fiber.datos.local.base_datos

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** 
 * EL DISCO DURO LOCAL DE LA APP (Room Database).
 * Aquí se configuran las tablas que se guardarán en la memoria del celular o TV,
 * como la lista de Favoritos y el Historial de "Seguir viendo".
 * Usamos "Room", que es una librería oficial de Android que facilita el trabajo con bases de datos (SQLite).
 */
@Database(entities = [Favorito::class, SeguirViendo::class], version = 3, exportSchema = false)
abstract class BaseDatosIPTV : RoomDatabase() {

    /** Devuelve el DAO (El mensajero) para operaciones sobre la tabla de favoritos. */
    abstract fun daoFavorito(): DaoFavorito

    /** Devuelve el DAO para operaciones sobre la tabla de historial de reproducción. */
    abstract fun daoSeguirViendo(): DaoSeguirViendo

    companion object {
        private val INSTANCIA: BaseDatosIPTV by lazy {
            getDatabaseBuilder()
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        fun obtenerBaseDatos(): BaseDatosIPTV {
            return INSTANCIA
        }
    }
}
