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
        assertEquals("models/gemini-2.0-flash-exp", ArushiPersonality.MODEL_LIVE_PRIMARY)
        assertNotNull(ArushiPersonality.SYSTEM_INSTRUCTION)
    }
}
