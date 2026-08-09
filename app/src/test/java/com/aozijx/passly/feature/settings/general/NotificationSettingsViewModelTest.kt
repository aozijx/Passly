package com.aozijx.passly.feature.settings.general

import com.aozijx.passly.domain.notice.model.NoticeLevel
import com.aozijx.passly.domain.notice.model.NoticeTopic
import com.aozijx.passly.domain.notice.model.TopicMessageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsViewModelTest {

    @Test
    fun `uiState default values are correct`() {
        val state = NotificationSettingsUiState()

        assertTrue(state.optionalMessagesEnabled)
        assertTrue(state.systemNotificationsEnabled)
        assertTrue(state.runtimeNotificationPermissionGranted)
        assertTrue(state.notificationsEnabledBySystem)
        assertTrue(state.notificationChannelEnabled)
        assertTrue(state.systemNotificationAvailable)
        assertEquals(TopicMessageSettings(), state.topicSetting(NoticeTopic.CLIPBOARD))
    }

    @Test
    fun `topicSetting returns default when topic not present`() {
        val state = NotificationSettingsUiState(topicSettings = emptyMap())

        assertEquals(TopicMessageSettings(), state.topicSetting(NoticeTopic.CLIPBOARD))
    }

    @Test
    fun `topicSetting returns stored value when present`() {
        val customSettings =
            TopicMessageSettings(enabled = false, minimumLevel = NoticeLevel.WARNING)
        val state = NotificationSettingsUiState(
            topicSettings = mapOf(NoticeTopic.CLIPBOARD to customSettings)
        )

        assertEquals(customSettings, state.topicSetting(NoticeTopic.CLIPBOARD))
    }

    @Test
    fun `systemNotificationAvailable is false when permission denied`() {
        val state = NotificationSettingsUiState(runtimeNotificationPermissionGranted = false)

        assertFalse(state.systemNotificationAvailable)
    }

    @Test
    fun `systemNotificationAvailable is false when channel disabled`() {
        val state = NotificationSettingsUiState(notificationChannelEnabled = false)

        assertFalse(state.systemNotificationAvailable)
    }

    @Test
    fun `systemNotificationAvailable is false when system notifications disabled`() {
        val state = NotificationSettingsUiState(notificationsEnabledBySystem = false)

        assertFalse(state.systemNotificationAvailable)
    }
}