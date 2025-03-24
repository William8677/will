package com.williamfq.xhat.service.audio

import android.content.Context
import android.media.AudioManager as AndroidAudioManager
import javax.inject.Inject

class AudioManagerImpl @Inject constructor(
    private val context: Context
) : AudioManager {
    private var muted = false
    private var currentVolume = 1.0f
    private var currentDevice = AudioDeviceType.EARPIECE
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AndroidAudioManager
    private val deviceCallbacks = mutableSetOf<AudioDeviceCallback>()

    override fun start() {
        updateAudioDevices()
        audioManager.mode = AndroidAudioManager.MODE_IN_COMMUNICATION
    }

    override fun stop() {
        audioManager.mode = AndroidAudioManager.MODE_NORMAL
        audioManager.stopBluetoothSco()
        deviceCallbacks.clear()
    }

    override fun setMuted(muted: Boolean) {
        if (this.muted != muted) {
            this.muted = muted
            if (muted) {
                setVolume(0f)
            } else {
                setVolume(currentVolume)
            }
            deviceCallbacks.forEach { it.onMuteChanged(muted) }
        }
    }

    override fun isMuted(): Boolean = muted

    override fun setVolume(volume: Float) {
        val newVolume = volume.coerceIn(0f, 1f)
        if (currentVolume != newVolume) {
            currentVolume = newVolume
            if (!muted) {
                val streamVolume = (currentVolume * audioManager.getStreamMaxVolume(AndroidAudioManager.STREAM_VOICE_CALL)).toInt()
                audioManager.setStreamVolume(
                    AndroidAudioManager.STREAM_VOICE_CALL,
                    streamVolume,
                    0
                )
            }
            deviceCallbacks.forEach { it.onVolumeChanged(currentVolume) }
        }
    }

    override fun getVolume(): Float = currentVolume

    override fun setAudioDevice(deviceType: AudioDeviceType) {
        if (currentDevice != deviceType) {
            when (deviceType) {
                AudioDeviceType.SPEAKER_PHONE -> {
                    audioManager.isSpeakerphoneOn = true
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                    audioManager.mode = AndroidAudioManager.MODE_IN_COMMUNICATION
                }
                AudioDeviceType.EARPIECE -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                    audioManager.mode = AndroidAudioManager.MODE_IN_COMMUNICATION
                }
                AudioDeviceType.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                    audioManager.mode = AndroidAudioManager.MODE_IN_COMMUNICATION
                }
                AudioDeviceType.WIRED_HEADSET -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                    audioManager.mode = AndroidAudioManager.MODE_IN_COMMUNICATION
                }
                AudioDeviceType.NONE -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                }
            }
            currentDevice = deviceType
            deviceCallbacks.forEach { it.onAudioDeviceChanged(deviceType) }
        }
    }

    override fun getCurrentAudioDevice(): AudioDeviceType = currentDevice

    override fun getAvailableAudioDevices(): List<AudioDeviceType> {
        val devices = mutableListOf<AudioDeviceType>()
        devices.add(AudioDeviceType.EARPIECE)
        devices.add(AudioDeviceType.SPEAKER_PHONE)
        if (audioManager.isWiredHeadsetOn) devices.add(AudioDeviceType.WIRED_HEADSET)
        if (audioManager.isBluetoothScoAvailableOffCall) devices.add(AudioDeviceType.BLUETOOTH)
        return devices
    }

    override fun registerAudioDeviceCallback(callback: AudioDeviceCallback) {
        deviceCallbacks.add(callback)
    }

    override fun unregisterAudioDeviceCallback(callback: AudioDeviceCallback) {
        deviceCallbacks.remove(callback)
    }

    private fun updateAudioDevices() {
        val availableDevices = getAvailableAudioDevices()
        if (availableDevices.isNotEmpty() && !availableDevices.contains(currentDevice)) {
            setAudioDevice(availableDevices.first())
        }
    }
}