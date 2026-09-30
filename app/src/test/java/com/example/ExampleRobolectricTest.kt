package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gemini.SamPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sam Ai", appName)
    }

    @Test
    fun `verify sam personality configuration`() {
        assertEquals("Sam Ai", SamPersonality.ASSISTANT_NAME)
        assertEquals("Aoede", SamPersonality.VOICE_NAME)
        assertEquals("models/gemini-3.8-flash", SamPersonality.MODEL_LIVE_PRIMARY)
        assertNotNull(SamPersonality.SYSTEM_INSTRUCTION)
    }

    @Test
    fun `verify room chat repository save and retrieval`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.ChatRepository(db.chatMessageDao())

        val msg = com.example.ui.ChatMessage(
            sender = com.example.ui.MessageSender.USER,
            text = "Testing Room chat history persistence"
        )
        repo.saveMessage(msg)

        val count = repo.getMessageCount()
        assertEquals(1, count)

        repo.clearHistory()
        assertEquals(0, repo.getMessageCount())
        db.close()
    }

    @Test
    fun `verify text to speech engine initialization and lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.audio.TextToSpeechEngine(
            context = context,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        )
        assertNotNull(engine)
        engine.speak("Hello from Sam AI")
        engine.stop()
        engine.release()
    }

    @Test
    fun `verify device action manager app launcher and fallback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.device.DeviceActionManager(context)
        val youtubeSuccess = manager.launchApp("YouTube")
        org.junit.Assert.assertTrue(youtubeSuccess)

        val whatsappSuccess = manager.launchApp("WhatsApp")
        org.junit.Assert.assertTrue(whatsappSuccess)
    }

    @Test
    fun `verify assistant language enum and dynamic speech configuration`() {
        val hinglish = com.example.audio.AssistantLanguage.HINGLISH
        assertEquals("hi-IN", hinglish.speechLocale)
        assertEquals("hinglish", hinglish.code)
        org.junit.Assert.assertTrue(hinglish.additionalLocales.contains("en-IN"))

        val englishUs = com.example.audio.AssistantLanguage.ENGLISH_US
        assertEquals("en-US", englishUs.speechLocale)

        val hindi = com.example.audio.AssistantLanguage.HINDI
        assertEquals("hi-IN", hindi.speechLocale)

        val englishIn = com.example.audio.AssistantLanguage.ENGLISH_IN
        assertEquals("en-IN", englishIn.speechLocale)

        val resolved = com.example.audio.AssistantLanguage.fromCode("es_es")
        assertEquals(com.example.audio.AssistantLanguage.SPANISH, resolved)

        val resolvedHindi = com.example.audio.AssistantLanguage.fromCode("hi_in")
        assertEquals(com.example.audio.AssistantLanguage.HINDI, resolvedHindi)

        // Verify dynamic instruction adaptation in SamPersonality
        com.example.gemini.SamPersonality.updateLanguage(com.example.audio.AssistantLanguage.HINDI)
        org.junit.Assert.assertTrue(com.example.gemini.SamPersonality.SYSTEM_INSTRUCTION.contains("Devanagari Hindi"))

        com.example.gemini.SamPersonality.updateLanguage(com.example.audio.AssistantLanguage.HINGLISH)
        org.junit.Assert.assertTrue(com.example.gemini.SamPersonality.SYSTEM_INSTRUCTION.contains("Conversational Hinglish"))
    }

    @Test
    fun `verify speech recognizer intent dynamic configuration for hinglish hindi and english`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = com.example.audio.VoiceInputHandler(context)

        // Hinglish Intent Verification
        val hinglishIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.HINGLISH)
        assertEquals("hi-IN", hinglishIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
        val hinglishAdditional = hinglishIntent.getStringArrayExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES")
        assertNotNull(hinglishAdditional)
        org.junit.Assert.assertTrue(hinglishAdditional!!.contains("en-IN"))
        org.junit.Assert.assertTrue(hinglishAdditional.contains("hi-IN"))

        // Hindi Intent Verification
        val hindiIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.HINDI)
        assertEquals("hi-IN", hindiIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
        org.junit.Assert.assertTrue(hindiIntent.getBooleanExtra(android.speech.RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false))

        // English (India) Intent Verification
        val englishInIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.ENGLISH_IN)
        assertEquals("en-IN", englishInIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))

        // English (US) Intent Verification
        val englishUsIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.ENGLISH_US)
        assertEquals("en-US", englishUsIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
    }

    @Test
    fun `verify ai engine mode quick response and deep thought configurations`() {
        val quick = com.example.gemini.AiEngineMode.QUICK
        assertEquals("gemini-3.8-flash", quick.modelName)
        assertEquals(0.5, quick.temperature, 0.001)
        assertEquals(350, quick.maxOutputTokens)
        assertEquals("⚡", quick.icon)

        val deep = com.example.gemini.AiEngineMode.DEEP_THINK
        assertEquals("gemini-3.1-pro-preview", deep.modelName)
        assertEquals(0.7, deep.temperature, 0.001)
        assertEquals(4096, deep.maxOutputTokens)
        assertEquals("🧠", deep.icon)

        assertEquals(com.example.gemini.AiEngineMode.QUICK, com.example.gemini.AiEngineMode.fromName("QUICK"))
        assertEquals(com.example.gemini.AiEngineMode.DEEP_THINK, com.example.gemini.AiEngineMode.fromName("DEEP_THINK"))
        assertEquals(com.example.gemini.AiEngineMode.DEEP_THINK, com.example.gemini.AiEngineMode.fromName("thinking"))
    }

    @Test
    fun `verify lightweight keyword spotter for hey arushi and hey sam wake words`() {
        val spotter = com.example.audio.LightweightKeywordSpotter()

        // 1. Verify WakeWordOption enum mappings
        val arushiOption = com.example.audio.WakeWordOption.HEY_ARUSHI
        assertEquals("hey_arushi", arushiOption.key)
        assertEquals("Hey Arushi", arushiOption.phrase)
        assertEquals(com.example.audio.WakeWordOption.HEY_ARUSHI, com.example.audio.WakeWordOption.fromKey("hey_arushi"))
        assertEquals(com.example.audio.WakeWordOption.HEY_SAM, com.example.audio.WakeWordOption.fromKey("hey_sam"))
        assertEquals(com.example.audio.WakeWordOption.ANY, com.example.audio.WakeWordOption.fromKey("any"))

        // 2. Synthesize audio waveform matching "Hey Arushi" acoustic phonetic profile
        val arushiPcm = spotter.synthesizeKeywordPcm("Hey Arushi")
        org.junit.Assert.assertTrue(arushiPcm.isNotEmpty())

        val arushiResult = spotter.evaluatePcm(arushiPcm, com.example.audio.WakeWordOption.HEY_ARUSHI, threshold = 0.50f)
        org.junit.Assert.assertTrue("Arushi result should match", arushiResult.isMatch)
        assertEquals("Hey Arushi", arushiResult.detectedKeyword)
        org.junit.Assert.assertTrue("Arushi confidence should exceed 0.5", arushiResult.confidence >= 0.50f)

        // 3. Synthesize audio waveform matching "Hey Sam" acoustic phonetic profile
        val samPcm = spotter.synthesizeKeywordPcm("Hey Sam")
        val samResult = spotter.evaluatePcm(samPcm, com.example.audio.WakeWordOption.HEY_SAM, threshold = 0.50f)
        org.junit.Assert.assertTrue("Sam result should match", samResult.isMatch)
        assertEquals("Hey Sam", samResult.detectedKeyword)
        org.junit.Assert.assertTrue("Sam confidence should exceed 0.5", samResult.confidence >= 0.50f)

        // 4. Verify rejection of silence / sub-threshold noise
        val silencePcm = ByteArray(16000 * 2) // 1 second of zero bytes
        val silenceResult = spotter.evaluatePcm(silencePcm, com.example.audio.WakeWordOption.HEY_ARUSHI)
        org.junit.Assert.assertFalse("Silence should not trigger wake word", silenceResult.isMatch)
        assertEquals(0.0f, silenceResult.confidence, 0.001f)

        // 5. Verify broadcast action constant
        assertEquals("com.example.sam.ACTION_WAKE_WORD_TRIGGERED", com.example.audio.SamWakeWordService.ACTION_WAKE_WORD_TRIGGERED)
    }

    @Test
    fun `verify natural language intent mapping to actionable android intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Call Intent Mapping ("call Mom")
        val callIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("call Mom", context)
        assertNotNull(callIntent)
        assertEquals(com.example.device.intent.IntentType.CALL, callIntent?.type)
        assertEquals("call_contact", callIntent?.actionName)
        assertNotNull(callIntent?.androidIntent)
        val callAction = callIntent?.androidIntent?.action
        org.junit.Assert.assertTrue(callAction == android.content.Intent.ACTION_CALL || callAction == android.content.Intent.ACTION_DIAL)

        // 2. Call Intent with phone number ("call 9876543210")
        val callNumberIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("call 9876543210", context)
        assertNotNull(callNumberIntent)
        assertEquals(android.net.Uri.parse("tel:9876543210"), callNumberIntent?.androidIntent?.data)

        // 3. Alarm Intent Mapping ("set alarm for 7:30 AM")
        val alarmIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("set alarm for 7:30 AM", context)
        assertNotNull(alarmIntent)
        assertEquals(com.example.device.intent.IntentType.ALARM, alarmIntent?.type)
        assertEquals("set_alarm", alarmIntent?.actionName)
        assertEquals(android.provider.AlarmClock.ACTION_SET_ALARM, alarmIntent?.androidIntent?.action)
        assertEquals(7, alarmIntent?.androidIntent?.getIntExtra(android.provider.AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, alarmIntent?.androidIntent?.getIntExtra(android.provider.AlarmClock.EXTRA_MINUTES, -1))

        // 4. Alarm with PM and Label ("set alarm for meeting at 6:15 pm")
        val meetingAlarm = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("set alarm for meeting at 6:15 pm", context)
        assertNotNull(meetingAlarm)
        assertEquals(18, meetingAlarm?.androidIntent?.getIntExtra(android.provider.AlarmClock.EXTRA_HOUR, -1))
        assertEquals(15, meetingAlarm?.androidIntent?.getIntExtra(android.provider.AlarmClock.EXTRA_MINUTES, -1))
        assertEquals("Meeting", meetingAlarm?.androidIntent?.getStringExtra(android.provider.AlarmClock.EXTRA_MESSAGE))

        // 5. Timer Intent Mapping ("set timer for 10 minutes")
        val timerIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("set timer for 10 minutes", context)
        assertNotNull(timerIntent)
        assertEquals(com.example.device.intent.IntentType.TIMER, timerIntent?.type)
        assertEquals(android.provider.AlarmClock.ACTION_SET_TIMER, timerIntent?.androidIntent?.action)
        assertEquals(600, timerIntent?.androidIntent?.getIntExtra(android.provider.AlarmClock.EXTRA_LENGTH, -1))

        // 6. SMS Intent Mapping ("send SMS to Rahul hello I am outside")
        val smsIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("send SMS to Rahul hello I am outside", context)
        assertNotNull(smsIntent)
        assertEquals(com.example.device.intent.IntentType.SMS, smsIntent?.type)
        assertEquals(android.content.Intent.ACTION_SENDTO, smsIntent?.androidIntent?.action)
        assertEquals("hello I am outside", smsIntent?.androidIntent?.getStringExtra("sms_body"))

        // 7. Navigation Intent Mapping ("navigate to Airport")
        val navIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("navigate to Airport", context)
        assertNotNull(navIntent)
        assertEquals(com.example.device.intent.IntentType.NAVIGATION, navIntent?.type)
        assertEquals(android.content.Intent.ACTION_VIEW, navIntent?.androidIntent?.action)
        assertEquals(android.net.Uri.parse("google.navigation:q=Airport"), navIntent?.androidIntent?.data)

        // 8. YouTube Intent Mapping ("play Believer on YouTube")
        val ytIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("play Believer on YouTube", context)
        assertNotNull(ytIntent)
        assertEquals(com.example.device.intent.IntentType.YOUTUBE, ytIntent?.type)
        assertEquals(android.content.Intent.ACTION_SEARCH, ytIntent?.androidIntent?.action)
        assertEquals("Believer", ytIntent?.androidIntent?.getStringExtra("query"))

        // 9. Hardware Flashlight Control ("turn on flashlight")
        val flashIntent = com.example.device.intent.IntentFunctionMapper.mapNaturalLanguageToIntent("turn on flashlight", context)
        assertNotNull(flashIntent)
        assertEquals(com.example.device.intent.IntentType.SYSTEM_CONTROL, flashIntent?.type)
        assertEquals("toggle_flashlight", flashIntent?.actionName)
        assertEquals(true, flashIntent?.parameters?.get("enabled"))
    }
}
