package com.aozijx.passly.app.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal sealed interface AppThemeSettingsState {
    data object Loading : AppThemeSettingsState

    data class Failed(
        val failure: ReadFailure,
    ) : AppThemeSettingsState

    data class Ready(
        val appearance: AppearanceSettings,
        val appCornerRadiusDp: Float,
    ) : AppThemeSettingsState
}

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
                        AppThemeSettingsState.Ready(
                            appearance = appearance,
                            appCornerRadiusDp = interfaceSettings.appCornerRadiusDp,
                        )
                    },
                scope = viewModelScope,
            )

        val state: StateFlow<AppThemeSettingsState> =
            read.state
                .map { sourceState ->
                    when (sourceState) {
                        RecoverableReadState.Loading -> AppThemeSettingsState.Loading
                        is RecoverableReadState.Ready -> sourceState.value
                        is RecoverableReadState.Failed -> AppThemeSettingsState.Failed(sourceState.failure)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = AppThemeSettingsState.Loading,
                )

        fun retry() = read.retry()
    }
