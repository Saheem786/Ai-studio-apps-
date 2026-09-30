package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Text-to-Speech (TTS) Engine wrapping Android's TextToSpeech class.
 * Provides voice output for Sam AI assistant with:
 * - Dynamic language & locale fallback (Hindi / Indian English / US English)
 * - Utterance progress tracking (started, done, error)
 * - Assistant state coordination (SPEAKING / IDLE)
 * - Rhythmic amplitude generation for Compose visualizers
 * - Safe initialization queueing and cleanup
 */
class TextToSpeechEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechEnded: () -> Unit = {},
    private val onAmplitudeChanged: (Float) -> Unit = {},
    private val onDiagnosticLog: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SamTextToSpeechEngine"
    }

    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)
    private val isSpeaking = AtomicBoolean(false)
    private var amplitudeJob: Job? = null
    private var pendingSpeechText: String? = null
    private var currentLanguage: AssistantLanguage = AssistantLanguage.HINGLISH

    private val mainHandler = Handler(Looper.getMainLooper())

    fun setLanguage(language: AssistantLanguage) {
        currentLanguage = language
        configureLocaleAndVoice()
    }

    init {
        mainHandler.post {
            try {
                onDiagnosticLog("Initializing Android TextToSpeech engine on Main thread...")
                tts = TextToSpeech(context.applicationContext, this@TextToSpeechEngine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to instantiate Android TextToSpeech", e)
                onDiagnosticLog("TTS creation error: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized.set(true)
            onDiagnosticLog("Android TextToSpeech engine initialized successfully")
            Log.i(TAG, "TextToSpeech initialized successfully")

            configureLocaleAndVoice()
            setupUtteranceListener()

            // Process any queued speech text requested while engine was initializing
            pendingSpeechText?.let { queuedText ->
                pendingSpeechText = null
                speak(queuedText)
            }
        } else {
            isInitialized.set(false)
            val errorMsg = "TTS initialization failed with status code: $status"
            Log.e(TAG, errorMsg)
            onDiagnosticLog(errorMsg)
        }
    }

    private fun configureLocaleAndVoice() {
        val currentTts = tts ?: return

        // 3. Audio Attributes: Configure for high-fidelity speech assistant playback
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            currentTts.setAudioAttributes(audioAttributes)
        } catch (e: Exception) {
            Log.w(TAG, "AudioAttributes configuration fallback", e)
        }

        val targetLocale = currentLanguage.ttsLocale
        val result = currentTts.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "${currentLanguage.displayName} Locale not fully supported, falling back to Indian English/Hindi")
            val inResult = currentTts.setLanguage(Locale("en", "IN"))
            if (inResult == TextToSpeech.LANG_MISSING_DATA || inResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                currentTts.setLanguage(Locale("hi", "IN"))
            }
        }

        // 1. Voice Selection:
        // Query available voices on the device using textToSpeech.voices.
        // Prefer high-quality/network neural Indian voices (Target names containing: "hi-in-x", "en-in-x", "hi_in", "en_in")
        try {
            val availableVoices = currentTts.voices
            if (!availableVoices.isNullOrEmpty()) {
                val preferredIndianVoice = availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    (nameLower.contains("hi-in-x") || nameLower.contains("en-in-x") ||
                     nameLower.contains("hi_in") || nameLower.contains("en_in")) &&
                     !voice.isNetworkConnectionRequired
                } ?: availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    nameLower.contains("hi-in-x") || nameLower.contains("en-in-x") ||
                    nameLower.contains("hi_in") || nameLower.contains("en_in")
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale == Locale("hi", "IN") || voice.locale == Locale("en", "IN")
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale.language == currentLanguage.ttsLanguage
                }

                preferredIndianVoice?.let {
                    currentTts.voice = it
                    onDiagnosticLog("Assigned warm natural voice: ${it.name} (${it.locale})")
                    Log.i(TAG, "Selected natural human voice: ${it.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply natural voice selection", e)
        }

        // 2. Human Pitch & Speech Rate:
        // Slightly elevated pitch for warmth, and relaxed conversational pace
        currentTts.setPitch(1.02f)      // Slightly elevated for warmth and natural human expression
        currentTts.setSpeechRate(0.96f)  // Slightly relaxed conversational pace (avoids robotic fast reading)
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.set(true)
                scope.launch(Dispatchers.Main) {
                    onSpeechStarted()
                    startAmplitudeSimulation()
                }
                onDiagnosticLog("TTS speech started (id: $utteranceId)")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking.set(false)
                scope.launch(Dispatchers.Main) {
                    stopAmplitudeSimulation()
                    onSpeechEnded()
                }
                onDiagnosticLog("TTS speech finished (id: $utteranceId)")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking.set(false)
                scope.launch(Dispatchers.Main) {
                    stopAmplitudeSimulation()
                    onSpeechEnded()
                }
                onDiagnosticLog("TTS speech error on utterance: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking.set(false)
                scope.launch(Dispatchers.Main) {
                    stopAmplitudeSimulation()
                    onSpeechEnded()
                }
                onDiagnosticLog("TTS speech error code $errorCode on utterance: $utteranceId")
            }
        })
    }

    /**
     * Speaks the given text with Indian English / Hindi locale emphasis if available.
     */
    fun speakWithIndianAccent(
        rawText: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = UUID.randomUUID().toString()
    ) {
        speak(rawText, queueMode, utteranceId)
    }

    /**
     * Speaks the given text using Android's TextToSpeech.
     * Sanitizes markdown formatting and handles queueing.
     */
    fun speak(
        rawText: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = UUID.randomUUID().toString()
    ) {
        val cleanText = sanitizeTextForSpeech(rawText)
        if (cleanText.isBlank()) return

        if (!isInitialized.get()) {
            onDiagnosticLog("TTS not ready yet, queueing message: '${cleanText.take(30)}...'")
            pendingSpeechText = cleanText
            // Watchdog fallback: if engine does not initialize within 2500ms, complete cycle so UI doesn't hang
            scope.launch(Dispatchers.Main) {
                delay(2500L)
                if (!isInitialized.get() && pendingSpeechText != null) {
                    onDiagnosticLog("TTS init timeout reached. Triggering speech completion fallback.")
                    pendingSpeechText = null
                    onSpeechEnded()
                }
            }
            return
        }

        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val result = tts?.speak(cleanText, queueMode, params, utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                onDiagnosticLog("TTS queued utterance: '${cleanText.take(40)}...'")
            } else {
                onDiagnosticLog("TTS speak call returned status: $result (fallback end)")
                scope.launch(Dispatchers.Main) {
                    onSpeechEnded()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking TextToSpeech.speak", e)
            onDiagnosticLog("TTS speak error: ${e.message}")
            scope.launch(Dispatchers.Main) {
                onSpeechEnded()
            }
        }
    }

    /**
     * Stop current speech immediately.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        isSpeaking.set(false)
        stopAmplitudeSimulation()
        scope.launch(Dispatchers.Main) {
            onSpeechEnded()
        }
        onDiagnosticLog("TTS stopped")
    }

    fun isSpeakingNow(): Boolean {
        return isSpeaking.get() || (tts?.isSpeaking == true)
    }

    /**
     * Simulates natural vocal amplitude fluctuations while TTS is actively speaking,
     * allowing the Orb and audio visualizer to respond dynamically in the UI.
     */
    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive && isSpeaking.get()) {
                // Speech cadence with rhythmic rises and falls
                phase += 0.35
                val wave = (Math.sin(phase) + 1.0) / 2.0
                val jitter = Random.nextDouble(0.15, 0.45)
                val amp = ((wave * 0.5 + jitter * 0.5) * 0.85).coerceIn(0.15, 0.95).toFloat()

                scope.launch(Dispatchers.Main) {
                    if (isSpeaking.get()) {
                        onAmplitudeChanged(amp)
                    }
                }
                delay(60)
            }
            scope.launch(Dispatchers.Main) {
                onAmplitudeChanged(0f)
            }
        }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        onAmplitudeChanged(0f)
    }

    /**
     * Strip markdown markers, emojis, and code formatting so spoken speech sounds natural.
     */
    private fun sanitizeTextForSpeech(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace(Regex("(?m)^#+\\s*"), "")
            .replace(Regex("https?://\\S+"), "link")
            .replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"), "") // Filter high-surrogate emojis
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()
    }

    /**
     * Release TextToSpeech resources.
     */
    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TextToSpeech", e)
        }
        tts = null
        isInitialized.set(false)
        onDiagnosticLog("Android TextToSpeech engine released")
    }
}
