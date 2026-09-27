package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioTrackPlayer
import com.example.audio.MicrophoneRecorder
import com.example.data.AppDatabase
import com.example.data.ChatRepository
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

    private val chatDatabase = AppDatabase.getInstance(application)
    private val chatRepository = ChatRepository(chatDatabase.chatMessageDao())

    private var audioPlayer: AudioTrackPlayer? = null
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
        val prefs = application.getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
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

        _uiState.update { it.copy(apiKey = initialKey) }

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
        val prefs = getApplication<Application>().getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
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
        geminiService?.sendUserText("I want to call ${contact.name}")
    }

    fun dismissContactsDialog() {
        _uiState.update { it.copy(multipleContacts = null) }
    }

    fun updateApiKey(newKey: String) {
        val trimmed = newKey.trim()
        val prefs = getApplication<Application>().getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
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
                inputText = "",
                isSendingChat = true
            )
        }
        viewModelScope.launch {
            chatRepository.saveMessage(userMsg)
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
            viewModelScope.launch {
                chatRepository.saveMessage(actionMsg)
            }
            _uiState.update { it.copy(isSendingChat = false) }
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
                        viewModelScope.launch {
                            chatRepository.saveMessage(actionMsg)
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
                val prefs = getApplication<Application>().getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
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
                val (reply, audioBase64) = geminiService?.generateChatResponse(text)
                    ?: Pair("Sam is unavailable right now.", null)
                val assistantMsg = ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = reply
                )
                chatRepository.saveMessage(assistantMsg)
                _uiState.update {
                    it.copy(isSendingChat = false)
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

    override fun onCleared() {
        super.onCleared()
        micRecorder?.stop()
        audioPlayer?.release()
        geminiService?.disconnect()
    }
}
