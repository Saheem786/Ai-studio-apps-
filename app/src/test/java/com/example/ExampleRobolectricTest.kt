package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gemini.ArushiPersonality
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
    fun `verify arushi personality configuration`() {
        assertEquals("Sam Ai", ArushiPersonality.ASSISTANT_NAME)
        assertEquals("Aoede", ArushiPersonality.VOICE_NAME)
        assertEquals("models/gemini-3.8-flash", ArushiPersonality.MODEL_LIVE_PRIMARY)
        assertNotNull(ArushiPersonality.SYSTEM_INSTRUCTION)
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
}
