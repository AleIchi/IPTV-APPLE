package com.iptv.fiber.datos.repositorio

import com.iptv.fiber.datos.modelo.ConfiguracionServidor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Gestiona la lista en memoria de servidores IPTV configurados por el usuario. */
class RepositorioServidor {

    private val _servidores = MutableStateFlow<List<ConfiguracionServidor>>(emptyList())
    /** Flow observable con la lista actual de servidores registrados. */
    val servidores: Flow<List<ConfiguracionServidor>> = _servidores.asStateFlow()

    /**
     * Agrega o actualiza un servidor en la lista. Si [servidor] tiene [estaActivo] = true,
     * desactiva automáticamente los demás para mantener solo uno activo a la vez.
     */
    fun agregarServidor(servidor: ConfiguracionServidor) {
        val servidoresActuales = _servidores.value.toMutableList()
        
        // Si este servidor se activa, desactivar los demás
        if (servidor.estaActivo) {
            servidoresActuales.forEachIndexed { indice, s ->
                if (s.estaActivo) {
                    servidoresActuales[indice] = s.copy(estaActivo = false)
                }
            }
        }
        
        // Comprobar si el servidor ya existe
        val indiceExistente = servidoresActuales.indexOfFirst { it.id == servidor.id }
        if (indiceExistente >= 0) {
            servidoresActuales[indiceExistente] = servidor
        } else {
            servidoresActuales.add(servidor)
        }
        
        _servidores.value = servidoresActuales
    }
    
    /** Elimina el servidor con el [idServidor] indicado de la lista. */
    fun eliminarServidor(idServidor: String) {
        val servidoresActuales = _servidores.value.toMutableList()
        servidoresActuales.removeAll { it.id == idServidor }
        _servidores.value = servidoresActuales
    }
    
    /** Marca como activo el servidor con [idServidor] y desactiva todos los demás. */
    fun establecerServidorActivo(idServidor: String) {
        val servidoresActuales = _servidores.value.toMutableList()
        servidoresActuales.forEachIndexed { indice, servidor ->
            servidoresActuales[indice] = servidor.copy(estaActivo = servidor.id == idServidor)
        }
        _servidores.value = servidoresActuales
    }
    
    /** Devuelve el servidor actualmente activo, o null si ninguno está activo. */
    fun obtenerServidorActivo(): ConfiguracionServidor? {
        return _servidores.value.firstOrNull { it.estaActivo }
    }
    
    /** Busca y devuelve el servidor con el [idServidor] indicado, o null si no existe. */
    fun obtenerServidor(idServidor: String): ConfiguracionServidor? {
        return _servidores.value.firstOrNull { it.id == idServidor }
    }
}

