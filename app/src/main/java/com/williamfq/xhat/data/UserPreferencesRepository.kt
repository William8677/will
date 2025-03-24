package com.williamfq.xhat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Definición del DataStore a nivel de contexto
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Clave para la preferencia de reporte de crashes (renombrada para seguir convenciones)
    private val crashReportingEnabledKey = booleanPreferencesKey("crash_reporting_enabled")

    // Flujo que emite el estado actual del reporte de crashes
    val crashReportingEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val enabled = preferences[crashReportingEnabledKey] ?: true
            Timber.d("Leyendo preferencia de reporte de crashes: $enabled")
            enabled
        }
        .catch { exception ->
            Timber.e(exception, "Error crítico al leer las preferencias de usuario")
            emit(true) // Valor por defecto en caso de error
        }

    // Establece si el reporte de crashes está habilitado o no
    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[crashReportingEnabledKey] = enabled
            }
            Timber.i("Reporte de crashes actualizado exitosamente a: $enabled")
        } catch (e: Exception) {
            Timber.e(e, "Fallo al guardar la preferencia de reporte de crashes")
            throw UserPreferencesException("Error al guardar la preferencia de reporte de crashes", e)
        }
    }

    // Obtiene el estado actual del reporte de crashes de forma asíncrona
    suspend fun isCrashReportingEnabled(): Boolean {
        return try {
            val enabled = context.dataStore.data
                .map { preferences ->
                    preferences[crashReportingEnabledKey] ?: true
                }
                .first()
            Timber.d("Estado actual del reporte de crashes: $enabled")
            enabled
        } catch (e: Exception) {
            Timber.e(e, "Error al recuperar el estado del reporte de crashes")
            throw UserPreferencesException("No se pudo obtener el estado del reporte de crashes", e)
        }
    }
}

// Excepción personalizada para un manejo de errores más claro
class UserPreferencesException(message: String, cause: Throwable? = null) : Exception(message, cause)