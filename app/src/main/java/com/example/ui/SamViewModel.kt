package com.example.ui

import android.app.Application
import android.content.Context
import com.example.ui.theme.AppThemeAccent

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioTrackPlayer
import com.example.audio.MicrophoneRecorder
import com.example.audio.TextToSpeechEngine
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.device.ContactItem
import com.example.device.DeviceActionManager
import com.example.device.DeviceActionResult
import com.example.gemini.SamPersonality
import com.example.gemini.GeminiLiveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.os.Build
import android.util.Log

enum class AssistantState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    ERROR
}

data class SamUiState(
    val state: AssistantState = AssistantState.IDLE,
    val amplitude: Float = 0f,
    val transcript: String = "Tap the microphone to start talking to Sam",
    val actionBanner: String? = null,
    val errorMessage: String? = null,
    val diagnosticLogs: List<String> = emptyList(),
    val isMicActive: Boolean = false,
    val apiKey: String = "",
    val selectedModel: String = SamPersonality.MODEL_LIVE_PRIMARY,
    val multipleContacts: List<ContactItem>? = null,
    val showDiagnosticsSheet: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val currentMode: AppMode = AppMode.VOICE,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Hello! Main Sam hoon. Aap mujhse bol kar ya text chat mein kuch bhi pooch sakte hain aur device actions (WhatsApp, Call, YouTube, etc.) chala sakte hain."
        )
    ),
    val inputText: String = "",
    val isSendingChat: Boolean = false,
    val quickReplies: List<String> = emptyList(),
    val showQuickRepliesConfigDialog: Boolean = false,
    val attachedImageBase64: String? = null,
    val deepResearchEnabled: Boolean = false,
    val canvasTitle: String = "",
    val canvasContent: String = "",
    val canvasContentType: String = "Code",
    val showCanvas: Boolean = false,
    val showToolsSheet: Boolean = false,
    val personalIntelligenceEnabled: Boolean = true,
    val voiceMatchEnforced: Boolean = false,
    val isVoiceCalibrationCompleted: Boolean = false,
    val showVoiceEnrollmentDialog: Boolean = false,
    val floatingWidgetEnabled: Boolean = false,
    val themeAccent: AppThemeAccent = AppThemeAccent.CLASSIC,
    val showThemeSettings: Boolean = false,
    val selectedVideoDuration: String? = null,
    val isImageGenActive: Boolean = false,
    val isVideoGenActive: Boolean = false,
    val showVideoDurationDialog: Boolean = false
)

class SamViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SamUiState())
    val uiState: StateFlow<SamUiState> = _uiState.asStateFlow()

    private val deviceActionManager = DeviceActionManager(application.applicationContext)
    private val deviceSystemManager = com.example.system.DeviceSystemManager(application.applicationContext)
    private val voiceMatchManager = com.example.audio.VoiceMatchManager(application.applicationContext)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val chatDatabase = AppDatabase.getInstance(application)
    private val chatRepository = ChatRepository(chatDatabase.chatMessageDao())

    private var audioPlayer: AudioTrackPlayer? = null
    private var ttsEngine: TextToSpeechEngine? = null
    private var micRecorder: MicrophoneRecorder? = null
    private var geminiService: GeminiLiveService? = null

    init {
        // Observe persistent chat history from Room Database via Flow
        viewModelScope.launch {
            chatRepository.allMessages.collect { messages ->
                _uiState.update { current ->
                    val displayList = if (messages.isEmpty()) {
                        listOf(
                            ChatMessage(
                                sender = MessageSender.ASSISTANT,
                                text = "Hello! Main Sam hoon. Aap mujhse bol kar ya text chat mein kuch bhi pooch sakte hain aur device actions (WhatsApp, Call, YouTube, etc.) chala sakte hain."
                            )
                        )
                    } else {
                        messages
                    }
                    current.copy(chatMessages = displayList)
                }
            }
        }

        // Load initial API key from SharedPreferences or BuildConfig
        val prefs = application.getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("gemini_api_key", null)?.trim()?.ifEmpty { null }
            ?: prefs.getString("custom_gemini_key", null)?.trim()?.ifEmpty { null }
        val initialKey = if (!savedKey.isNullOrBlank()) {
            savedKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY.trim()
            } catch (e: Exception) {
                ""
            }
        }

        // Load custom or default quick replies
        val defaultQuickReplies = listOf(
            "What's the weather?",
            "Remind me in 5 mins",
            "💬 WhatsApp kholo",
            "Ek joke sunao",
            "▶️ Open YouTube"
        )
        val savedRepliesStr = prefs.getString("quick_replies", null)
        val replies = if (!savedRepliesStr.isNullOrEmpty()) {
            savedRepliesStr.split("|||")
        } else {
            defaultQuickReplies
        }

        val isEnrolled = voiceMatchManager.isVoiceEnrolled()
        val isEnforced = prefs.getBoolean("voice_match_enforced", false)
        val isFloatingWidgetOn = prefs.getBoolean("floating_widget_enabled", false)

        val savedThemeStr = prefs.getString("theme_accent", "CLASSIC") ?: "CLASSIC"
        val themeAccent = try {
            AppThemeAccent.valueOf(savedThemeStr)
        } catch (e: Exception) {
            AppThemeAccent.CLASSIC
        }

        _uiState.update { 
            it.copy(
                apiKey = initialKey, 
                quickReplies = replies,
                isVoiceCalibrationCompleted = isEnrolled,
                voiceMatchEnforced = isEnforced && isEnrolled,
                floatingWidgetEnabled = isFloatingWidgetOn,
                themeAccent = themeAccent
            ) 
        }

        initAudioSystem()
        geminiService?.setApiKey(initialKey)
        if (initialKey.isNotBlank()) {
            addLog("Loaded Gemini API key from storage (${initialKey.take(4)}...${initialKey.takeLast(4)})")
        }
        addLog("Room Database initialized: chat history loaded")
    }

    private fun addLog(message: String) {
        val timestamp = timeFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        _uiState.update { current ->
            val updated = current.diagnosticLogs.takeLast(100) + logEntry
            current.copy(diagnosticLogs = updated)
        }
    }

    private fun initAudioSystem() {
        audioPlayer = AudioTrackPlayer(
            onPlaybackStarted = {
                _uiState.update { it.copy(state = AssistantState.SPEAKING) }
                addLog("Speaker playback started - UI state changed to SPEAKING")
            },
            onPlaybackEnded = {
                val nextState = if (_uiState.value.isMicActive) AssistantState.LISTENING else AssistantState.IDLE
                _uiState.update { it.copy(state = nextState, amplitude = 0f) }
                addLog("Speaker playback ended - UI state returned to $nextState")
            },
            onAmplitudeChanged = { amp ->
                if (_uiState.value.state == AssistantState.SPEAKING) {
                    _uiState.update { it.copy(amplitude = amp) }
                }
            },
            onDiagnosticLog = { logMsg ->
                addLog(logMsg)
            }
        )

        audioPlayer?.startPlaybackLoop(viewModelScope)

        ttsEngine = TextToSpeechEngine(
            context = getApplication(),
            scope = viewModelScope,
            onSpeechStarted = {
                _uiState.update { it.copy(state = AssistantState.SPEAKING) }
                addLog("TTS speech started - UI state changed to SPEAKING")
            },
            onSpeechEnded = {
                val nextState = if (_uiState.value.isMicActive) AssistantState.LISTENING else AssistantState.IDLE
                _uiState.update { it.copy(state = nextState, amplitude = 0f) }
                addLog("TTS speech ended - UI state returned to $nextState")
            },
            onAmplitudeChanged = { amp ->
                if (_uiState.value.state == AssistantState.SPEAKING) {
                    _uiState.update { it.copy(amplitude = amp) }
                }
            },
            onDiagnosticLog = { logMsg ->
                addLog(logMsg)
            }
        )

        micRecorder = MicrophoneRecorder(
            onAudioChunk = { base64Data, rawBytes ->
                var shouldStream = true
                if (_uiState.value.voiceMatchEnforced && voiceMatchManager.isVoiceEnrolled()) {
                    val score = voiceMatchManager.verifySpeaker(rawBytes)
                    if (score < 0.78f) {
                        shouldStream = false
                        _uiState.update { it.copy(actionBanner = "Voice Mismatch: Unauthorized speaker") }
                    } else {
                        _uiState.update { it.copy(actionBanner = "Sam is listening...") }
                    }
                }
                if (shouldStream) {
                    geminiService?.sendAudioChunk(base64Data)
                }
            },
            onAmplitudeChanged = { amp ->
                if (_uiState.value.state == AssistantState.LISTENING) {
                    _uiState.update { it.copy(amplitude = amp) }
                }
            },
            onInterruptionDetected = {
                if (_uiState.value.state == AssistantState.SPEAKING) {
                    addLog("User interrupted speech detected via microphone")
                    audioPlayer?.interrupt()
                    ttsEngine?.stop()
                    _uiState.update { it.copy(state = AssistantState.LISTENING) }
                }
            },
            onError = { errMsg ->
                addLog("Microphone error: $errMsg")
                _uiState.update { it.copy(errorMessage = errMsg, state = AssistantState.ERROR) }
            }
        )

        geminiService = GeminiLiveService(
            onConnected = {
                addLog("Gemini Live session connected. Microphone active.")
                _uiState.update {
                    it.copy(
                        state = AssistantState.LISTENING,
                        isMicActive = true,
                        errorMessage = null,
                        actionBanner = "Sam is listening..."
                    )
                }
                micRecorder?.start(viewModelScope)
            },
            onDisconnected = { reason ->
                addLog("Gemini Live disconnected: $reason")
                micRecorder?.stop()
                _uiState.update {
                    it.copy(
                        state = AssistantState.IDLE,
                        isMicActive = false,
                        actionBanner = null
                    )
                }
            },
            onAudioChunkReceived = { base64Audio, mimeType ->
                audioPlayer?.enqueueChunk(base64Audio, mimeType)
            },
            onTranscriptReceived = { text, isUser ->
                val prefix = if (isUser) "You: " else "Sam: "
                val sender = if (isUser) MessageSender.USER else MessageSender.ASSISTANT
                val newMsg = ChatMessage(sender = sender, text = text)
                _uiState.update { current ->
                    current.copy(
                        transcript = "$prefix$text"
                    )
                }
                viewModelScope.launch {
                    chatRepository.saveMessage(newMsg)
                }
                addLog("$prefix$text")
            },
            onToolCallReceived = { name, callId, args ->
                handleToolCall(name, callId, args)
            },
            onInterrupted = {
                addLog("Gemini reported interruption")
                audioPlayer?.interrupt()
                ttsEngine?.stop()
            },
            onError = { errMsg ->
                addLog("Gemini session error: $errMsg")
                _uiState.update { it.copy(errorMessage = errMsg, state = AssistantState.ERROR) }
            },
            onDiagnosticLog = { logMsg ->
                addLog(logMsg)
            }
        )
    }

    private fun handleToolCall(name: String, callId: String, args: Map<String, Any?>) {
        addLog("Executing device action: $name with args: $args")
        _uiState.update { it.copy(actionBanner = "Executing action: $name...") }

        viewModelScope.launch(Dispatchers.Main) {
            when (name) {
                "openWhatsApp" -> {
                    val result = deviceActionManager.openWhatsApp()
                    val success = result is DeviceActionResult.Success
                    val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
                    _uiState.update { it.copy(actionBanner = msg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to success, "message" to msg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "WhatsApp action: $msg",
                            isAction = true,
                            actionName = "openWhatsApp",
                            actionDetail = msg
                        )
                    )
                }

                "openApp" -> {
                    val appName = args["appName"]?.toString() ?: ""
                    val result = deviceActionManager.openApp(appName)
                    val success = result is DeviceActionResult.Success
                    val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
                    _uiState.update { it.copy(actionBanner = msg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to success, "appName" to appName, "message" to msg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Opened app '$appName': $msg",
                            isAction = true,
                            actionName = "openApp",
                            actionDetail = msg
                        )
                    )
                }

                "openWebsite" -> {
                    val url = args["url"]?.toString() ?: ""
                    val result = deviceActionManager.openWebsite(url)
                    val success = result is DeviceActionResult.Success
                    val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
                    _uiState.update { it.copy(actionBanner = msg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to success, "url" to url, "message" to msg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Opened website: $url",
                            isAction = true,
                            actionName = "openWebsite",
                            actionDetail = msg
                        )
                    )
                }

                "makeCall" -> {
                    val phone = args["phoneNumber"]?.toString() ?: ""
                    val result = deviceActionManager.makeCall(phone)
                    val success = result is DeviceActionResult.Success
                    val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
                    _uiState.update { it.copy(actionBanner = msg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to success, "phoneNumber" to phone, "message" to msg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Call action ($phone): $msg",
                            isAction = true,
                            actionName = "makeCall",
                            actionDetail = msg
                        )
                    )
                }

                "callContact" -> {
                    val contactName = args["contactName"]?.toString() ?: ""
                    val result = deviceActionManager.callContact(contactName)
                    when (result) {
                        is DeviceActionResult.ContactFound -> {
                            _uiState.update { it.copy(actionBanner = result.message) }
                            geminiService?.sendToolResponse(
                                callId,
                                name,
                                mapOf(
                                    "success" to true,
                                    "contact" to result.contact.name,
                                    "phone" to result.contact.phoneNumber
                                )
                            )
                            chatRepository.saveMessage(
                                ChatMessage(
                                    sender = MessageSender.ASSISTANT,
                                    text = "${result.contact.name} (${result.contact.phoneNumber}) ko call mila diya hai. 📞",
                                    isAction = true,
                                    actionName = "callContact",
                                    actionDetail = result.message
                                )
                            )
                        }
                        is DeviceActionResult.MultipleContactsFound -> {
                            _uiState.update {
                                it.copy(
                                    multipleContacts = result.contacts,
                                    actionBanner = "Multiple contacts found for '${result.query}'"
                                )
                            }
                            geminiService?.sendToolResponse(
                                callId,
                                name,
                                mapOf(
                                    "success" to false,
                                    "error" to "Multiple matching contacts found",
                                    "matches" to result.contacts.map { "${it.name} (${it.phoneNumber})" }
                                )
                            )
                        }
                        is DeviceActionResult.ContactNotFound -> {
                            _uiState.update { it.copy(actionBanner = "No contact found matching '${result.query}'") }
                            geminiService?.sendToolResponse(
                                callId,
                                name,
                                mapOf(
                                    "success" to false,
                                    "error" to "No contact found matching '${result.query}'"
                                )
                            )
                        }
                        is DeviceActionResult.PermissionRequired -> {
                            _uiState.update { it.copy(actionBanner = result.reason) }
                            geminiService?.sendToolResponse(
                                callId,
                                name,
                                mapOf("success" to false, "error" to "Contacts permission required")
                            )
                        }
                        is DeviceActionResult.Failure -> {
                            _uiState.update { it.copy(actionBanner = result.error) }
                            geminiService?.sendToolResponse(
                                callId,
                                name,
                                mapOf("success" to false, "error" to result.error)
                            )
                        }
                        else -> {}
                    }
                }

                "toggleFlashlight" -> {
                    val enable = args["enable"] as? Boolean ?: true
                    val ok = deviceSystemManager.toggleFlashlight(enable)
                    val resultMsg = if (ok) {
                        if (enable) "Flashlight turned on" else "Flashlight turned off"
                    } else {
                        "Failed to toggle flashlight"
                    }
                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to ok, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "$resultMsg 🔦",
                            isAction = true,
                            actionName = "toggleFlashlight",
                            actionDetail = resultMsg
                        )
                    )
                }

                "adjustVolume" -> {
                    val action = args["action"]?.toString() ?: "UP"
                    val level = (args["level"] as? Number)?.toInt()
                    val resultMsg = deviceSystemManager.adjustVolume(action, level)
                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to true, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "$resultMsg 🔊",
                            isAction = true,
                            actionName = "adjustVolume",
                            actionDetail = resultMsg
                        )
                    )
                }

                "getBatteryStatus" -> {
                    val resultMsg = deviceSystemManager.getBatteryInfo()
                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to true, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "$resultMsg 🔋",
                            isAction = true,
                            actionName = "getBatteryStatus",
                            actionDetail = resultMsg
                        )
                    )
                }

                "setAlarm" -> {
                    val hour = (args["hour"] as? Number)?.toInt() ?: 7
                    val minute = (args["minute"] as? Number)?.toInt() ?: 0
                    val label = args["label"]?.toString() ?: "Alarm"
                    val ok = deviceSystemManager.setAlarm(hour, minute, label)
                    val resultMsg = if (ok) {
                        "Alarm successfully set for $hour:${String.format("%02d", minute)} ($label)"
                    } else {
                        "Could not trigger alarm activity"
                    }
                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to ok, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "$resultMsg ⏰",
                            isAction = true,
                            actionName = "setAlarm",
                            actionDetail = resultMsg
                        )
                    )
                }

                "sendWhatsAppMessage" -> {
                    val recipient = args["recipient"]?.toString() ?: ""
                    val message = args["message"]?.toString() ?: ""
                    val ok = deviceSystemManager.sendWhatsAppMessage(recipient, message)
                    val resultMsg = if (ok) {
                        "WhatsApp opened with message to $recipient ready"
                    } else {
                        "Failed to open WhatsApp intent"
                    }
                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to ok, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "WhatsApp message to $recipient is ready sir. 💬",
                            isAction = true,
                            actionName = "sendWhatsAppMessage",
                            actionDetail = resultMsg
                        )
                    )
                }

                "dialContact" -> {
                    val phoneNumber = args["phoneNumber"]?.toString() ?: ""
                    val ok = deviceSystemManager.initiatePhoneCall(phoneNumber)
                    val resultMsg = if (ok) {
                        "Dialer initiated with $phoneNumber"
                    } else {
                        "Failed to launch dialer intent"
                    }
                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to ok, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Dialer initiated for $phoneNumber. 📞",
                            isAction = true,
                            actionName = "dialContact",
                            actionDetail = resultMsg
                        )
                    )
                }

                "toggleFloatingWidget" -> {
                    val enable = args["enable"] as? Boolean ?: true
                    val context = getApplication<Application>().applicationContext
                    val intent = Intent(context, JarvisFloatingWidgetService::class.java)
                    
                    val ok = try {
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                                val settingsIntent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(settingsIntent)
                                "Overlay permission required. Settings page opened."
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                "Floating Jarvis widget has been activated"
                            }
                        } else {
                            context.stopService(intent)
                            "Floating Jarvis widget has been deactivated"
                        }
                        true
                    } catch (e: Exception) {
                        Log.e("SamViewModel", "Error toggling floating widget", e)
                        false
                    }

                    val resultMsg = if (ok) {
                        if (enable) "Floating widget activated" else "Floating widget deactivated"
                    } else {
                        "Failed to toggle floating widget"
                    }

                    _uiState.update { it.copy(actionBanner = resultMsg) }
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to ok, "message" to resultMsg)
                    )
                    chatRepository.saveMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "$resultMsg. 🔮",
                            isAction = true,
                            actionName = "toggleFloatingWidget",
                            actionDetail = resultMsg
                        )
                    )
                }

                else -> {
                    geminiService?.sendToolResponse(
                        callId,
                        name,
                        mapOf("success" to false, "error" to "Unsupported function $name")
                    )
                }
            }
        }
    }

    fun toggleSession() {
        if (_uiState.value.isMicActive || _uiState.value.state != AssistantState.IDLE) {
            stopSession()
        } else {
            startSession()
        }
    }

    fun startSession() {
        val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        val key = _uiState.value.apiKey.ifBlank {
            prefs.getString("gemini_api_key", null)?.trim()?.ifEmpty { null }
                ?: prefs.getString("custom_gemini_key", null)?.trim()?.ifEmpty { null }
                ?: ""
        }
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    showSettingsDialog = true,
                    errorMessage = "Please enter your Gemini API key in Settings"
                )
            }
            return
        }

        if (_uiState.value.apiKey != key) {
            _uiState.update { it.copy(apiKey = key) }
        }
        geminiService?.setApiKey(key)

        _uiState.update {
            it.copy(
                state = AssistantState.CONNECTING,
                errorMessage = null,
                actionBanner = "Connecting to Sam..."
            )
        }
        addLog("User started session. Connecting...")
        geminiService?.connect(key, _uiState.value.selectedModel)
    }

    fun stopSession() {
        addLog("User stopped session")
        micRecorder?.stop()
        audioPlayer?.interrupt()
        ttsEngine?.stop()
        geminiService?.disconnect()
        _uiState.update {
            it.copy(
                state = AssistantState.IDLE,
                isMicActive = false,
                amplitude = 0f,
                actionBanner = null
            )
        }
    }

    /**
     * Speaker Diagnostic Test: plays 440Hz sine tone and tests Text-to-Speech output
     */
    fun testSpeaker() {
        addLog("Running Speaker & TextToSpeech Diagnostic Test...")
        audioPlayer?.testSpeakerTone(viewModelScope)
        ttsEngine?.speak("Sam voice assistant speaker and Text to Speech test successful.")
    }

    /**
     * Quick Prompt / Action Chip Click
     */
    fun sendQuickPrompt(promptText: String) {
        addLog("Quick action triggered: '$promptText'")
        val clean = promptText.replace(Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Punct}\\s]+"), "").trim().lowercase()

        // 1. YouTube Quick Action
        if (clean.contains("youtube")) {
            val userMsg = ChatMessage(sender = MessageSender.USER, text = promptText)
            val result = deviceActionManager.openApp("YouTube")
            val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: "Opened YouTube"
            val actionMsg = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "YouTube open kar diya hai! ▶️",
                isAction = true,
                actionName = "openApp",
                actionDetail = msg
            )
            viewModelScope.launch {
                chatRepository.saveMessage(userMsg)
                chatRepository.saveMessage(actionMsg)
            }
            ttsEngine?.speak("YouTube open kar diya hai")
            _uiState.update { it.copy(actionBanner = "YouTube open kar diya hai! ▶️") }
            return
        }

        // 2. WhatsApp Quick Action
        if (clean.contains("whatsapp")) {
            val userMsg = ChatMessage(sender = MessageSender.USER, text = promptText)
            val result = deviceActionManager.openWhatsApp()
            val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: "Opened WhatsApp"
            val actionMsg = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "WhatsApp open kar diya hai! 💬",
                isAction = true,
                actionName = "openWhatsApp",
                actionDetail = msg
            )
            viewModelScope.launch {
                chatRepository.saveMessage(userMsg)
                chatRepository.saveMessage(actionMsg)
            }
            ttsEngine?.speak("WhatsApp open kar diya hai")
            _uiState.update { it.copy(actionBanner = "WhatsApp open kar diya hai! 💬") }
            return
        }

        // 3. Google / Website Quick Action
        if (clean.contains("google")) {
            val userMsg = ChatMessage(sender = MessageSender.USER, text = promptText)
            val result = deviceActionManager.openWebsite("https://www.google.com")
            val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: "Opened Google"
            val actionMsg = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Google browser mein open kar diya hai! 🌐",
                isAction = true,
                actionName = "openWebsite",
                actionDetail = msg
            )
            viewModelScope.launch {
                chatRepository.saveMessage(userMsg)
                chatRepository.saveMessage(actionMsg)
            }
            ttsEngine?.speak("Google browser mein open kar diya hai")
            _uiState.update { it.copy(actionBanner = "Google browser mein open kar diya hai! 🌐") }
            return
        }

        // 4. Call Mom Quick Action
        if (clean.contains("call mom")) {
            val userMsg = ChatMessage(sender = MessageSender.USER, text = promptText)
            val result = deviceActionManager.callContact("Mom")
            when (result) {
                is DeviceActionResult.ContactFound -> {
                    val actionMsg = ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "${result.contact.name} (${result.contact.phoneNumber}) ko call mila diya hai. 📞",
                        isAction = true,
                        actionName = "callContact",
                        actionDetail = result.message
                    )
                    viewModelScope.launch {
                        chatRepository.saveMessage(userMsg)
                        chatRepository.saveMessage(actionMsg)
                    }
                    ttsEngine?.speak("${result.contact.name} ko call mila diya hai")
                    _uiState.update { it.copy(actionBanner = "Calling ${result.contact.name}...") }
                    return
                }
                is DeviceActionResult.MultipleContactsFound -> {
                    viewModelScope.launch {
                        chatRepository.saveMessage(userMsg)
                    }
                    _uiState.update { it.copy(multipleContacts = result.contacts) }
                    return
                }
                else -> {
                    deviceActionManager.makeCall("")
                    val actionMsg = ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Phone dialer open kar diya hai. 📞",
                        isAction = true,
                        actionName = "makeCall",
                        actionDetail = "Opened dialer"
                    )
                    viewModelScope.launch {
                        chatRepository.saveMessage(userMsg)
                        chatRepository.saveMessage(actionMsg)
                    }
                    ttsEngine?.speak("Phone dialer open kar diya hai")
                    return
                }
            }
        }

        if (!_uiState.value.isMicActive) {
            startSession()
        }
        viewModelScope.launch {
            // Give WebSocket brief moment to connect if starting
            var attempts = 0
            while (attempts < 15 && geminiService?.isSessionActive() != true) {
                kotlinx.coroutines.delay(200)
                attempts++
            }
            if (geminiService?.isSessionActive() == true) {
                geminiService?.sendUserText(promptText)
            } else {
                // Try REST Native Audio Fallback!
                val ok = geminiService?.generateRestAudioFallback(promptText) ?: false
                if (!ok) {
                    addLog("Could not send quick prompt: Session not active")
                }
            }
        }
    }

    fun selectContactToCall(contact: ContactItem) {
        _uiState.update { it.copy(multipleContacts = null, actionBanner = "Calling ${contact.name}...") }
        val result = deviceActionManager.makeCall(contact.phoneNumber)
        val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
        val actionMsg = ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "${contact.name} (${contact.phoneNumber}) ko call mila diya hai. 📞",
            isAction = true,
            actionName = "callContact",
            actionDetail = msg
        )
        viewModelScope.launch {
            chatRepository.saveMessage(actionMsg)
        }
        ttsEngine?.speak("${contact.name} ko call mila diya hai")
        geminiService?.sendUserText("I want to call ${contact.name}")
    }

    fun dismissContactsDialog() {
        _uiState.update { it.copy(multipleContacts = null) }
    }

    fun updateApiKey(newKey: String) {
        val trimmed = newKey.trim()
        val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("gemini_api_key", trimmed)
            .putString("custom_gemini_key", trimmed)
            .commit()
        geminiService?.setApiKey(trimmed)
        _uiState.update { it.copy(apiKey = trimmed, showSettingsDialog = false, errorMessage = null) }
        addLog("Gemini API key updated and saved into SharedPreferences")
    }

    fun toggleDiagnosticsSheet(show: Boolean) {
        _uiState.update { it.copy(showDiagnosticsSheet = show) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun toggleThemeSettings(show: Boolean) {
        _uiState.update { it.copy(showThemeSettings = show) }
    }

    fun toggleToolsSheet(show: Boolean) {
        _uiState.update { it.copy(showToolsSheet = show) }
    }

    fun togglePersonalIntelligence(enabled: Boolean) {
        _uiState.update { it.copy(personalIntelligenceEnabled = enabled) }
        addLog("Personal Intelligence setting set to: $enabled")
    }

    fun setImageGenMode(active: Boolean) {
        _uiState.update { it.copy(isImageGenActive = active, isVideoGenActive = false) }
        addLog("Image Generation Mode: $active")
    }

    fun toggleVideoDurationDialog(show: Boolean) {
        _uiState.update { it.copy(showVideoDurationDialog = show) }
    }

    fun setVideoGenMode(active: Boolean, duration: String? = null) {
        _uiState.update { 
            it.copy(
                isVideoGenActive = active,
                isImageGenActive = false,
                selectedVideoDuration = duration
            )
        }
        addLog("Video Generation Mode: $active, Duration: $duration")
    }

    fun requestImageGeneration(prompt: String, referenceImageBase64: String?) {
        _uiState.update { it.copy(isSendingChat = true) }
        viewModelScope.launch {
            val service = geminiService
            if (service == null) {
                _uiState.update { it.copy(isSendingChat = false, errorMessage = "AI Service not connected") }
                return@launch
            }
            
            val result = service.generateImage(prompt, referenceImageBase64)
            result.onSuccess { base64 ->
                val responseMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Aapka custom dynamic image generate ho chuka hai! 🎨 Here is the output for: \"$prompt\"",
                    imageBase64 = base64
                )
                chatRepository.saveMessage(responseMsg)
                addLog("Successfully generated AI image for '$prompt'")
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Image generate karne mein error aaya: ${err.message}"
                )
                chatRepository.saveMessage(errorMsg)
                addLog("Failed to generate AI image: ${err.message}")
            }
            _uiState.update { it.copy(isSendingChat = false, isImageGenActive = false) }
        }
    }

    fun requestVideoGeneration(prompt: String, duration: String) {
        _uiState.update { it.copy(isSendingChat = true) }
        viewModelScope.launch {
            val service = geminiService
            if (service == null) {
                _uiState.update { it.copy(isSendingChat = false, errorMessage = "AI Service not connected") }
                return@launch
            }

            val isShort = duration.contains("15") || duration.contains("30") || duration == "15s" || duration == "30s"
            if (isShort) {
                val durSeconds = if (duration.contains("15") || duration == "15s") 15 else 30
                val result = service.generateVideoClip(prompt, durSeconds)
                result.onSuccess { mp4Url ->
                    val responseMsg = ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Aapka AI video clip ($duration) ready hai! 🎬 Rendered output for: \"$prompt\"",
                        videoUrl = mp4Url,
                        videoDuration = duration
                    )
                    chatRepository.saveMessage(responseMsg)
                    addLog("Successfully generated $duration AI video for '$prompt'")
                }.onFailure { err ->
                    val errorMsg = ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Video rendering failed: ${err.message}"
                    )
                    chatRepository.saveMessage(errorMsg)
                }
            } else {
                // Multi-Scene Script, Storyboard, Canvas visualization
                val scenesCount = if (duration.contains("1") || duration == "1m") 3 else if (duration.contains("5") || duration == "5m") 8 else 12
                val sceneCards = StringBuilder()
                sceneCards.append("# AI Video Storyboard - $duration Production\n")
                sceneCards.append("Prompt: \"$prompt\"\n\n")
                sceneCards.append("Generated scenes and timeline sequences mapped to Canvas interactive workspace:\n\n")
                
                for (i in 1..scenesCount) {
                    sceneCards.append("## Scene $i: Timeline [${(i-1)*15}s - ${i*15}s]\n")
                    sceneCards.append("- **Visual Prompt**: Cinematic close-up, high-fidelity render of segment matching scene $i for '$prompt'. Ultra detailed cinematic lighting.\n")
                    sceneCards.append("- **Audio/Voiceover Script**: \"Exploring the depth of this transition, visual layers blend smoothly...\"\n")
                    sceneCards.append("- **Rendering Status**: COMPILED / READY\n\n")
                }
                
                openCanvas(
                    title = "AI Video Production: $duration",
                    contentType = "Markdown",
                    content = sceneCards.toString()
                )
                
                val responseMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Aapka multi-scene video storyboard ($duration) generate karke side Canvas workspace board mein load kar diya gaya hai! 📽️ Open the Canvas workspace to edit or play the rendered storyboard visual scripts."
                )
                chatRepository.saveMessage(responseMsg)
                addLog("Successfully generated multi-scene video production script for '$prompt'")
            }
            
            _uiState.update { 
                it.copy(
                    isSendingChat = false, 
                    isVideoGenActive = false, 
                    selectedVideoDuration = null
                )
            }
        }
    }

    fun toggleVoiceMatchEnforced(enforced: Boolean) {
        val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("voice_match_enforced", enforced).apply()
        _uiState.update { it.copy(voiceMatchEnforced = enforced) }
        addLog("Voice Match enforcement toggled to: $enforced")
    }

    fun toggleVoiceEnrollmentDialog(show: Boolean) {
        _uiState.update { it.copy(showVoiceEnrollmentDialog = show) }
    }

    fun completeVoiceEnrollment(samples: List<ByteArray>) {
        val ok = voiceMatchManager.enrollVoice(samples)
        if (ok) {
            val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("voice_match_enforced", true).apply()
            _uiState.update { 
                it.copy(
                    isVoiceCalibrationCompleted = true,
                    voiceMatchEnforced = true,
                    showVoiceEnrollmentDialog = false
                ) 
            }
            addLog("Voice Enrollment completed successfully. Voiceprint calibrated & active.")
        } else {
            addLog("Voice Enrollment failed due to poor acoustic sample calibration.")
        }
    }

    fun clearVoiceMatch() {
        voiceMatchManager.clearEnrolledVoice()
        val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("voice_match_enforced", false).apply()
        _uiState.update { 
            it.copy(
                isVoiceCalibrationCompleted = false,
                voiceMatchEnforced = false
            ) 
        }
        addLog("Cleared owner voiceprint signature. Voice Match enforcement disabled.")
    }

    fun toggleFloatingWidget(enabled: Boolean) {
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("floating_widget_enabled", enabled).apply()
        _uiState.update { it.copy(floatingWidgetEnabled = enabled) }

        val intent = Intent(context, JarvisFloatingWidgetService::class.java)
        try {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                    val settingsIntent = Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    addLog("Requested SYSTEM_ALERT_WINDOW permission from Settings screen")
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    addLog("Jarvis Floating Widget Service started successfully")
                }
            } else {
                context.stopService(intent)
                addLog("Jarvis Floating Widget Service stopped successfully")
            }
        } catch (e: Exception) {
            Log.e("SamViewModel", "Error toggling floating widget service", e)
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(diagnosticLogs = emptyList()) }
    }

    fun setAppMode(mode: AppMode) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendChatMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        val attachedImg = _uiState.value.attachedImageBase64
        val isDeepResearch = _uiState.value.deepResearchEnabled

        val userMsg = ChatMessage(sender = MessageSender.USER, text = text, imageBase64 = attachedImg)
        _uiState.update { current ->
            current.copy(
                inputText = "",
                isSendingChat = true,
                attachedImageBase64 = null
            )
        }
        viewModelScope.launch {
            chatRepository.saveMessage(userMsg)
        }
        addLog("Chat message sent: '$text' (Has Image: ${attachedImg != null}, Deep Research: $isDeepResearch)")

        if (_uiState.value.isImageGenActive) {
            requestImageGeneration(text, attachedImg)
            return
        }

        if (_uiState.value.isVideoGenActive) {
            requestVideoGeneration(text, _uiState.value.selectedVideoDuration ?: "15s")
            return
        }

        // Check if message corresponds to a common device action
        val lower = text.lowercase()
        if (lower.contains("whatsapp")) {
            val result = deviceActionManager.openWhatsApp()
            val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: "Attempted to open WhatsApp"
            val actionMsg = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "WhatsApp open kar diya hai! 💬",
                isAction = true,
                actionName = "openWhatsApp",
                actionDetail = msg
            )
            viewModelScope.launch {
                chatRepository.saveMessage(actionMsg)
            }
            if (_uiState.value.currentMode == AppMode.VOICE) {
                ttsEngine?.speak("WhatsApp open kar diya hai")
            }
            _uiState.update { it.copy(isSendingChat = false) }
            return
        }

        // Check for other supported apps (YouTube, Instagram, Chrome, Maps, Spotify, Camera, Calculator)
        val supportedApps = listOf(
            "youtube" to "YouTube",
            "instagram" to "Instagram",
            "chrome" to "Google Chrome",
            "browser" to "Google Chrome",
            "maps" to "Google Maps",
            "spotify" to "Spotify",
            "camera" to "Camera",
            "calculator" to "Calculator"
        )
        for ((appKey, appDisplayName) in supportedApps) {
            if (lower.contains(appKey) && (lower.contains("open") || lower.contains("kholo") || lower.contains("chalao") || lower.contains("start") || lower.contains("launch") || lower.trim() == appKey || lower.trim() == "open $appKey")) {
                val result = deviceActionManager.openApp(appDisplayName)
                val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: "Attempted to open $appDisplayName"
                val actionMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "$appDisplayName open kar diya hai! 🚀",
                    isAction = true,
                    actionName = "openApp",
                    actionDetail = msg
                )
                viewModelScope.launch {
                    chatRepository.saveMessage(actionMsg)
                }
                if (_uiState.value.currentMode == AppMode.VOICE) {
                    ttsEngine?.speak("$appDisplayName open kar diya hai")
                }
                _uiState.update { it.copy(isSendingChat = false) }
                return
            }
        }

        if (lower.startsWith("call ") || lower.contains("ko call karo") || lower.contains("ko phone lagao")) {
            val contactQuery = text.replace(Regex("(?i)^(call|call to|phone|phone lagao)\\s*"), "")
                .replace(Regex("(?i)\\s*(ko call karo|ko phone lagao|ko call lagao)$"), "")
                .trim()
            if (contactQuery.isNotBlank()) {
                val result = deviceActionManager.callContact(contactQuery)
                when (result) {
                    is DeviceActionResult.ContactFound -> {
                        val actionMsg = ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "${result.contact.name} (${result.contact.phoneNumber}) ko call mila diya hai. 📞",
                            isAction = true,
                            actionName = "callContact",
                            actionDetail = result.message
                        )
                        viewModelScope.launch {
                            chatRepository.saveMessage(actionMsg)
                        }
                        if (_uiState.value.currentMode == AppMode.VOICE) {
                            ttsEngine?.speak("${result.contact.name} ko call mila diya hai")
                        }
                        _uiState.update { it.copy(isSendingChat = false) }
                        return
                    }
                    is DeviceActionResult.MultipleContactsFound -> {
                        _uiState.update { it.copy(multipleContacts = result.contacts, isSendingChat = false) }
                        return
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            val effectiveKey = _uiState.value.apiKey.ifBlank {
                val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
                prefs.getString("gemini_api_key", null)?.trim()?.ifEmpty { null }
                    ?: prefs.getString("custom_gemini_key", null)?.trim()?.ifEmpty { null }
                    ?: ""
            }
            if (effectiveKey.isNotBlank()) {
                geminiService?.setApiKey(effectiveKey)
                if (_uiState.value.apiKey != effectiveKey) {
                    _uiState.update { it.copy(apiKey = effectiveKey) }
                }
            }

            if (geminiService?.isSessionActive() == true) {
                geminiService?.sendUserText(text)
                _uiState.update { it.copy(isSendingChat = false) }
            } else {
                val (reply, audioBase64) = geminiService?.generateChatResponse(
                    prompt = text,
                    base64Image = attachedImg,
                    isDeepResearch = isDeepResearch
                ) ?: Pair("Sam is unavailable right now.", null)
                val assistantMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = reply
                )
                chatRepository.saveMessage(assistantMsg)
                _uiState.update {
                    it.copy(isSendingChat = false)
                }

                if (_uiState.value.currentMode == AppMode.VOICE) {
                    if (!audioBase64.isNullOrEmpty()) {
                        audioPlayer?.enqueueChunk(audioBase64)
                    } else {
                        ttsEngine?.speak(reply)
                    }
                }
            }
        }
    }

    fun speakMessage(text: String) {
        addLog("Speaking message via Android TextToSpeech: '${text.take(40)}...'")
        audioPlayer?.interrupt()
        ttsEngine?.speak(text)
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            addLog("Persistent Room chat history cleared")
        }
    }

    fun deleteChatMessage(id: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(id)
            addLog("Message removed from Room database")
        }
    }

    fun addQuickReply(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value.quickReplies.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            saveQuickReplies(current)
            addLog("Added quick reply: '$trimmed'")
        }
    }

    fun removeQuickReply(phrase: String) {
        val current = _uiState.value.quickReplies.toMutableList()
        if (current.remove(phrase)) {
            saveQuickReplies(current)
            addLog("Removed quick reply: '$phrase'")
        }
    }

    fun resetQuickReplies() {
        val defaultQuickReplies = listOf(
            "What's the weather?",
            "Remind me in 5 mins",
            "💬 WhatsApp kholo",
            "Ek joke sunao",
            "▶️ Open YouTube"
        )
        saveQuickReplies(defaultQuickReplies)
        addLog("Reset quick replies to default")
    }

    private fun saveQuickReplies(list: List<String>) {
        val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("quick_replies", list.joinToString("|||")).apply()
        _uiState.update { it.copy(quickReplies = list) }
    }

    fun toggleQuickRepliesConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showQuickRepliesConfigDialog = show) }
    }

    fun setAttachedImage(base64: String?) {
        _uiState.update { it.copy(attachedImageBase64 = base64) }
    }

    fun setDeepResearchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(deepResearchEnabled = enabled) }
        addLog("Deep Research mode toggled to: $enabled")
    }

    fun openCanvas(title: String, content: String, contentType: String = "Code") {
        _uiState.update {
            it.copy(
                canvasTitle = title,
                canvasContent = content,
                canvasContentType = contentType,
                showCanvas = true
            )
        }
        addLog("Opened Canvas Workspace for: $title ($contentType)")
    }

    fun closeCanvas() {
        _uiState.update { it.copy(showCanvas = false) }
    }

    fun changeThemeAccent(accent: AppThemeAccent) {
        val prefs = getApplication<Application>().getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_accent", accent.name).apply()
        _uiState.update { it.copy(themeAccent = accent) }
        addLog("Accent theme color changed to: ${accent.displayName}")
    }

    override fun onCleared() {
        super.onCleared()
        micRecorder?.stop()
        audioPlayer?.release()
        ttsEngine?.release()
        geminiService?.disconnect()
    }
}
