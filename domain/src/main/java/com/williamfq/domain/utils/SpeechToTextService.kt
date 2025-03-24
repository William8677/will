package com.williamfq.domain.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import timber.log.Timber
import javax.inject.Inject

interface SpeechToTextService {
    fun transcribeAudio(filePath: String, callback: (String) -> Unit)
}

class SpeechToTextServiceImpl @Inject constructor(
    private val context: Context
) : SpeechToTextService, RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var transcriptionCallback: ((String) -> Unit)? = null

    override fun transcribeAudio(filePath: String, callback: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.e("Reconocimiento de voz no disponible en este dispositivo")
            callback("Reconocimiento de voz no disponible")
            return
        }

        transcriptionCallback = callback
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@SpeechToTextServiceImpl)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            }
            startListening(intent)
        }
    }

    override fun onResults(results: Bundle?) {
        val transcription = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
        transcriptionCallback?.invoke(transcription)
        cleanup()
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
            SpeechRecognizer.ERROR_NO_MATCH -> "No se encontraron coincidencias"
            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera de voz"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
            else -> "Error desconocido"
        }
        Timber.e("Error en transcripción: $errorMessage")
        transcriptionCallback?.invoke("Error: $errorMessage")
        cleanup()
    }

    private fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}