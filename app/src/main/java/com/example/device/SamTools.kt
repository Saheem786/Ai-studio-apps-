package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

sealed class SamToolResult(open val message: String) {
    data class Success(override val message: String, val data: Map<String, Any?> = emptyMap()) : SamToolResult(message)
    data class Failure(override val message: String, val error: String) : SamToolResult(message)
}

class SamTools(private val context: Context) {

    companion object {
        private const val TAG = "SamTools"

        fun getSamToolDeclarationsJson(): JSONArray {
            val funcs = JSONArray()

            // 1. open_app
            funcs.put(JSONObject().apply {
                put("name", "open_app")
                put("description", "Launches any installed app on the user's Android phone (e.g. YouTube, WhatsApp, Camera, Settings, Gallery, Calculator, Spotify, Chrome, Maps, Instagram).")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("appName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Name of the application to open (e.g. YouTube, WhatsApp, Camera, Settings, Gallery, Spotify, Maps, Calculator).")
                        })
                    })
                    put("required", JSONArray().put("appName"))
                })
            })

            // 2. send_whatsapp
            funcs.put(JSONObject().apply {
                put("name", "send_whatsapp")
                put("description", "Opens WhatsApp chat or share screen with a pre-filled message text to a contact or phone number.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Recipient contact name or phone number.")
                        })
                        put("message", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The message text to send.")
                        })
                    })
                    put("required", JSONArray().put("message"))
                })
            })

            // 3. set_timer_or_alarm
            funcs.put(JSONObject().apply {
                put("name", "set_timer_or_alarm")
                put("description", "Sets a native Android countdown timer or system clock alarm with a specified duration/time and label.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("timeMinutes", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Duration in minutes (for timer) or minute of the hour (for alarm).")
                        })
                        put("hour", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Hour in 24-hour format (0-23) if setting a specific alarm time.")
                        })
                        put("label", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Title or description for the alarm/timer.")
                        })
                        put("isAlarm", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "Set true for a specific alarm time, or false for a countdown timer.")
                        })
                    })
                    put("required", JSONArray().put("label"))
                })
            })

            // 4. device_status
            funcs.put(JSONObject().apply {
                put("name", "device_status")
                put("description", "Checks device hardware status: Battery percentage & charging state, Wi-Fi connection status & SSID, available internal storage, screen brightness, and audio volume.")
            })

            // 5. generate_image (Imagen 3)
            funcs.put(JSONObject().apply {
                put("name", "generate_image")
                put("description", "Generates an AI photo, illustration, wallpaper or artwork using Google Imagen 3. Call whenever the user asks for pictures, photos, or visual generation (e.g. 'photo do', 'billi ki image banao').")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("prompt", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Detailed descriptive visual prompt in English for Imagen 3.")
                        })
                        put("aspectRatio", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Aspect ratio: '1:1', '16:9', '9:16', '4:3', or '3:4'. Default is '1:1'.")
                        })
                    })
                    put("required", JSONArray().put("prompt"))
                })
            })

            // 6. toggle_flashlight
            funcs.put(JSONObject().apply {
                put("name", "toggle_flashlight")
                put("description", "Toggles the phone hardware torch/flashlight ON or OFF.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("enable", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "True to turn flashlight on, false to turn off.")
                        })
                    })
                    put("required", JSONArray().put("enable"))
                })
            })

            // 7. adjust_volume
            funcs.put(JSONObject().apply {
                put("name", "adjust_volume")
                put("description", "Controls media volume: UP, DOWN, MAX, MUTE, or SET level.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("action", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Action: UP, DOWN, MAX, MUTE, or SET.")
                        })
                        put("level", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Percentage level 0-100 (if action is SET).")
                        })
                    })
                    put("required", JSONArray().put("action"))
                })
            })

            // 8. make_call
            funcs.put(JSONObject().apply {
                put("name", "make_call")
                put("description", "Dials a phone number or opens dialer for a contact.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("phoneNumber", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Phone number to dial.")
                        })
                    })
                    put("required", JSONArray().put("phoneNumber"))
                })
            })

            return funcs
        }
    }

    /**
     * Executes native tool actions dispatched by Gemini Tool Calls
     */
    fun execute(toolName: String, args: Map<String, Any?>): SamToolResult {
        val normalizedName = toolName.trim()
        Log.d(TAG, "Executing native tool: $normalizedName with args: $args")

        return when (normalizedName) {
            "open_app", "openApp", "launchApp" -> {
                val appName = (args["appName"] ?: args["name"] ?: args["app"] ?: "").toString().trim()
                openApp(appName)
            }

            "send_whatsapp", "sendWhatsAppMessage", "openWhatsApp", "sendWhatsApp" -> {
                val contact = (args["contactName"] ?: args["recipient"] ?: args["phone"] ?: "").toString().trim()
                val message = (args["message"] ?: args["text"] ?: "Hello from Sam AI!").toString().trim()
                sendWhatsApp(contact, message)
            }

            "set_timer_or_alarm", "setAlarm", "setTimer", "createAlarm" -> {
                val timeMinutes = (args["timeMinutes"] as? Number)?.toInt()
                    ?: (args["minutes"] as? Number)?.toInt()
                    ?: (args["minute"] as? Number)?.toInt()
                val hour = (args["hour"] as? Number)?.toInt()
                val label = (args["label"] ?: args["message"] ?: "Sam AI Alert").toString().trim()
                val isAlarm = (args["isAlarm"] as? Boolean) ?: (hour != null)
                setTimerOrAlarm(timeMinutes, hour, label, isAlarm)
            }

            "device_status", "getBatteryStatus", "getDeviceStatus", "checkStatus" -> {
                getDeviceStatus()
            }

            "toggle_flashlight", "toggleFlashlight", "flashlight" -> {
                val enable = (args["enable"] as? Boolean) ?: true
                toggleFlashlight(enable)
            }

            "adjust_volume", "adjustVolume", "setVolume" -> {
                val action = (args["action"] ?: "UP").toString().trim().uppercase()
                val level = (args["level"] as? Number)?.toInt()
                adjustVolume(action, level)
            }

            "make_call", "makeCall", "dialContact", "callContact" -> {
                val phone = (args["phoneNumber"] ?: args["phone"] ?: args["number"] ?: "").toString().trim()
                makeCall(phone)
            }

            else -> {
                SamToolResult.Failure("Unknown tool: $toolName", "Unsupported tool action")
            }
        }
    }

    fun openApp(appName: String): SamToolResult {
        if (appName.isBlank()) {
            return SamToolResult.Failure("App name was not provided", "Missing appName argument")
        }

        val clean = appName.lowercase().trim()
            .replace(Regex("(?i)^(open|launch|start|kholo|chalao)\\s*"), "")
            .replace(Regex("(?i)\\s*(kholo|chalao|open karo|launch karo)$"), "")
            .trim()

        // YouTube Search Interception
        if (clean.contains("youtube") && clean.length > 7) {
            val query = clean.replace("youtube", "").replace("par", "").replace("pe", "")
                .replace("play", "").replace("search", "").replace("karo", "").trim()
            if (query.isNotBlank()) {
                return try {
                    val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage("com.google.android.youtube")
                        putExtra("query", query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(appIntent)
                    SamToolResult.Success("YouTube par '$query' play kar diya hai! 🎬")
                } catch (e: Exception) {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    SamToolResult.Success("YouTube web par '$query' search kar diya hai! 🎬")
                }
            }
        }

        // Direct package / intent mapping
        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "google" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "calculator" to "com.google.android.calculator",
            "camera" to "android.media.action.IMAGE_CAPTURE",
            "settings" to "android.settings.SETTINGS",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock"
        )

        val targetPackage = packageMap.entries.firstOrNull { clean.contains(it.key) || it.key.contains(clean) }?.value

        // Try launching by intent action (e.g. Camera, Settings)
        if (targetPackage == "android.media.action.IMAGE_CAPTURE") {
            return try {
                val camIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(camIntent)
                SamToolResult.Success("Camera open kar diya hai! 📷")
            } catch (e: Exception) {
                SamToolResult.Failure("Camera open karne mein error aaya", e.message ?: "")
            }
        }

        if (targetPackage == "android.settings.SETTINGS") {
            return try {
                val setIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(setIntent)
                SamToolResult.Success("Phone Settings open kar di hain! ⚙️")
            } catch (e: Exception) {
                SamToolResult.Failure("Settings open karne mein error aaya", e.message ?: "")
            }
        }

        // Try launch by specific package
        if (targetPackage != null) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return SamToolResult.Success("$appName open kar diya hai! 🚀")
            }
        }

        // Search installed applications matching name
        return try {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val match = installed.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                label.contains(clean) || clean.contains(label)
            }

            if (match != null) {
                val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return SamToolResult.Success("$appName successfully open ho gaya hai! 🚀")
                }
            }

            // Web fallback
            val webUrl = "https://www.google.com/search?q=${URLEncoder.encode(appName, "UTF-8")}"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            SamToolResult.Success("'$appName' ke liye web search open kar diya hai! 🌐")
        } catch (e: Exception) {
            SamToolResult.Failure("Failed to open '$appName': ${e.message}", e.message ?: "")
        }
    }

    fun sendWhatsApp(contactNameOrNumber: String, messageText: String): SamToolResult {
        return try {
            val digits = contactNameOrNumber.filter { it.isDigit() }
            val uriString = if (digits.length >= 7) {
                "https://api.whatsapp.com/send?phone=$digits&text=${URLEncoder.encode(messageText, "UTF-8")}"
            } else {
                "https://api.whatsapp.com/send?text=${URLEncoder.encode(messageText, "UTF-8")}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val targetStr = if (contactNameOrNumber.isNotBlank()) " to $contactNameOrNumber" else ""
            SamToolResult.Success("WhatsApp chat$targetStr open kar diya hai with pre-filled message! 💬")
        } catch (e: Exception) {
            SamToolResult.Failure("WhatsApp message dispatch failed", e.message ?: "")
        }
    }

    fun setTimerOrAlarm(timeMinutes: Int?, hour: Int?, label: String, isAlarm: Boolean): SamToolResult {
        return try {
            if (isAlarm && hour != null) {
                val targetMinute = timeMinutes ?: 0
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                    putExtra(AlarmClock.EXTRA_MINUTES, targetMinute.coerceIn(0, 59))
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                SamToolResult.Success("Alarm set ho gaya hai for %02d:%02d ('$label')! ⏰".format(hour, targetMinute))
            } else {
                val seconds = ((timeMinutes ?: 5) * 60).coerceAtLeast(10)
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                SamToolResult.Success("${timeMinutes ?: 5} minutes ka countdown timer start ho gaya hai ('$label')! ⏱️")
            }
        } catch (e: Exception) {
            SamToolResult.Failure("Failed to set timer/alarm: ${e.message}", e.message ?: "")
        }
    }

    fun getDeviceStatus(): SamToolResult {
        return try {
            // 1. Battery
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            val chargeStatus = if (isCharging) "Charging ⚡" else "Discharging 🔋"

            // 2. Wi-Fi & Network
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connMgr.activeNetwork
            val capabilities = connMgr.getNetworkCapabilities(activeNetwork)
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val netStatus = when {
                isWifi -> "Wi-Fi Connected 📶"
                isCellular -> "Mobile Data (Cellular) 📱"
                capabilities != null -> "Connected 🌐"
                else -> "Offline ❌"
            }

            // 3. Storage
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val freeGb = bytesAvailable / (1024 * 1024 * 1024)
            val totalGb = bytesTotal / (1024 * 1024 * 1024)

            // 4. Audio Volume
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volPct = (currentVol * 100) / maxVol.coerceAtLeast(1)

            // 5. Screen Brightness
            val brightnessVal = try {
                val b = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                (b * 100) / 255
            } catch (se: Exception) {
                50
            }

            val statusReport = "📱 Device Status Report:\n• Battery: $batteryPct% ($chargeStatus)\n• Network: $netStatus\n• Storage: ${freeGb}GB Free / ${totalGb}GB Total\n• Brightness: $brightnessVal%\n• Volume: $volPct%"

            SamToolResult.Success(
                message = statusReport,
                data = mapOf(
                    "battery" to batteryPct,
                    "isCharging" to isCharging,
                    "network" to netStatus,
                    "freeStorageGb" to freeGb,
                    "totalStorageGb" to totalGb,
                    "brightnessPercent" to brightnessVal,
                    "volumePercent" to volPct
                )
            )
        } catch (e: Exception) {
            SamToolResult.Failure("Unable to retrieve device status", e.message ?: "")
        }
    }

    fun toggleFlashlight(enabled: Boolean): SamToolResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return SamToolResult.Failure("No camera flash available", "Missing hardware")
            cameraManager.setTorchMode(cameraId, enabled)
            val status = if (enabled) "ON 🔦" else "OFF"
            SamToolResult.Success("Flashlight turn $status kar di hai!")
        } catch (e: Exception) {
            SamToolResult.Failure("Flashlight toggle failed", e.message ?: "")
        }
    }

    fun adjustVolume(direction: String, levelPercent: Int?): SamToolResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            val msg = when (direction.uppercase()) {
                "UP" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume increased to $currentPercent% 🔊"
                }
                "DOWN" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume decreased to $currentPercent% 🔉"
                }
                "MAX" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)
                    "Volume set to maximum (100%) 🔊"
                }
                "MUTE" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    "Volume muted (0%) 🔇"
                }
                "SET" -> {
                    if (levelPercent != null) {
                        val targetVol = (levelPercent.coerceIn(0, 100) * maxVolume) / 100
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        "Volume set to $levelPercent% 🎚️"
                    } else {
                        "Specific volume level not provided"
                    }
                }
                else -> "Volume adjusted"
            }
            SamToolResult.Success(msg)
        } catch (e: Exception) {
            SamToolResult.Failure("Volume adjustment failed", e.message ?: "")
        }
    }

    fun makeCall(phoneNumber: String): SamToolResult {
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SamToolResult.Success("Dialer opened for $cleanPhone 📞")
        } catch (e: Exception) {
            SamToolResult.Failure("Failed to initiate call", e.message ?: "")
        }
    }
}
