package com.aozijx.passly.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.core.message.AppMessagePresentation
import com.aozijx.passly.core.message.AppStatusBarNotifier
import com.aozijx.passly.domain.repository.settings.PortableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppMessagePreferences(
    val statusBarNotificationsEnabled: Boolean = true,
    val iconDownloadNotificationsEnabled: Boolean = true,
    val clipboardClearToastsEnabled: Boolean = true,
    val appCloseToastsEnabled: Boolean = true
) {
    fun allows(message: AppMessage): Boolean = when (message.presentation) {
        AppMessagePresentation.STATUS_BAR -> statusBarNotificationsEnabled && when (message.category) {
            AppMessageCategory.ICON_DOWNLOAD -> iconDownloadNotificationsEnabled
            AppMessageCategory.GENERAL -> true
            AppMessageCategory.CLIPBOARD_CLEAR, AppMessageCategory.APP_CLOSE -> false
        }

        AppMessagePresentation.TOAST -> when (message.category) {
            AppMessageCategory.CLIPBOARD_CLEAR -> clipboardClearToastsEnabled
            AppMessageCategory.APP_CLOSE -> appCloseToastsEnabled
            AppMessageCategory.GENERAL -> true
            AppMessageCategory.ICON_DOWNLOAD -> false
        }
    }
}

@HiltViewModel
class AppMessageHostViewModel @Inject constructor(
    settings: PortableRepository,
    private val statusBarNotifier: AppStatusBarNotifier
) : ViewModel() {
    private val preferences: StateFlow<AppMessagePreferences> = combine(
        settings.statusBarNotificationsEnabled,
        settings.iconDownloadNotificationsEnabled,
        settings.clipboardClearToastsEnabled,
        settings.appCloseToastsEnabled
    ) { statusBar, icon, clipboard, close ->
        AppMessagePreferences(statusBar, icon, clipboard, close)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppMessagePreferences()
    )

    val toastMessages = AppMessageCenter.messages
        .filter { message -> preferences.value.allows(message) }
        .onEach { message ->
            if (message.presentation == AppMessagePresentation.STATUS_BAR) {
                statusBarNotifier.show(message)
            }
        }
        .filter { it.presentation == AppMessagePresentation.TOAST }
}
