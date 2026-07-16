package com.iptv.fiber.datos.local.base_datos

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iptv.fiber.AplicacionIPTV

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
        // Variable para asegurarnos de tener SOLO UNA copia de la base de datos abierta al mismo tiempo.
        @Volatile private var INSTANCIA: BaseDatosIPTV? = null

        /** 
         * RETORNAR O CREAR LA BASE DE DATOS (Singleton):
         * Si la base de datos ya está abierta, la devuelve. 
         * Si no existe, la crea con el nombre "iptv_database".
         * Es peligroso abrir muchas veces la misma base de datos, por eso se hace así.
         */
        fun obtenerBaseDatos(): BaseDatosIPTV {
            return INSTANCIA
                    ?: synchronized(this) {
                val instancia = getDatabaseBuilder()
                                        // Si cambiamos la estructura de la tabla (ej. añadimos una columna),
                                        // esto borra la base anterior para evitar que la app crashee.
                                        .fallbackToDestructiveMigration()
                                        // MODO WAL (Write-Ahead Logging): 
                                        // Un truco avanzado. Permite que la app LEA favoritos al mismo tiempo
                                        // que ESCRIBE el historial. Esto evita los "tirones" y bloqueos en
                                        // TV Box baratos que tienen almacenamiento muy lento.
                                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                                        .build()
                        INSTANCIA = instancia
                        instancia
                    }
        }
    }
}
