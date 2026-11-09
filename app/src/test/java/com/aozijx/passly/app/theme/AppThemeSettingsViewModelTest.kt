package com.aozijx.passly.app.theme

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemeSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `corrupt theme settings can be retried without defaulting`() =
        runTest {
            var starts = 0
            val appearance =
                AppearanceSettings(
                    themeMode = ThemeMode.DARK,
                    isDynamicColor = false,
                    themeKey = "default",
                    language = AppLanguage.EN,
                    fontFamily = FontFamilyMode.SYSTEM,
                )
            val viewModel =
                AppThemeSettingsViewModel(
                    appearanceSettingsRepository =
                        object : AppearanceSettingsRepository {
                            override val appearance: Flow<AppearanceSettings> =
                                flow {
                                    starts++
                                    if (starts == 1) throw CorruptSettingsDataException(IllegalStateException())
                                    emit(appearance)
                                    awaitCancellation()
                                }

                            override suspend fun setThemeMode(mode: ThemeMode) = Unit

                            override suspend fun setDynamicColor(enabled: Boolean) = Unit

                            override suspend fun setThemeKey(key: String) = Unit

                            override suspend fun setLanguage(language: AppLanguage) = Unit

                            override suspend fun setFontFamily(mode: FontFamilyMode) = Unit
                        },
                    interfaceSettingsRepository =
                        object : InterfaceSettingsRepository {
                            override val interfaceSettings =
                                MutableStateFlow(
                                    InterfaceSettings(
                                        hideSystemBars = false,
                                        collapseTopBarOnScroll = false,
                                        collapseQuickFilterBarOnScroll = true,
                                        appCornerRadiusDp = 30f,
                                    ),
                                )

                            override suspend fun setHideSystemBars(enabled: Boolean) = Unit

                            override suspend fun setTopBarCollapsible(enabled: Boolean) = Unit

                            override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = Unit

                            override suspend fun setAppCornerRadius(radiusDp: Float) = Unit
                        },
                )
            backgroundScope.launch { viewModel.state.collect {} }
            runCurrent()

            assertEquals(RecoverableReadState.Failed(ReadFailure.CORRUPT_DATA), viewModel.state.value)
            viewModel.retry()
            runCurrent()

            assertEquals(2, starts)
            assertEquals(
                RecoverableReadState.Ready(AppThemeSettings(appearance, appCornerRadiusDp = 30f)),
                viewModel.state.value,
            )
        }
}
