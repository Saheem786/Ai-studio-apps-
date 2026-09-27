package com.example.gemini

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveService(
    private val onConnected: () -> Unit,
    private val onDisconnected: (String) -> Unit,
    private val onAudioChunkReceived: (base64Audio: String, mimeType: String?) -> Unit,
    private val onTranscriptReceived: (userOrAssistantText: String, isUser: Boolean) -> Unit,
    private val onToolCallReceived: (functionName: String, callId: String, args: Map<String, Any?>) -> Unit,
    private val onInterrupted: () -> Unit,
    private val onError: (String) -> Unit,
    private val onDiagnosticLog: (String) -> Unit
) {
    companion object {
        private const val TAG = "ArushiGeminiLive"
        private const val WS_BASE_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
        private const val REST_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta"
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSocket
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private var activeModel = ArushiPersonality.MODEL_LIVE_PRIMARY
    private var currentApiKey: String = ""

    fun connect(apiKey: String, model: String = ArushiPersonality.MODEL_LIVE_PRIMARY) {
        if (apiKey.isBlank()) {
            onError("Gemini API key is missing. Please configure it in Secrets or in Settings.")
            onDiagnosticLog("Error: Missing Gemini API key")
            return
        }

        currentApiKey = apiKey.trim()
        activeModel = model

        onDiagnosticLog("Connecting to Gemini Live ($activeModel)...")
        Log.i(TAG, "Connecting to Gemini Live with model $activeModel")

        disconnect()

        val url = "$WS_BASE_URL?key=$currentApiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                onDiagnosticLog("Gemini session connected successfully (HTTP ${response.code})")
                Log.i(TAG, "WebSocket opened: ${response.code}")
                onConnected()

                // Send Initial Setup message
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleServerMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onDiagnosticLog("Gemini session closing: $code / $reason")
                Log.i(TAG, "WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                onDiagnosticLog("Gemini session disconnected: $code / $reason")
                Log.i(TAG, "WebSocket closed: $code / $reason")
                onDisconnected(reason.ifEmpty { "Session closed ($code)" })
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                val errMsg = t.message ?: "Connection error"
                val responseMsg = response?.let { " HTTP ${it.code} (${it.message})" } ?: ""
                val fullErr = "$errMsg$responseMsg"
                onDiagnosticLog("Gemini session connection error: $fullErr")
                Log.e(TAG, "WebSocket failure: $fullErr", t)
                onError(fullErr)
                onDisconnected(fullErr)
            }
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                val setupObj = JSONObject().apply {
                    put("model", activeModel)
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", ArushiPersonality.VOICE_NAME)
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", ArushiPersonality.SYSTEM_INSTRUCTION.trim())
                        }))
                    })
                    // Declare Tools for Safe Device Actions
                    put("tools", JSONArray().put(JSONObject().apply {
                        val funcs = JSONArray()

                        // openWhatsApp
                        funcs.put(JSONObject().apply {
                            put("name", "openWhatsApp")
                            put("description", "Open WhatsApp messenger application or web link on the user's phone")
                        })

                        // openApp
                        funcs.put(JSONObject().apply {
                            put("name", "openApp")
                            put("description", "Open a supported application on the user's device (WhatsApp, YouTube, Instagram, Maps, Chrome, Camera, Calculator, Settings, Clock, Spotify)")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("appName", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Name of the app to open")
                                    })
                                })
                                put("required", JSONArray().put("appName"))
                            })
                        })

                        // openWebsite
                        funcs.put(JSONObject().apply {
                            put("name", "openWebsite")
                            put("description", "Open a website in the device's web browser")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("url", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "The website URL to open (must start with https://)")
                                    })
                                })
                                put("required", JSONArray().put("url"))
                            })
                        })

                        // makeCall
                        funcs.put(JSONObject().apply {
                            put("name", "makeCall")
                            put("description", "Open phone dialer or initiate a call to a specific phone number")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("phoneNumber", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "The phone number to call")
                                    })
                                })
                                put("required", JSONArray().put("phoneNumber"))
                            })
                        })

                        // callContact
                        funcs.put(JSONObject().apply {
                            put("name", "callContact")
                            put("description", "Search device contacts by contact name (e.g. Mom, Mummy, Dad, Rahul) and initiate a phone call or dialer")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("contactName", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "The name of the contact to call")
                                    })
                                })
                                put("required", JSONArray().put("contactName"))
                            })
                        })

                        put("functionDeclarations", funcs)
                    }))
                }
                put("setup", setupObj)
            }

            val payload = setupJson.toString()
            onDiagnosticLog("Sending Live session setup (${payload.length} bytes)...")
            ws.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error building setup JSON", e)
            onDiagnosticLog("Setup error: ${e.message}")
        }
    }

    private fun handleServerMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)

            // Setup Complete check
            if (root.has("setupComplete")) {
                onDiagnosticLog("Live session setup completed by Gemini server")
                return
            }

            // Server Content check
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    onDiagnosticLog("Audio playback interrupted by user speech")
                    onInterrupted()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Check for Audio inlineData
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val mimeType = inlineData.optString("mimeType", "audio/pcm;rate=24000")
                                val audioBase64 = inlineData.optString("data", "")
                                if (audioBase64.isNotEmpty()) {
                                    onDiagnosticLog("Gemini response contains audio (MIME: $mimeType, length: ${audioBase64.length} chars)")
                                    onAudioChunkReceived(audioBase64, mimeType)
                                }
                            }

                            // Check for text transcript
                            if (part.has("text")) {
                                val text = part.getString("text")
                                if (text.isNotBlank()) {
                                    onTranscriptReceived(text, false)
                                }
                            }
                        }
                    }
                }
            }

            // Tool Call check
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.optString("id", "call_${System.currentTimeMillis()}")
                        val name = call.getString("name")
                        val argsObj = call.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any?>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.opt(key)
                            }
                        }
                        onDiagnosticLog("Gemini tool call received: $name (id: $callId, args: $argsMap)")
                        onToolCallReceived(name, callId, argsMap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.message}")
            onDiagnosticLog("Error parsing message: ${e.message}")
        }
    }

    /**
     * Send Realtime Microphone PCM audio chunk to Gemini Live
     */
    fun sendAudioChunk(base64Data: String) {
        if (!isConnected.get() || webSocket == null) return

        try {
            val payload = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().put(JSONObject().apply {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    }))
                })
            }.toString()

            webSocket?.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending realtime audio chunk", e)
        }
    }

    /**
     * Send User Text to Gemini Live session
     */
    fun sendUserText(text: String) {
        if (!isConnected.get() || webSocket == null) {
            // If not connected, trigger connect then send
            if (currentApiKey.isNotBlank()) {
                connect(currentApiKey)
            }
            return
        }

        try {
            onTranscriptReceived(text, true)
            val payload = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turns", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", text)
                        }))
                    }))
                    put("turnComplete", true)
                })
            }.toString()

            onDiagnosticLog("Sending user prompt to Gemini Live: '$text'")
            webSocket?.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending user text", e)
            onDiagnosticLog("Error sending prompt: ${e.message}")
        }
    }

    /**
     * Send Tool Response back to Gemini Live
     */
    fun sendToolResponse(callId: String, functionName: String, output: Map<String, Any>) {
        if (!isConnected.get() || webSocket == null) return

        try {
            val outputJson = JSONObject()
            output.forEach { (k, v) -> outputJson.put(k, v) }

            val payload = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", JSONArray().put(JSONObject().apply {
                        put("id", callId)
                        put("response", JSONObject().apply {
                            put("output", outputJson)
                        })
                    }))
                })
            }.toString()

            onDiagnosticLog("Sending tool response for $functionName back to Gemini ($callId)")
            webSocket?.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tool response", e)
        }
    }

    /**
     * Fallback REST Native Audio Generation
     * In case WebSocket encounters an emulator firewall, this directly requests native audio
     * from Gemini and sends audio chunks to our AudioTrack player!
     */
    suspend fun generateRestAudioFallback(prompt: String): Boolean = withContext(Dispatchers.IO) {
        if (currentApiKey.isBlank()) return@withContext false

        onDiagnosticLog("REST Fallback: Requesting native audio response for '$prompt'...")
        try {
            val url = "$REST_BASE_URL/models/gemini-2.5-flash-native-audio-preview-12-2025:generateContent?key=$currentApiKey"
            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", ArushiPersonality.SYSTEM_INSTRUCTION.trim())
                    }))
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", ArushiPersonality.VOICE_NAME)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: "HTTP ${response.code}"
                onDiagnosticLog("REST Fallback failed: $err")
                return@withContext false
            }

            val respString = response.body?.string() ?: ""
            val jsonResp = JSONObject(respString)
            val candidates = jsonResp.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        val mime = inline.optString("mimeType", "audio/pcm;rate=24000")
                        val data = inline.optString("data", "")
                        if (data.isNotEmpty()) {
                            onDiagnosticLog("REST Fallback native audio received (${data.length} chars). Playing...")
                            onAudioChunkReceived(data, mime)
                            return@withContext true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "REST Fallback exception", e)
            onDiagnosticLog("REST Fallback error: ${e.message}")
            false
        }
    }

    /**
     * Text Chat request returning (replyText, audioBase64?)
     */
    suspend fun generateChatResponse(prompt: String): Pair<String, String?> = withContext(Dispatchers.IO) {
        if (currentApiKey.isBlank()) {
            return@withContext Pair("Please configure your Gemini API key in Settings.", null)
        }

        onDiagnosticLog("Chat: Generating response for '$prompt'...")
        try {
            val url = "$REST_BASE_URL/models/gemini-2.0-flash:generateContent?key=$currentApiKey"
            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", ArushiPersonality.SYSTEM_INSTRUCTION.trim())
                    }))
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: "HTTP ${response.code}"
                onDiagnosticLog("Chat API error: $err")
                return@withContext Pair("Error from Gemini API: $err", null)
            }

            val respString = response.body?.string() ?: ""
            val jsonResp = JSONObject(respString)
            val candidates = jsonResp.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")

            var responseText = ""
            var audioBase64: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        responseText += p.getString("text")
                    }
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        if (inline.has("data")) {
                            audioBase64 = inline.getString("data")
                        }
                    }
                }
            }

            if (responseText.isBlank()) {
                responseText = "Sam: I am here, but I didn't get any text response."
            }

            onDiagnosticLog("Chat response received: '${responseText.take(60)}...'")
            Pair(responseText, audioBase64)
        } catch (e: Exception) {
            Log.e(TAG, "Chat API exception", e)
            onDiagnosticLog("Chat API error: ${e.message}")
            Pair("Network error: ${e.message}", null)
        }
    }

    fun isSessionActive(): Boolean = isConnected.get()

    fun disconnect() {
        if (isConnected.get() || webSocket != null) {
            onDiagnosticLog("Closing Gemini Live session...")
            try {
                webSocket?.close(1000, "User closed session")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing WebSocket", e)
            }
            webSocket = null
            isConnected.set(false)
        }
    }
}
