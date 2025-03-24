package com.williamfq.domain.utils

import com.arthenica.ffmpegkit.FFmpegKit
import timber.log.Timber
import java.io.File
import javax.inject.Inject

interface AudioTrimmer {
    fun trim(inputFilePath: String, startMs: Long, endMs: Long): Boolean
}

class AudioTrimmerImpl @Inject constructor() : AudioTrimmer {

    override fun trim(inputFilePath: String, startMs: Long, endMs: Long): Boolean {
        val outputFilePath = inputFilePath.replace(".aac", "_trimmed.aac")
        val startTime = formatTime(startMs)
        val duration = formatTime(endMs - startMs)

        val command = arrayOf(
            "-i", inputFilePath,
            "-ss", startTime,
            "-t", duration,
            "-c", "copy",
            outputFilePath
        )

        return try {
            val session = FFmpegKit.execute(command.joinToString(" "))
            val rc = session.returnCode
            if (rc.isValueSuccess) {
                File(inputFilePath).delete()
                File(outputFilePath).renameTo(File(inputFilePath))
                Timber.d("Audio recortado exitosamente: $inputFilePath")
                true
            } else {
                Timber.e("Error al recortar audio: FFmpeg rc=${rc.value}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Excepción al recortar audio")
            false
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        val minutes = seconds / 60
        val hours = minutes / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}