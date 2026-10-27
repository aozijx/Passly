package com.aozijx.passly.feature.settings.presentation.main.general

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
    fun `remains loading until persisted message preferences arrive`() = runTest {
        val repository = RecordingMessageSettingsRepository(emptyFlow())
        val viewModel = viewModel(repository, availableState())

        assertEquals(NotificationSettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `enabling notifications requests runtime permission before persisting`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        val viewModel = viewModel(repository, unavailableState(runtimePermissionGranted = false))

        viewModel.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))

        val request = viewModel.nextPermissionRequest()
        assertTrue(viewModel.canLaunchRuntimePermissionRequest(request))
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
        viewModel.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        val request = viewModel.nextPermissionRequest()
        systemState = availableState()

        viewModel.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
        advanceUntilIdle()

        assertEquals(listOf(true), repository.systemNotificationWrites)
    }

    @Test
    fun `unsolicited permission grants do not change notification preference`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        val model = viewModel(repository, availableState())
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(NotificationPermissionRequest()))
        runCurrent()
        assertEquals(emptyList<Boolean>(), repository.systemNotificationWrites)
    }

    @Test
    fun `disabling while permission is pending rejects late grant`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        var system = unavailableState(false)
        val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        val request = model.nextPermissionRequest()
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
        system = availableState()
        assertEquals(false, model.canLaunchRuntimePermissionRequest(request))
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
        runCurrent()
        assertEquals(listOf(false), repository.systemNotificationWrites)
    }

    @Test
    fun `repeated enable and reenable reuse outstanding permission request`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        var system = unavailableState(false)
        val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
        val effects = mutableListOf<NotificationSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        runCurrent()
        assertEquals(1, effects.size)
        val request = (effects.single() as NotificationSettingsEffect.RequestRuntimeNotificationPermission).request
        system = availableState()
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
        runCurrent()
        assertEquals(listOf(false, true), repository.systemNotificationWrites)
    }

    @Test
    fun `denial after user disables is ignored and next enable can request again`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        val model = viewModel(repository, unavailableState(false))
        val effects = mutableListOf<NotificationSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        runCurrent()
        val request = (effects.single() as NotificationSettingsEffect.RequestRuntimeNotificationPermission).request
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
        model.onAction(NotificationSettingsAction.RuntimePermissionDenied(request))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        runCurrent()
        assertEquals(2, effects.size)
        assertTrue(effects.all { it is NotificationSettingsEffect.RequestRuntimeNotificationPermission })
        assertEquals(listOf(false), repository.systemNotificationWrites)
    }

    @Test
    fun `leaving page invalidates queued request and old callbacks cannot consume new request`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        var system = unavailableState(false)
        val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        model.onAction(NotificationSettingsAction.CancelRuntimePermissionRequest)
        val oldRequest = model.nextPermissionRequest()
        assertEquals(false, model.canLaunchRuntimePermissionRequest(oldRequest))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        val currentRequest = model.nextPermissionRequest()
        assertTrue(model.canLaunchRuntimePermissionRequest(currentRequest))
        system = availableState()
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(oldRequest))
        model.onAction(NotificationSettingsAction.RuntimePermissionDenied(oldRequest))
        runCurrent()
        assertTrue(repository.systemNotificationWrites.isEmpty())
        assertTrue(model.canLaunchRuntimePermissionRequest(currentRequest))
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(currentRequest))
        runCurrent()
        assertEquals(listOf(true), repository.systemNotificationWrites)
    }

    @Test
    fun `notification write failures preserve source state and do not report permission denial`() = runTest {
        val repository = RecordingMessageSettingsRepository(save = { throw IOException("synthetic private failure") })
        val model = viewModel(repository, availableState())
        val effects = mutableListOf<NotificationSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        backgroundScope.launch { model.uiState.collect {} }
        runCurrent()
        val before = model.uiState.value
        model.onAction(NotificationSettingsAction.SetOptionalMessagesEnabled(false))
        model.onAction(NotificationSettingsAction.SetTopicEnabled(MessageTopic.SECURITY, false))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        runCurrent()
        assertEquals(List(4) {
            NotificationSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
        }, effects)
        assertEquals(before, model.uiState.value)
        assertEquals(emptyList<Boolean>(), repository.systemNotificationWrites)
        assertEquals(emptyList<Pair<MessageTopic, Boolean>>(), repository.topicWrites)
    }

    @Test
    fun `granted permission with storage failure reports save failure only once`() = runTest {
        val repository = RecordingMessageSettingsRepository(save = { throw IOException("synthetic failure") })
        var system = unavailableState(false)
        val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
        val request = model.nextPermissionRequest()
        val effects = mutableListOf<NotificationSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        system = availableState()
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
        model.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
        runCurrent()
        assertEquals(listOf(NotificationSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))), effects)
        assertEquals(emptyList<Boolean>(), repository.systemNotificationWrites)
    }

    @Test
    fun `destroyed page discards late notification write failure`() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = RecordingMessageSettingsRepository(save = {
            withContext(NonCancellable) { release.await() }
            throw IOException("synthetic late failure")
        })
        val model = viewModel(repository, availableState())
        val effects = mutableListOf<NotificationSettingsEffect>()
        backgroundScope.launch { model.effects.toList(effects) }
        model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
        runCurrent()
        ViewModelStore().apply { put("notifications", model); clear() }
        release.complete(Unit)
        runCurrent()
        assertEquals(emptyList<NotificationSettingsEffect>(), effects)
    }

    @Test
    fun `topic action persists the selected domain topic`() = runTest {
        val repository = RecordingMessageSettingsRepository()
        val viewModel = viewModel(repository, availableState())

        viewModel.onAction(NotificationSettingsAction.SetTopicEnabled(MessageTopic.SECURITY, false))
        advanceUntilIdle()

        assertEquals(listOf(MessageTopic.SECURITY to false), repository.topicWrites)
    }

    private fun viewModel(
        repository: RecordingMessageSettingsRepository,
        state: SystemNotificationState,
    ) = NotificationSettingsViewModel(
        settingsRepository = repository,
        systemNotificationStateProvider = SystemNotificationStateProvider { state },
    )

    private suspend fun NotificationSettingsViewModel.nextPermissionRequest(): NotificationPermissionRequest =
        (effects.first() as NotificationSettingsEffect.RequestRuntimeNotificationPermission).request

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

    private class RecordingMessageSettingsRepository(
        override val messages: Flow<MessageSettings> = MutableStateFlow(MessageSettings()),
        private val save: suspend () -> Unit = {},
    ) : MessageSettingsRepository {
        val systemNotificationWrites = mutableListOf<Boolean>()
        val topicWrites = mutableListOf<Pair<MessageTopic, Boolean>>()

        override suspend fun setOptionalMessagesEnabled(enabled: Boolean) = save()

        override suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
            save()
            systemNotificationWrites += enabled
        }

        override suspend fun setTopicEnabled(topic: MessageTopic, enabled: Boolean) {
            save()
            topicWrites += topic to enabled
        }

        override suspend fun setTopicMinimumLevel(topic: MessageTopic, level: MessageLevel) = Unit
    }
}
