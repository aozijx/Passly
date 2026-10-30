package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppearanceSettingsRepository
) : ViewModel() {
    private val errorChannel = Channel<UiErrorMessage>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    private var activePageOwner: Any? = null

    fun onAction(action: AppearanceSettingsAction) {
        when (action) {
            is AppearanceSettingsAction.SetThemeMode -> saveSetting {
                settingsRepository.setThemeMode(action.mode)
            }

            is AppearanceSettingsAction.SetDynamicColor -> saveSetting {
                settingsRepository.setDynamicColor(action.enabled)
            }

            is AppearanceSettingsAction.SetThemeKey -> saveSetting {
                settingsRepository.setThemeKey(action.key)
            }

            is AppearanceSettingsAction.SetLanguage -> saveSetting {
                settingsRepository.setLanguage(action.language)
            }

            is AppearanceSettingsAction.SetFontFamily -> saveSetting {
                settingsRepository.setFontFamily(action.mode)
            }
        }
    }

    private fun saveSetting(write: suspend () -> Unit) {
        val owner = activePageOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_appearance_save_failed,
                onFailure = {
                    if (activePageOwner === owner) errorChannel.trySend(it)
                },
                write = write,
            )
        }
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }
}
