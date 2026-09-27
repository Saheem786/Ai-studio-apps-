package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioTrackPlayer
import com.example.audio.MicrophoneRecorder
import com.example.device.ContactItem
import com.example.device.DeviceActionManager
import com.example.device.DeviceActionResult
import com.example.gemini.ArushiPersonality
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

enum class AssistantState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    ERROR
}

data class ArushiUiState(
    val state: AssistantState = AssistantState.IDLE,
    val amplitude: Float = 0f,
    val transcript: String = "Tap the microphone to start talking to Sam",
    val actionBanner: String? = null,
    val errorMessage: String? = null,
    val diagnosticLogs: List<String> = emptyList(),
    val isMicActive: Boolean = false,
    val apiKey: String = "",
    val selectedModel: String = ArushiPersonality.MODEL_LIVE_PRIMARY,
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
    val isSendingChat: Boolean = false
)

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ArushiUiState())
    val uiState: StateFlow<ArushiUiState> = _uiState.asStateFlow()

    private val deviceActionManager = DeviceActionManager(application.applicationContext)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private var audioPlayer: AudioTrackPlayer? = null
    private var micRecorder: MicrophoneRecorder? = null
    private var geminiService: GeminiLiveService? = null

    init {
        // Load initial API key from BuildConfig or SharedPreferences
        val prefs = application.getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("custom_gemini_key", null)
        val initialKey = if (!savedKey.isNullOrBlank()) {
            savedKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        _uiState.update { it.copy(apiKey = initialKey ?: "") }

        initAudioSystem()
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

        micRecorder = MicrophoneRecorder(
            onAudioChunk = { base64Data, rawBytes ->
                geminiService?.sendAudioChunk(base64Data)
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
                _uiState.update { current ->
                    val newMsg = ChatMessage(sender = sender, text = text)
                    current.copy(
                        transcript = "$prefix$text",
                        chatMessages = current.chatMessages + newMsg
                    )
                }
                addLog("$prefix$text")
            },
            onToolCallReceived = { name, callId, args ->
                handleToolCall(name, callId, args)
            },
            onInterrupted = {
                addLog("Gemini reported interruption")
                audioPlayer?.interrupt()
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
        val key = _uiState.value.apiKey
        if (key.isBlank()) {
            _uiState.update {
                it.copy(
                    showSettingsDialog = true,
                    errorMessage = "Please enter your Gemini API key in Settings"
                )
            }
            return
        }

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
     * Speaker Diagnostic Test: plays 440Hz sine tone
     */
    fun testSpeaker() {
        addLog("Running Speaker Diagnostic Test (440Hz Tone)...")
        audioPlayer?.testSpeakerTone(viewModelScope)
    }

    /**
     * Quick Prompt / Action Chip Click
     */
    fun sendQuickPrompt(promptText: String) {
        addLog("Quick action triggered: '$promptText'")
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
        deviceActionManager.makeCall(contact.phoneNumber)
        geminiService?.sendUserText("I want to call ${contact.name}")
    }

    fun dismissContactsDialog() {
        _uiState.update { it.copy(multipleContacts = null) }
    }

    fun updateApiKey(newKey: String) {
        val trimmed = newKey.trim()
        val prefs = getApplication<Application>().getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_gemini_key", trimmed).apply()
        _uiState.update { it.copy(apiKey = trimmed, showSettingsDialog = false) }
        addLog("Gemini API key updated")
    }

    fun toggleDiagnosticsSheet(show: Boolean) {
        _uiState.update { it.copy(showDiagnosticsSheet = show) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
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

        val userMsg = ChatMessage(sender = MessageSender.USER, text = text)
        _uiState.update { current ->
            current.copy(
                chatMessages = current.chatMessages + userMsg,
                inputText = "",
                isSendingChat = true
            )
        }
        addLog("Chat message sent: '$text'")

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
            _uiState.update { it.copy(chatMessages = it.chatMessages + actionMsg, isSendingChat = false) }
            return
        } else if (lower.startsWith("call ") || lower.contains("ko call karo") || lower.contains("ko phone lagao")) {
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
                        _uiState.update { it.copy(chatMessages = it.chatMessages + actionMsg, isSendingChat = false) }
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
            if (geminiService?.isSessionActive() == true) {
                geminiService?.sendUserText(text)
                _uiState.update { it.copy(isSendingChat = false) }
            } else {
                val (reply, audioBase64) = geminiService?.generateChatResponse(text)
                    ?: Pair("Sam is unavailable right now.", null)
                val assistantMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = reply
                )
                _uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + assistantMsg,
                        isSendingChat = false
                    )
                }

                if (!audioBase64.isNullOrEmpty()) {
                    audioPlayer?.enqueueChunk(audioBase64)
                }
            }
        }
    }

    fun speakMessage(text: String) {
        viewModelScope.launch {
            addLog("Speaking message: '${text.take(40)}...'")
            geminiService?.generateRestAudioFallback(text)
        }
    }

    fun clearChatHistory() {
        _uiState.update {
            it.copy(
                chatMessages = listOf(
                    ChatMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "Chat history clear ho gayi hai. Aap kya poochna chahte hain?"
                    )
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        micRecorder?.stop()
        audioPlayer?.release()
        geminiService?.disconnect()
    }
}
