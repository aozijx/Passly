package com.aozijx.passly.presentation.feature.settings.main.general

import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.presentation.feature.settings.ui.general.NotificationSettingsEvent
import com.aozijx.passly.presentation.feature.settings.ui.general.NotificationTopic
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `enabling notifications requests runtime permission before persisting`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        val viewModel = viewModel(repository, unavailableState(runtimePermissionGranted = false))

        viewModel.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))

        assertEquals(
            NotificationSettingsEffect.RequestRuntimeNotificationPermission,
            viewModel.effects.first(),
        )
        assertTrue(repository.systemNotificationWrites.isEmpty())
    }

    @Test
    fun `system-disabled notifications report denial and open platform settings`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        val viewModel = viewModel(repository, unavailableState(runtimePermissionGranted = true))

        viewModel.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))

        assertEquals(
            listOf(
                NotificationSettingsEffect.ShowPermissionDenied,
                NotificationSettingsEffect.OpenSystemNotificationSettings,
            ),
            viewModel.effects.take(2).toList(),
        )
        assertTrue(repository.systemNotificationWrites.isEmpty())
    }

    @Test
    fun `permission grant rechecks system availability before enabling notifications`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        var systemState = unavailableState(runtimePermissionGranted = false)
        val viewModel = NotificationSettingsViewModel(
            settingsRepository = repository,
            systemNotificationStateProvider = SystemNotificationStateProvider { systemState },
        )
        systemState = availableState()

        viewModel.onAction(NotificationSettingsAction.RuntimePermissionGranted)
        advanceUntilIdle()

        assertEquals(listOf(true), repository.systemNotificationWrites)
    }

    @Test
    fun `notification ui events map to feature actions without callback handlers`() {
        val cases = listOf(
            NotificationSettingsEvent.SystemNotificationsEnabledChanged(false) to
                NotificationSettingsAction.SetSystemNotificationsEnabled(false),
            NotificationSettingsEvent.OpenSystemNotificationSettings to
                NotificationSettingsAction.OpenSystemNotificationSettings,
            NotificationSettingsEvent.OptionalMessagesEnabledChanged(true) to
                NotificationSettingsAction.SetOptionalMessagesEnabled(true),
            NotificationSettingsEvent.TopicEnabledChanged(NotificationTopic.SECURITY, false) to
                NotificationSettingsAction.SetTopicEnabled(MessageTopic.SECURITY, false),
        )

        cases.forEach { (event, expectedAction) ->
            assertEquals(expectedAction, event.toAction())
        }
    }

    private fun viewModel(
        repository: RecordingMessageSettingsRepository,
        state: SystemNotificationState,
    ) = NotificationSettingsViewModel(
        settingsRepository = repository,
        systemNotificationStateProvider = SystemNotificationStateProvider { state },
    )

    private fun availableState() = SystemNotificationState(
        userSettingEnabled = true,
        runtimePermissionGranted = true,
        notificationsEnabledBySystem = true,
        channelEnabled = true,
    )

    private fun unavailableState(runtimePermissionGranted: Boolean) = SystemNotificationState(
        userSettingEnabled = true,
        runtimePermissionGranted = runtimePermissionGranted,
        notificationsEnabledBySystem = false,
        channelEnabled = true,
    )

    private class RecordingMessageSettingsRepository : MessageSettingsRepository {
        override val messages = MutableStateFlow(MessageSettings())
        val systemNotificationWrites = mutableListOf<Boolean>()

        override suspend fun setOptionalMessagesEnabled(enabled: Boolean) = Unit

        override suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
            systemNotificationWrites += enabled
        }

        override suspend fun setTopicEnabled(topic: MessageTopic, enabled: Boolean) = Unit

        override suspend fun setTopicMinimumLevel(topic: MessageTopic, level: MessageLevel) = Unit
    }
}
