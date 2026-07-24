package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val statusBarEnabled: Boolean = false,
    val iconDownloadsEnabled: Boolean = true,
    val optionalMessagesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val topicSettings: Map<NoticeTopic, TopicMessageSettings> = emptyMap()
) {
    fun topicSetting(topic: NoticeTopic): TopicMessageSettings =
        topicSettings[topic] ?: TopicMessageSettings()
}

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {
    val uiState: StateFlow<NotificationSettingsUiState> = settingsRepository.settings.map { s ->
        val n = s.notifications
        NotificationSettingsUiState(
            statusBarEnabled = n.statusBarNotificationsEnabled,
            iconDownloadsEnabled = n.iconDownloadNotificationsEnabled,
            optionalMessagesEnabled = n.optionalMessagesEnabled,
            systemNotificationsEnabled = n.systemNotificationsEnabled,
            topicSettings = n.topicSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationSettingsUiState()
    )

    fun setStatusBarEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetStatusBarNotificationsEnabled(enabled))
    }

    fun setIconDownloadsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetIconDownloadNotificationsEnabled(enabled))
    }

    fun setOptionalMessagesEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetOptionalMessagesEnabled(enabled))
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetSystemNotificationsEnabled(enabled))
    }

    fun setMessageTopicEnabled(topic: NoticeTopic, enabled: Boolean) = viewModelScope.launch {
        settingsRepository.update(SettingsCommand.SetMessageTopicEnabled(topic, enabled))
    }

    fun setMessageTopicMinimumLevel(topic: NoticeTopic, level: NoticeLevel) =
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetMessageTopicMinimumLevel(topic, level))
        }
}
