package com.example.audio

import android.content.Context
import android.os.Bundle
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

    init {
        scope.launch(Dispatchers.IO) {
            try {
                onDiagnosticLog("Initializing Android TextToSpeech engine asynchronously...")
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

        val indianLocale = Locale("en", "IN")
        val result = currentTts.setLanguage(indianLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "en-IN Locale not fully supported, falling back to US English locale detection")
            currentTts.setLanguage(Locale.US)
        }

        // Try to assign a high-quality Indian English or Hindi voice
        try {
            val indianVoice = currentTts.voices?.firstOrNull { voice ->
                (voice.locale.language == "en" && voice.locale.country == "IN") && !voice.isNetworkConnectionRequired
            } ?: currentTts.voices?.firstOrNull { voice ->
                voice.locale.language == "en" && voice.locale.country == "IN"
            } ?: currentTts.voices?.firstOrNull { voice ->
                voice.locale.language == "hi" && voice.locale.country == "IN"
            }

            indianVoice?.let {
                currentTts.voice = it
                onDiagnosticLog("Assigned high-quality Indian accent voice: ${it.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply custom Indian voice to TTS", e)
        }

        currentTts.setPitch(1.0f)
        currentTts.setSpeechRate(1.02f)
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
                onDiagnosticLog("TTS speak call returned status: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking TextToSpeech.speak", e)
            onDiagnosticLog("TTS speak error: ${e.message}")
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
