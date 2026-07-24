package com.aozijx.passly.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.core.message.AppMessagePresentation
import com.aozijx.passly.core.message.AppStatusBarNotifier
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.port.InAppNoticeStream
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 旧版 AppMessage 偏好 — 向后兼容。
 */
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
    private val settingsRepository: AppSettingsRepository,
    private val statusBarNotifier: AppStatusBarNotifier,
    private val inAppNoticeStream: InAppNoticeStream
) : ViewModel() {

    private val legacyPreferences: StateFlow<AppMessagePreferences> = combine(
        settingsRepository.settings.map { it.notifications.statusBarNotificationsEnabled },
        settingsRepository.settings.map { it.notifications.iconDownloadNotificationsEnabled },
        settingsRepository.settings.map { it.notifications.clipboardClearToastsEnabled },
        settingsRepository.settings.map { it.notifications.appCloseToastsEnabled }
    ) { statusBar, icon, clipboard, close ->
        AppMessagePreferences(statusBar, icon, clipboard, close)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppMessagePreferences()
    )

    // Legacy AppMessage 流（向后兼容，待 M14 删除）
    private val legacyToastFlow: Flow<AppMessage> = combine(
        AppMessageCenter.messages,
        legacyPreferences
    ) { message, prefs ->
        message to prefs
    }.flatMapLatest { (message, prefs) ->
        if (prefs.allows(message)) {
            if (message.presentation == AppMessagePresentation.STATUS_BAR) {
                statusBarNotifier.show(message)
            }
            flowOf(message)
                .filter { it.presentation == AppMessagePresentation.TOAST }
        } else {
            emptyFlow()
        }
    }

    // 新的 InAppNoticeStream — 统一在 UI 层渲染
    private val noticeToastFlow: Flow<AppMessage> = inAppNoticeStream.notices
        .map { notice ->
            AppMessage(
                text = "", // FIXME: 接入 NoticeTextResolver
                longDuration = notice.level.ordinal >= NoticeLevel.ERROR.ordinal
            )
        }

    val toastMessages: Flow<AppMessage> = merge(legacyToastFlow, noticeToastFlow)
}
