package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `every appearance write reports a localized error without changing persisted state`() = runTest {
        val repository = Repository { throw IOException("synthetic private diagnostic") }
        val model = AppearanceSettingsViewModel(repository)
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        val before = repository.appearance.value
        val actions = listOf(
            AppearanceSettingsAction.SetThemeMode(ThemeMode.DARK),
            AppearanceSettingsAction.SetDynamicColor(false),
            AppearanceSettingsAction.SetThemeKey("test-theme"),
            AppearanceSettingsAction.SetLanguage(AppLanguage.EN),
            AppearanceSettingsAction.SetFontFamily(FontFamilyMode.SYSTEM),
        )
        actions.forEach(model::onAction)
        runCurrent()
        assertEquals(List(actions.size) { UiErrorMessage(R.string.ui_error_file_io) }, errors)
        assertEquals(before, repository.appearance.value)
    }

    @Test
    fun `successful appearance writes remain silent`() = runTest {
        val repository = Repository { }
        val model = AppearanceSettingsViewModel(repository)
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        model.onAction(AppearanceSettingsAction.SetLanguage(AppLanguage.EN))
        runCurrent()
        assertEquals(listOf("language"), repository.writes)
        assertEquals(emptyList<UiErrorMessage>(), errors)
    }

    @Test
    fun `destroyed appearance page discards late failures`() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = Repository {
            withContext(NonCancellable) { release.await() }
            throw IOException("synthetic late failure")
        }
        val model = AppearanceSettingsViewModel(repository)
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        model.onAction(AppearanceSettingsAction.SetThemeMode(ThemeMode.DARK))
        runCurrent()
        ViewModelStore().apply { put("appearance", model); clear() }
        release.complete(Unit)
        runCurrent()
        assertEquals(emptyList<UiErrorMessage>(), errors)
    }

    private class Repository(private val save: suspend () -> Unit) : AppearanceSettingsRepository {
        override val appearance = MutableStateFlow(AppearanceSettings())
        val writes = mutableListOf<String>()
        override suspend fun setThemeMode(mode: ThemeMode) = write("theme")
        override suspend fun setDynamicColor(enabled: Boolean) = write("dynamic")
        override suspend fun setThemeKey(key: String) = write("key")
        override suspend fun setLanguage(language: AppLanguage) = write("language")
        override suspend fun setFontFamily(mode: FontFamilyMode) = write("font")
        private suspend fun write(field: String) {
            save()
            writes += field
        }
    }
}
