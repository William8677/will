package com.williamfq.xhat.network

import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object NetworkManager {
    private const val SOCKET_TAG = 0x1234 // Valor hexadecimal válido

    suspend fun <T> performNetworkOperation(operation: () -> T): T {
        return withContext(Dispatchers.IO) {
            try {
                TrafficStats.setThreadStatsTag(SOCKET_TAG)
                val result = operation()
                Timber.d("Operación de red completada: $result")
                result
            } catch (e: Exception) {
                Timber.e(e, "Error en operación de red")
                throw e
            } finally {
                TrafficStats.clearThreadStatsTag()
            }
        }
    }

    // Función avanzada para Xhat: enviar mensajes con previsualización en tiempo real
    suspend fun sendMessageWithPreview(message: String, recipientId: String): Boolean {
        return performNetworkOperation {
            // Simulación de envío (reemplaza con tu lógica real)
            Timber.d("Enviando mensaje '$message' a $recipientId")
            true
        }
    }
}