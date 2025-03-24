package com.williamfq.xhat.call.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioDeviceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentDevice = MutableStateFlow<AudioDevice>(AudioDevice.Earpiece)
    val currentDevice: StateFlow<AudioDevice> = _currentDevice

    private val _availableDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices

    init {
        updateAvailableDevices()
    }

    fun selectAudioDevice(device: AudioDevice) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val audioDeviceInfo = getAudioDeviceInfo(device)
            audioDeviceInfo?.let {
                audioManager.setCommunicationDevice(it)
                _currentDevice.value = device
            }
        } else {
            when (device) {
                AudioDevice.Bluetooth -> {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
                AudioDevice.Speaker -> {
                    audioManager.isSpeakerphoneOn = true
                    audioManager.isBluetoothScoOn = false
                }
                AudioDevice.Earpiece -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isBluetoothScoOn = false
                }
                AudioDevice.WiredHeadset -> {
                    audioManager.isSpeakerphoneOn = false
                    audioManager.isBluetoothScoOn = false
                }
            }
            _currentDevice.value = device
        }
    }

    private fun getAudioDeviceInfo(device: AudioDevice): AudioDeviceInfo? {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            when (device) {
                AudioDevice.Bluetooth -> it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                AudioDevice.WiredHeadset -> it.type in listOf(
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                )
                AudioDevice.Earpiece -> it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                AudioDevice.Speaker -> it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
        }
    }

    fun updateAvailableDevices() {
        val devices = mutableListOf<AudioDevice>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> devices.add(AudioDevice.Bluetooth)
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
                        devices.add(AudioDevice.WiredHeadset)
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> devices.add(AudioDevice.Earpiece)
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> devices.add(AudioDevice.Speaker)
                }
            }
        } else {
            devices.addAll(listOf(AudioDevice.Earpiece, AudioDevice.Speaker))
            if (audioManager.isWiredHeadsetOn) devices.add(AudioDevice.WiredHeadset)
            if (audioManager.isBluetoothScoAvailableOffCall) devices.add(AudioDevice.Bluetooth)
        }
        _availableDevices.value = devices.distinct()
    }

    fun cleanup() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        audioManager.apply {
            mode = AudioManager.MODE_NORMAL
            stopBluetoothSco()
            isBluetoothScoOn = false
            isSpeakerphoneOn = false
        }
    }
}

enum class AudioDevice {
    Earpiece, Speaker, WiredHeadset, Bluetooth
}