package com.aozijx.passly.app.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

internal data class AppThemeSettings(
    val appearance: AppearanceSettings,
    val appCornerRadiusDp: Float,
)

@HiltViewModel
internal class AppThemeSettingsViewModel
    @Inject
    constructor(
        appearanceSettingsRepository: AppearanceSettingsRepository,
        interfaceSettingsRepository: InterfaceSettingsRepository,
    ) : ViewModel() {
        private val read =
            RecoverableRead(
                source =
                    combine(
                        appearanceSettingsRepository.appearance,
                        interfaceSettingsRepository.interfaceSettings,
                    ) { appearance, interfaceSettings ->
                        AppThemeSettings(
                            appearance = appearance,
                            appCornerRadiusDp = interfaceSettings.appCornerRadiusDp,
                        )
                    },
                scope = viewModelScope,
            )

        val state: StateFlow<RecoverableReadState<AppThemeSettings>> = read.state

        fun retry() = read.retry()
    }
