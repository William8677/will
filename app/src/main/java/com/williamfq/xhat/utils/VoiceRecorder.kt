package com.williamfq.xhat.utils

import android.net.Uri

interface VoiceRecorder {
    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun startRecording()
    fun stopRecording()
    fun getRecordedFileUri(): Uri?
    fun getAmplitude(): Int


}