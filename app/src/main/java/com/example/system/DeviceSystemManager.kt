package com.example.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.util.Log

class DeviceSystemManager(private val context: Context) {

    fun toggleFlashlight(enabled: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enabled)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Flashlight error", e)
            false
        }
    }

    fun adjustVolume(direction: String, levelPercent: Int?): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            when (direction.uppercase()) {
                "UP" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume increased to $currentPercent%"
                }
                "DOWN" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume decreased to $currentPercent%"
                }
                "MAX" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)
                    "Volume set to maximum"
                }
                "MUTE" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    "Volume muted"
                }
                "SET" -> {
                    if (levelPercent != null) {
                        val targetVol = (levelPercent.coerceIn(0, 100) * maxVolume) / 100
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        "Volume set to $levelPercent%"
                    } else {
                        "Specific volume level not provided"
                    }
                }
                else -> "Unknown volume action"
            }
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Volume adjust error", e)
            "Could not adjust volume: ${e.message}"
        }
    }

    fun getBatteryInfo(): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            val statusStr = if (isCharging) "charging" else "discharging"
            "Battery level is at $percent% and currently $statusStr"
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Battery info error", e)
            "Unable to fetch battery status"
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Alarm error", e)
            false
        }
    }

    fun setTimer(seconds: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Timer error", e)
            false
        }
    }

    fun sendWhatsAppMessage(phoneNumberOrName: String, messageText: String): Boolean {
        return try {
            val digits = phoneNumberOrName.filter { it.isDigit() }
            val uriString = if (digits.length >= 7) {
                "https://api.whatsapp.com/send?phone=$digits&text=${java.net.URLEncoder.encode(messageText, "UTF-8")}"
            } else {
                "https://api.whatsapp.com/send?text=${java.net.URLEncoder.encode(messageText, "UTF-8")}"
            }
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "WhatsApp intent error", e)
            false
        }
    }

    fun initiatePhoneCall(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:${phoneNumber.filter { it.isDigit() || it == '+' }}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Phone call intent error", e)
            false
        }
    }
}
