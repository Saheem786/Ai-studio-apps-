package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Robust Speech Recognition (STT) Manager using Android's SpeechRecognizer API.
 * Supports:
 * - Free-form Indian English / Hindi (Hinglish) speech recognition
 * - Auto-silence detection and automatic sentence endpointing
 * - Real-time partial results and RMS amplitude callbacks for UI wave visualization
 * - Auto-reconnection / error resilience for continuous hands-free voice mode
 * - Safe Main-thread execution with permission guards
 */
class VoiceInputHandler(
    private val context: Context,
    private val onSpeechReady: () -> Unit = {},
    private val onSpeechStarted: () -> Unit = {},
    private val onRmsAmplitudeChanged: (amplitude: Float) -> Unit = {},
    private val onPartialTranscript: (partialText: String) -> Unit = {},
    private val onFinalTranscript: (finalText: String) -> Unit = {},
    private val onErrorOccurred: (errorMessage: String, isTimeout: Boolean) -> Unit = { _, _ -> },
    private val onSpeechEnded: () -> Unit = {},
    private val onDiagnosticLog: (String) -> Unit = {}
) {

    companion object {
        private const val TAG = "SamVoiceInputHandler"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDestroyed = false
    private var retryCount = 0

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            retryCount = 0
            onDiagnosticLog("SpeechRecognizer is ready for speech")
            onSpeechReady()
            // Provide gentle baseline audio level indicating active listening
            onRmsAmplitudeChanged(0.08f)
        }

        override fun onBeginningOfSpeech() {
            onDiagnosticLog("User started speaking")
            onSpeechStarted()
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical speech RMS from [-2 dB, 10 dB] into [0.05, 1.0]
            val normalized = when {
                rmsdB <= -2f -> 0.05f
                rmsdB >= 10f -> 1.0f
                else -> ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
            }
            onRmsAmplitudeChanged(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Raw buffer received (unused)
        }

        override fun onEndOfSpeech() {
            onDiagnosticLog("User stopped speaking (silence detected)")
            isListening = false
            onRmsAmplitudeChanged(0f)
            onSpeechEnded()
        }

        override fun onError(errorCode: Int) {
            isListening = false
            onRmsAmplitudeChanged(0f)

            // Auto-recovery for Error 11 (ERROR_SERVER_DISCONNECTED) and Error 8 (ERROR_RECOGNIZER_BUSY)
            if ((errorCode == SpeechRecognizer.ERROR_SERVER_DISCONNECTED || errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) && retryCount < 3 && !isDestroyed) {
                retryCount++
                onDiagnosticLog("Speech service disconnected/busy (code $errorCode). Auto-reconnecting attempt $retryCount/3...")
                mainHandler.postDelayed({
                    if (!isDestroyed) {
                        startListening()
                    }
                }, 350L * retryCount)
                return
            }

            val isTimeout = (errorCode == SpeechRecognizer.ERROR_NO_MATCH || 
                             errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

            val errorMsg = when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions for speech recognition"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during speech recognition"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out"
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service disconnected. Tap mic to retry."
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported by device"
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language pack unavailable"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests to speech service"
                else -> "Speech recognition error code: $errorCode"
            }

            onDiagnosticLog("SpeechRecognizer error ($errorCode): $errorMsg")
            Log.w(TAG, "SpeechRecognizer error: $errorMsg (code: $errorCode)")
            onErrorOccurred(errorMsg, isTimeout)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            retryCount = 0
            onRmsAmplitudeChanged(0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()

            onDiagnosticLog("SpeechRecognizer final result: '$recognizedText'")
            if (recognizedText.isNotBlank()) {
                onFinalTranscript(recognizedText)
            } else {
                onErrorOccurred("No speech recognized", true)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()?.trim().orEmpty()
            if (partialText.isNotBlank()) {
                onPartialTranscript(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Extra events
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening() {
        if (isDestroyed) return

        runOnMain {
            if (!hasRecordAudioPermission()) {
                val err = "Microphone permission is required for voice recognition."
                onDiagnosticLog("Start listening aborted: $err")
                onErrorOccurred(err, false)
                return@runOnMain
            }

            if (!isRecognitionAvailable()) {
                val err = "Speech recognition service is not available on this device."
                onDiagnosticLog("Speech recognition unavailable")
                onErrorOccurred(err, false)
                return@runOnMain
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = null

                speechRecognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    try {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                            setRecognitionListener(recognitionListener)
                        }
                    } catch (e: Exception) {
                        SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(recognitionListener)
                        }
                    }
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(recognitionListener)
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    val defaultLocale = java.util.Locale.getDefault().toLanguageTag()
                    val primaryLang = if (defaultLocale.startsWith("hi", ignoreCase = true)) "hi-IN" else "en-IN"
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLang)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US", "hi"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
                }

                speechRecognizer?.startListening(intent)
                onDiagnosticLog("SpeechRecognizer started listening (hi-IN / en-IN / Hinglish)")
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission denied for SpeechRecognizer", e)
                onDiagnosticLog("SecurityException: Microphone permission not granted")
                onErrorOccurred("Microphone permission required", false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer", e)
                onDiagnosticLog("Failed to start SpeechRecognizer: ${e.message}")
                onErrorOccurred("Voice error: ${e.localizedMessage ?: e.message}", false)
            }
        }
    }

    fun stopListening() {
        runOnMain {
            try {
                isListening = false
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
            onRmsAmplitudeChanged(0f)
        }
    }

    fun cancel() {
        runOnMain {
            try {
                isListening = false
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling SpeechRecognizer", e)
            }
            onRmsAmplitudeChanged(0f)
        }
    }

    fun destroy() {
        isDestroyed = true
        runOnMain {
            try {
                isListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
        }
    }

    fun isCurrentlyListening(): Boolean = isListening
}
