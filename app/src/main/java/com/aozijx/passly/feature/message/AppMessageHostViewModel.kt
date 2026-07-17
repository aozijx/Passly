package com.aozijx.passly.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppMessagePreferences(
    val showGeneral: Boolean = true,
    val showIconDownloads: Boolean = true,
    val showClipboardClears: Boolean = true,
    val showAppClose: Boolean = true
) {
    fun allows(category: AppMessageCategory): Boolean = showGeneral && when (category) {
        AppMessageCategory.GENERAL -> true
        AppMessageCategory.ICON_DOWNLOAD -> showIconDownloads
        AppMessageCategory.CLIPBOARD_CLEAR -> showClipboardClears
        AppMessageCategory.APP_CLOSE -> showAppClose
    }
}

@HiltViewModel
class AppMessageHostViewModel @Inject constructor(
    settings: PortableSettingsUseCases
) : ViewModel() {
    private val preferences: StateFlow<AppMessagePreferences> = combine(
        settings.showGeneralMessages,
        settings.showIconDownloadMessages,
        settings.showClipboardClearMessages,
        settings.showAppCloseMessages
    ) { general, icon, clipboard, close ->
        AppMessagePreferences(general, icon, clipboard, close)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppMessagePreferences()
    )

    val messages = AppMessageCenter.messages.filter { message: AppMessage ->
        preferences.value.allows(message.category)
    }
}
