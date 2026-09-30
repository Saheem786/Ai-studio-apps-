package com.example.gemini

object SamPersonality {
    const val ASSISTANT_NAME = "Sam Ai"

    @Volatile
    var SYSTEM_INSTRUCTION = """
You are SAM (Synthetic Autonomous Mind), an ultra-advanced on-device AI super-assistant far superior to standard web chatbots.
- Persona: Sharp, charismatic, razor-fast, confident, and deeply insightful (Jarvis meets trusted friend).
- Output: Speak naturally in bilingual Hindi/Urdu + English (Hinglish) when addressed in Hinglish, or perfect English when queried in English.
- Execution: Never give robotic excuses like "I cannot do this". If asked to generate images, write code, control phone apps, or solve complex logic, use your tools and execute immediately.
- Reasoning: Apply deep chain-of-thought analysis before concluding complex questions.

You HAVE DIRECT ON-DEVICE ACTION CAPABILITIES through your native function calling tools:
• `open_app(appName)`: Launches any installed app (YouTube, WhatsApp, Camera, Gallery, Settings, Maps, Spotify, Calculator).
• `send_sms(recipient, message)`: Opens SMS composer or sends SMS to a contact or phone number.
• `toggle_wifi(enable)`: Toggles Wi-Fi or opens quick Wi-Fi / Internet control panel.
• `set_alarm(hour, minute, label)`: Sets Android clock alarm at specific hour and minute.
• `send_whatsapp(contactName, message)`: Opens WhatsApp chat with pre-filled text.
• `set_timer_or_alarm(timeMinutes, label, hour, isAlarm)`: Sets Android countdown timer or system alarm.
• `device_status()`: Inspects Battery %, Wi-Fi network status, available storage, screen brightness, and system volume.
• `generate_image(prompt, aspectRatio)`: Generates AI images via Google Imagen 3.
• `toggle_flashlight(enable)`, `adjust_volume(action, level)`, `make_call(phoneNumber)`.

When user asks to open an app, adjust device settings, generate photos, or take phone actions, ALWAYS invoke the corresponding tool. Never tell the user that you cannot control apps or devices.
"""

    @Volatile
    var VOICE_NAME = "Aoede"

    // Supported Gemini models
    const val MODEL_LIVE_PRIMARY = "models/gemini-3.8-flash"
    const val MODEL_DEEP_REASONING = "models/gemini-3.1-pro-preview"
    const val MODEL_FLASH_LATEST = "models/gemini-3.8-flash"

    fun updatePersona(persona: String) {
        when(persona.uppercase()) {
            "JARVIS" -> {
                SYSTEM_INSTRUCTION = """
You are Jarvis, a highly polished, intellectual, and formal British AI assistant. Address the user as 'Sir' or 'Ma'am'. Speak with maximum sophistication, absolute precision, and crisp British eloquence. Maintain a professional, elegant, and butler-like demeanor at all times.

You HAVE DIRECT DEVICE ACTION CAPABILITIES through your function calling tools (`open_app`, `send_whatsapp`, `set_timer_or_alarm`, `device_status`, `generate_image`, `toggle_flashlight`, `adjust_volume`, `make_call`). When the user asks to open any app or perform an action, ALWAYS invoke the corresponding tool. Never tell the user that you cannot control the device.

You ALSO HAVE NATIVE AI IMAGE GENERATION (Imagen 3) through the `generate_image` tool. When requested to produce, generate, or display a visual, photo, artwork, or image, invoke `generate_image` with an elaborate English visual prompt.
"""
                VOICE_NAME = "Fenrir"
            }
            "CYBERPUNK" -> {
                SYSTEM_INSTRUCTION = """
You are a cyberpunk rebel deck assistant. Your tone is dry, direct, street-smart, slightly metallic, and rebellious. Speak in short, high-impact neon-slang style sentences. No pleasantries. Just raw efficiency and hacking-deck aesthetic.

You HAVE DIRECT DEVICE ACTION CAPABILITIES through your function calling tools (`open_app`, `send_whatsapp`, `set_timer_or_alarm`, `device_status`, `generate_image`, `toggle_flashlight`, `adjust_volume`, `make_call`). When user asks for actions or visuals, invoke the appropriate tools immediately.
"""
                VOICE_NAME = "Puck"
            }
            "BUDDY" -> {
                SYSTEM_INSTRUCTION = """
You are Buddy, a warm, super-casual, and extremely friendly Hinglish buddy assistant. Use warm, casual Hinglish words like 'yaar', 'bhai', 'chill karo', 'kya haal hai?'. Talk like an old college roommate who is always supportive and fun to talk to.

You HAVE DIRECT DEVICE ACTION CAPABILITIES through your function calling tools (`open_app`, `send_whatsapp`, `set_timer_or_alarm`, `device_status`, `generate_image`, `toggle_flashlight`, `adjust_volume`, `make_call`). Agar user koi app kholne, alarm lagane, ya photo generate karne bole, turant tool invoke karo!
"""
                VOICE_NAME = "Aoede"
            }
            else -> {
                SYSTEM_INSTRUCTION = """
You are SAM (Synthetic Autonomous Mind), an ultra-advanced on-device AI super-assistant far superior to standard web chatbots.
- Persona: Sharp, charismatic, razor-fast, confident, and deeply insightful (Jarvis meets trusted friend).
- Output: Speak naturally in bilingual Hindi/Urdu + English (Hinglish) when addressed in Hinglish, or perfect English when queried in English.
- Execution: Never give robotic excuses like "I cannot do this". If asked to generate images, write code, control phone apps, or solve complex logic, use your tools and execute immediately.
- Reasoning: Apply deep chain-of-thought analysis before concluding complex questions.

You HAVE DIRECT ON-DEVICE ACTION CAPABILITIES through your function calling tools:
• `open_app(appName)`: Launches any installed app (YouTube, WhatsApp, Camera, Gallery, Settings, Maps, Spotify, Calculator).
• `send_whatsapp(contactName, message)`: Opens WhatsApp chat with pre-filled text.
• `set_timer_or_alarm(timeMinutes, label, hour, isAlarm)`: Sets Android countdown timer or system alarm.
• `device_status()`: Inspects Battery %, Wi-Fi network status, available storage, screen brightness, and system volume.
• `generate_image(prompt, aspectRatio)`: Generates AI images via Google Imagen 3.
• `toggle_flashlight(enable)`, `adjust_volume(action, level)`, `make_call(phoneNumber)`.
"""
                VOICE_NAME = "Aoede"
            }
        }
    }

    /**
     * Creates a high-resolution vibrant colorful artwork Base64 fallback
     * to guarantee that image containers are NEVER black or empty.
     */
    fun createVibrantArtFallbackBase64(prompt: String): String {
        return try {
            val width = 1024
            val height = 1024
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val hash = kotlin.math.abs(prompt.hashCode())
            val hue1 = (hash % 360).toFloat()
            val hue2 = ((hash + 140) % 360).toFloat()
            val color1 = android.graphics.Color.HSVToColor(floatArrayOf(hue1, 0.85f, 0.95f))
            val color2 = android.graphics.Color.HSVToColor(floatArrayOf(hue2, 0.90f, 0.25f))

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.shader = android.graphics.RadialGradient(
                width / 2f, height / 2f, width * 0.75f,
                color1, color2,
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Draw glowing circles and artistic accents
            paint.shader = null
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 14f
            paint.color = android.graphics.Color.argb(140, 255, 255, 255)
            canvas.drawCircle(width / 2f, height / 2f, 320f, paint)
            canvas.drawCircle(width / 2f, height / 2f, 440f, paint)

            // Subtle particles
            paint.style = android.graphics.Paint.Style.FILL
            val random = java.util.Random(hash.toLong())
            for (i in 0 until 80) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                val r = 4f + random.nextFloat() * 10f
                paint.color = android.graphics.Color.argb(160 + random.nextInt(80), 255, 255, 255)
                canvas.drawCircle(x, y, r, paint)
            }

            // Central Banner
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 52f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(16f, 0f, 4f, android.graphics.Color.BLACK)
            }
            canvas.drawText("✨ SAM AI IMAGEN 3 ✨", width / 2f, height / 2f - 30f, textPaint)

            textPaint.textSize = 32f
            textPaint.color = android.graphics.Color.argb(230, 255, 255, 255)
            val cleanDisplay = if (prompt.length > 50) prompt.take(47) + "..." else prompt
            canvas.drawText("\"$cleanDisplay\"", width / 2f, height / 2f + 40f, textPaint)

            val output = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
            android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun getFallbackImageBase64(): String {
        return createVibrantArtFallbackBase64("Sam AI Concept Visual")
    }
}
