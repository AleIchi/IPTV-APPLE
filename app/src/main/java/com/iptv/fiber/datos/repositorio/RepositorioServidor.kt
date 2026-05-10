package com.iptv.fiber.datos.repositorio

import com.iptv.fiber.datos.modelo.ConfiguracionServidor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RepositorioServidor {
    
    private val _servidores = MutableStateFlow<List<ConfiguracionServidor>>(emptyList())
    val servidores: Flow<List<ConfiguracionServidor>> = _servidores.asStateFlow()
    
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
    
    fun eliminarServidor(idServidor: String) {
        val servidoresActuales = _servidores.value.toMutableList()
        servidoresActuales.removeAll { it.id == idServidor }
        _servidores.value = servidoresActuales
    }
    
    fun establecerServidorActivo(idServidor: String) {
        val servidoresActuales = _servidores.value.toMutableList()
        servidoresActuales.forEachIndexed { indice, servidor ->
            servidoresActuales[indice] = servidor.copy(estaActivo = servidor.id == idServidor)
        }
        _servidores.value = servidoresActuales
    }
    
    fun obtenerServidorActivo(): ConfiguracionServidor? {
        return _servidores.value.firstOrNull { it.estaActivo }
    }
    
    fun obtenerServidor(idServidor: String): ConfiguracionServidor? {
        return _servidores.value.firstOrNull { it.id == idServidor }
    }
}

