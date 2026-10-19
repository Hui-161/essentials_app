package com.sameerasw.essentials.island

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sameerasw.essentials.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "de-rDE")
class IslandTranslationTest {
    @Test
    fun showWhenIsGermanOnAGermanPhone() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("Anzeigen, wenn", context.getString(R.string.island_show_when_title))
        assertEquals("Entsperrt", context.getString(R.string.island_show_when_unlocked))
        assertEquals("Bildschirm an", context.getString(R.string.island_show_when_screen_on))
        assertEquals("Immer", context.getString(R.string.island_show_when_always))
    }
}
