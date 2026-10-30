package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppearanceSettingsRepository
) : ViewModel() {
    private val pageEffects = PageEffects<UiErrorMessage>()
    val errors = pageEffects.effects

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
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_appearance_save_failed,
                onFailure = {
                    pageEffects.tryEmit(owner, it)
                },
                write = write,
            )
        }
    }

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        pageEffects.exit(owner)
    }
}
