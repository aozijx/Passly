package com.aozijx.passly.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessageCenter
import com.aozijx.passly.core.message.AppMessagePresentation
import com.aozijx.passly.core.message.AppNoticeCenter
import com.aozijx.passly.core.message.AppStatusBarNotifier
import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 旧版 AppMessage 偏好 — 向后兼容。
 *
 * 从 [NotificationSettings] 的旧字段推导，用于过滤来自 [AppMessageCenter] 的遗留消息。
 * 新代码应直接通过 [AppNoticeCenter] 发布 [AppNotice]，经由 [AppMessageSettings] 统一过滤。
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

/**
 * 将 [AppNotice] 转换为 [AppMessage] 用于 Toast 展示。
 */
private fun com.aozijx.passly.domain.notice.model.AppNotice.toAppMessage(): AppMessage {
    val isStatusBar =
        deliveryHint == com.aozijx.passly.domain.notice.model.DeliveryHint.STATUS_BAR ||
                (deliveryHint == com.aozijx.passly.domain.notice.model.DeliveryHint.AUTO && content.title != null)
    return AppMessage(
        text = content.text,
        title = content.title,
        longDuration = level == com.aozijx.passly.domain.notice.model.NoticeLevel.ERROR ||
                level == com.aozijx.passly.domain.notice.model.NoticeLevel.CRITICAL,
        presentation = if (isStatusBar) AppMessagePresentation.STATUS_BAR else AppMessagePresentation.TOAST
    )
}

@HiltViewModel
class AppMessageHostViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val statusBarNotifier: AppStatusBarNotifier
) : ViewModel() {

    // ---- Legacy preferences (from old notification fields) ----
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

    // ---- Structured notice settings (AppMessageSettings) ----
    private val noticeSettings: StateFlow<AppMessageSettings> = settingsRepository.settings
        .map { it.notifications.appMessageSettings }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppMessageSettings()
        )

    // ---- Legacy AppMessage 流（向后兼容） ----
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
            flowOf(message).filter { it.presentation == AppMessagePresentation.TOAST }
        } else {
            kotlinx.coroutines.flow.emptyFlow()
        }
    }

    // ---- AppNotice 流（新结构化通知） ----
    private val noticeToastFlow: Flow<AppMessage> = combine(
        AppNoticeCenter.notices,
        noticeSettings
    ) { notice, settings ->
        notice to settings
    }.flatMapLatest { (notice, settings) ->
        if (settings.allows(notice)) {
            // 需要系统通知且满足条件
            val shouldNotify =
                notice.deliveryHint == com.aozijx.passly.domain.notice.model.DeliveryHint.STATUS_BAR ||
                        (notice.deliveryHint == com.aozijx.passly.domain.notice.model.DeliveryHint.AUTO &&
                                notice.content.title != null)
            if (shouldNotify && settings.allowsSystemNotification()) {
                statusBarNotifier.show(notice.toAppMessage())
            }
            flowOf(notice.toAppMessage())
                .filter { it.presentation == AppMessagePresentation.TOAST }
        } else {
            kotlinx.coroutines.flow.emptyFlow()
        }
    }

    /**
     * 合并后的 Toast 消息流。
     * 包含来自旧 [AppMessageCenter] 和 [AppNoticeCenter] 的所有通过过滤的消息。
     */
    val toastMessages: Flow<AppMessage> = merge(legacyToastFlow, noticeToastFlow)
}

/**
 * Helper: wrap a single value into a Flow.
 */
private fun <T> flowOf(value: T): Flow<T> = kotlinx.coroutines.flow.flowOf(value)
