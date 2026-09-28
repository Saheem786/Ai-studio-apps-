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
}
