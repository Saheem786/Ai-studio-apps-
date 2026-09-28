package com.example.ui

import android.app.Application
import android.content.Context
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
    val isSendingChat: Boolean = false,
    val quickReplies: List<String> = emptyList(),
    val showQuickRepliesConfigDialog: Boolean = false,
    val attachedImageBase64: String? = null,
    val deepResearchEnabled: Boolean = false,
    val canvasTitle: String = "",
    val canvasContent: String = "",
    val canvasContentType: String = "Code",
    val showCanvas: Boolean = false
)

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ArushiUiState())
    val uiState: StateFlow<ArushiUiState> = _uiState.asStateFlow()

    private val deviceActionManager = DeviceActionManager(application.applicationContext)
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

        _uiState.update { it.copy(apiKey = initialKey, quickReplies = replies) }

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
        val prefs = getApplication<Application>().getSharedPreferences("arushi_prefs", Context.MODE_PRIVATE)
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

    override fun onCleared() {
        super.onCleared()
        micRecorder?.stop()
        audioPlayer?.release()
        ttsEngine?.release()
        geminiService?.disconnect()
    }
}
