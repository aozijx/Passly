package com.aozijx.passly.presentation.shared.error

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.core.error.model.DatabaseLocked
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UiErrorLocalizationTest {
    @Test
    fun sameErrorKeyResolvesUsingTheCurrentRenderingLanguage() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val error = DatabaseLocked().toUiErrorMessage()
        val english = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        })
        val chinese = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
            setLocale(Locale.SIMPLIFIED_CHINESE)
        })
        assertEquals("The database is locked. Unlock it first.", english.getString(error.resourceId))
        assertEquals("数据库已锁定，请先解锁", chinese.getString(error.resourceId))
    }
}
