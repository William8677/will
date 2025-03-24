package com.williamfq.xhat.utils

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.storage.FirebaseStorage
import com.williamfq.domain.utils.AudioTrimmer
import com.williamfq.domain.utils.SpeechToTextService
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

class VoiceRecorderImpl @Inject constructor(
    private val context: Context,
    private val audioTrimmer: AudioTrimmer,
    private val speechToTextService: SpeechToTextService
) : VoiceRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var outputFile: String? = null
    private var isRecording: Boolean = false
    private val notificationManager = NotificationManagerCompat.from(context)
    private val bufferSize = AudioRecord.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    override fun start() {
        startRecording()
    }

    override fun stop() {
        stopRecording()
    }

    override fun pause() {
        if (!isRecording) {
            Timber.w("No se está grabando, no se puede pausar")
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                Timber.d("Grabación en pausa")
            } catch (e: RuntimeException) {
                Timber.e(e, "Error al pausar la grabación")
            }
        } else {
            Timber.w("Pausa no soportada en esta versión de Android")
        }
    }

    override fun resume() {
        if (!isRecording) {
            Timber.w("No se está grabando, no se puede reanudar")
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                Timber.d("Grabación reanudada")
            } catch (e: RuntimeException) {
                Timber.e(e, "Error al reanudar la grabación")
            }
        } else {
            Timber.w("Reanudar no soportado en esta versión de Android")
        }
    }

    override fun startRecording() {
        if (isRecording) {
            Timber.w("Ya se está grabando")
            return
        }
        if (!hasMicrophonePermission()) {
            Timber.w("Permiso de micrófono no concedido")
            throw SecurityException("Se requiere permiso RECORD_AUDIO")
        }

        outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.aac").absolutePath
        mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000)
                }
                prepare()
                start()
                isRecording = true
                Timber.d("Grabación iniciada. Archivo: $outputFile")
                showRecordingNotification()
                startAmplitudeMonitoring()
            } catch (e: IOException) {
                Timber.e(e, "Error al iniciar la grabación")
            } catch (e: IllegalStateException) {
                Timber.e(e, "Estado ilegal al iniciar la grabación")
            }
        }
    }

    override fun stopRecording() {
        if (!isRecording) {
            Timber.w("No se está grabando, no se puede detener")
            return
        }
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            audioRecord?.stop()
            audioRecord?.release()
            Timber.d("Grabación detenida. Archivo guardado: $outputFile")
            notificationManager.cancel(RECORDING_NOTIFICATION_ID)
            outputFile?.let {
                audioTrimmer.trim(it, 0, 5000) // Ejemplo: recorta primeros 5 segundos
                uploadToFirebaseStorage(it)
                transcribeAudio(it)
            }
        } catch (e: RuntimeException) {
            Timber.e(e, "Error al detener la grabación")
        } finally {
            mediaRecorder = null
            audioRecord = null
            isRecording = false
        }
    }

    override fun getRecordedFileUri(): Uri? {
        return outputFile?.let { Uri.fromFile(File(it)) }
    }

    override fun getAmplitude(): Int {
        return mediaRecorder?.maxAmplitude ?: audioRecord?.let {
            val buffer = ShortArray(bufferSize)
            val read = it.read(buffer, 0, bufferSize)
            if (read > 0) buffer.maxOrNull()?.toInt() ?: 0 else 0
        } ?: 0
    }

    private fun hasMicrophonePermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun showRecordingNotification() {
        val notification = NotificationCompat.Builder(context, "recording_channel")
            .setContentTitle("Grabando audio")
            .setContentText("Grabación en curso")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(RECORDING_NOTIFICATION_ID, notification)
    }

    private fun uploadToFirebaseStorage(filePath: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("recordings/${File(filePath).name}")
        storageRef.putFile(Uri.fromFile(File(filePath)))
            .addOnSuccessListener { Timber.d("Archivo subido a Firebase Storage") }
            .addOnFailureListener { e -> Timber.e(e, "Error subiendo archivo") }
    }

    private fun transcribeAudio(filePath: String) {
        speechToTextService.transcribeAudio(filePath) { transcription ->
            Timber.d("Transcripción: $transcription")
        }
    }

    private fun startAmplitudeMonitoring() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        ).apply {
            startRecording()
        }
    }

    companion object {
        private const val RECORDING_NOTIFICATION_ID = 1001
    }
}