package com.williamfq.xhat.utils

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor() {
    private lateinit var crashlytics: FirebaseCrashlytics
    private var isInitialized = false
    private var isReporting = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "CrashReporter"
        private const val USER_ID = "William8677"
        private const val TIMESTAMP = "2025-02-21 20:00:03"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val REPORTING_TIMEOUT = 5000L // 5 segundos
    }

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) {
            Timber.tag(TAG).d("CrashReporter ya está inicializado")
            return
        }

        try {
            crashlytics = FirebaseCrashlytics.getInstance()
            setupCrashlytics()
            isInitialized = true
            Timber.tag(TAG).d("CrashReporter inicializado correctamente con contexto: ${context.packageName}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error al inicializar CrashReporter")
            handleInitializationError(e)
        }
    }

    private fun setupCrashlytics() {
        crashlytics.apply {
            setCustomKey("user_id", USER_ID)
            setCustomKey("timestamp", TIMESTAMP)
            setCustomKey("initialization_time", System.currentTimeMillis())
            setUserId(USER_ID)
            isCrashlyticsCollectionEnabled = true
        }
    }

    private fun handleInitializationError(error: Exception) {
        Timber.tag(TAG).e(error, "Fallback: usando logging básico debido a error de inicialización")
        Timber.tag(TAG).e("Detalles del error: ${error.message}")
        error.stackTrace.forEach { element ->
            Timber.tag(TAG).e("    en $element")
        }
    }

    @Synchronized
    fun reportException(exception: Exception) {
        if (isReporting) {
            Timber.tag(TAG).w("Reporte en curso, evitando bucle")
            return
        }
        isReporting = true
        scope.launch {
            tryWithRetries("reportException") {
                if (!isInitialized) {
                    Timber.tag(TAG).e("CrashReporter no inicializado")
                    return@tryWithRetries
                }
                crashlytics.apply {
                    setCustomKey("last_action", "custom_exception")
                    setCustomKey("exception_time", System.currentTimeMillis())
                    setCustomKey("exception_class", exception.javaClass.name)
                    setCustomKey("exception_message", exception.message ?: "Sin mensaje")
                    setCustomKey("report_timestamp", TIMESTAMP)
                    recordException(exception)
                }
                Timber.tag(TAG).e(exception, "Excepción reportada: ${exception.message}")
            }
            isReporting = false
        }
    }

    private fun fallbackExceptionLogging(exception: Exception) {
        try {
            Timber.tag(TAG).e("LOG DE RESPALDO - Detalles de la excepción:")
            Timber.tag(TAG).e("Hora: $TIMESTAMP")
            Timber.tag(TAG).e("Usuario: $USER_ID")
            Timber.tag(TAG).e("Excepción: ${exception.javaClass.name}")
            Timber.tag(TAG).e("Mensaje: ${exception.message}")
            Timber.tag(TAG).e("Traza de pila:")
            exception.stackTrace.forEach { element ->
                Timber.tag(TAG).e("    en $element")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error crítico en logging de respaldo: ${e.message}")
        }
    }

    fun logError(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (!isInitialized) {
            Timber.tag(TAG).e("CrashReporter no inicializado para logError")
            return
        }
        scope.launch {
            tryWithRetries("logError") {
                crashlytics.apply {
                    setCustomKey("error_priority", priority)
                    setCustomKey("error_tag", tag ?: "SIN_TAG")
                    setCustomKey("error_time", TIMESTAMP)
                    log("$priority/$tag: $message")
                    throwable?.let { recordException(it) }
                }
            }
        }
    }

    private fun fallbackErrorLogging(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        Timber.tag(TAG).e("LOG DE RESPALDO DE ERROR:")
        Timber.tag(TAG).e("Prioridad: $priority")
        Timber.tag(TAG).e("Etiqueta: $tag")
        Timber.tag(TAG).e("Mensaje: $message")
        throwable?.let {
            Timber.tag(TAG).e("Throwable: ${it.message}")
            it.stackTrace.forEach { element ->
                Timber.tag(TAG).e("    en $element")
            }
        }
    }

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        if (!isInitialized) {
            Timber.tag(TAG).e("CrashReporter no inicializado para logEvent")
            return
        }
        scope.launch {
            tryWithRetries("logEvent") {
                crashlytics.apply {
                    setCustomKey("event_name", eventName)
                    setCustomKey("event_time", TIMESTAMP)
                    params.forEach { (key, value) -> setCustomKey(key, value) }
                    log("Evento: $eventName, Parámetros: $params")
                }
            }
        }
    }

    private fun fallbackEventLogging(eventName: String, params: Map<String, String>) {
        Timber.tag(TAG).i("LOG DE RESPALDO DE EVENTO:")
        Timber.tag(TAG).i("Evento: $eventName")
        Timber.tag(TAG).i("Hora: $TIMESTAMP")
        Timber.tag(TAG).i("Parámetros:")
        params.forEach { (key, value) ->
            Timber.tag(TAG).i("    $key: $value")
        }
    }

    fun setUserIdentifier(userId: String) {
        if (!isInitialized) {
            Timber.tag(TAG).e("CrashReporter no inicializado para setUserIdentifier")
            return
        }
        scope.launch {
            tryWithRetries("setUserIdentifier") {
                crashlytics.apply {
                    setUserId(userId)
                    setCustomKey("custom_user_id", userId)
                    setCustomKey("id_set_time", TIMESTAMP)
                }
                Timber.tag(TAG).d("Identificador de usuario establecido: $userId")
            }
        }
    }

    fun enableCrashReporting(enable: Boolean) {
        if (!isInitialized) {
            Timber.tag(TAG).e("CrashReporter no inicializado para enableCrashReporting")
            return
        }
        scope.launch {
            tryWithRetries("enableCrashReporting") {
                crashlytics.apply {
                    setCrashlyticsCollectionEnabled(enable)
                    setCustomKey("crash_reporting_enabled", enable)
                    setCustomKey("reporting_status_change_time", TIMESTAMP)
                }
                Timber.tag(TAG).d("Reporte de crashes habilitado: $enable")
            }
        }
    }

    private suspend fun tryWithRetries(operationName: String, block: suspend () -> Unit) {
        var attempts = 0
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                block()
                return
            } catch (e: Exception) {
                attempts++
                if (attempts == MAX_RETRY_ATTEMPTS) {
                    Timber.tag(TAG).e(e, "Fallo en $operationName tras $MAX_RETRY_ATTEMPTS intentos")
                    when (operationName) {
                        "reportException" -> fallbackExceptionLogging(e)
                        "logError" -> fallbackErrorLogging(0, null, "Fallo en logError", e)
                        "logEvent" -> fallbackEventLogging(operationName, emptyMap())
                        else -> Timber.tag(TAG).e(e, "Fallo sin respaldo específico")
                    }
                    return
                }
                Timber.tag(TAG).w(e, "$operationName falló, reintentando ($attempts/$MAX_RETRY_ATTEMPTS)")
                delay(REPORTING_TIMEOUT)
            }
        }
    }
}