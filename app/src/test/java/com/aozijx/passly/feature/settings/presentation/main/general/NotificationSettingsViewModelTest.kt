package com.aozijx.passly.feature.settings.presentation.main.general

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.R
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.SystemNotificationStateProvider
import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.sampleMessageSettings
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `message preference read failure is visible and retry re-subscribes`() =
        runTest {
            var starts = 0
            val repository =
                RecordingMessageSettingsRepository(
                    messages =
                        flow {
                            starts++
                            if (starts == 1) throw IOException("read failure")
                            emit(
                                sampleMessageSettings(
                                    optionalMessagesEnabled = false,
                                    systemNotificationsEnabled = false,
                                    topicSettings =
                                        MessageTopic.entries.associateWith { topic ->
                                            TopicMessageSettings(
                                                enabled = topic != MessageTopic.SECURITY,
                                                minimumLevel = MessageLevel.WARNING,
                                            )
                                        },
                                ),
                            )
                            awaitCancellation()
                        },
                )
            val model = viewModel(repository, availableState())
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()

            assertEquals(RecoverableReadState.Failed(ReadFailure.IO), model.uiState.value)
            model.onAction(NotificationSettingsAction.RetryRead)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(
                RecoverableReadState.Ready(
                    NotificationSettingsUiModel(
                        optionalMessagesEnabled = false,
                        systemNotificationsEnabled = false,
                        topics =
                            MessageTopic.entries.map { topic ->
                                NotificationTopicUiModel(
                                    topic = topic,
                                    enabled = topic != MessageTopic.SECURITY,
                                )
                            },
                    ),
                ),
                model.uiState.value,
            )
        }

    @Test
    fun `message preference writes are rejected while persisted settings are failed`() =
        runTest {
            val repository = RecordingMessageSettingsRepository(messages = flow { throw IOException("read failure") })
            val model = viewModel(repository, availableState())
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()

            model.onAction(NotificationSettingsAction.SetOptionalMessagesEnabled(true))
            model.onAction(NotificationSettingsAction.SetTopicEnabled(MessageTopic.SECURITY, true))
            runCurrent()

            assertTrue(repository.systemNotificationWrites.isEmpty())
            assertTrue(repository.topicWrites.isEmpty())
        }

    @Test
    fun `remains loading until persisted message preferences arrive`() =
        runTest {
            val repository = RecordingMessageSettingsRepository(emptyFlow())
            val viewModel = viewModel(repository, availableState())

            assertEquals(RecoverableReadState.Loading, viewModel.uiState.value)
        }

    @Test
    fun `enabling notifications requests runtime permission before persisting`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            val viewModel = viewModel(repository, unavailableState(runtimePermissionGranted = false))

            viewModel.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))

            val request = viewModel.nextPermissionRequest()
            assertTrue(viewModel.canLaunchRuntimePermissionRequest(request))
            assertTrue(repository.systemNotificationWrites.isEmpty())
        }

    @Test
    fun `permission launch failure can be retried without reporting system denial`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            val model = viewModel(repository, unavailableState(runtimePermissionGranted = false))
            backgroundScope.launch { model.uiState.collect {} }
            val effects = mutableListOf<NotificationSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            runCurrent()

            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            runCurrent()
            val first =
                (effects.single() as NotificationSettingsEffect.RequestRuntimeNotificationPermission)
                    .request

            model.onAction(NotificationSettingsAction.RuntimePermissionRequestNotStarted(first))
            runCurrent()

            assertTrue(effects.none { it == NotificationSettingsEffect.ShowPermissionDenied })
            assertTrue(repository.systemNotificationWrites.isEmpty())
            assertEquals(false, model.canLaunchRuntimePermissionRequest(first))

            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            runCurrent()

            val requests = effects.filterIsInstance<NotificationSettingsEffect.RequestRuntimeNotificationPermission>()
            assertEquals(2, requests.size)
            assertTrue(requests[1].request !== first)
        }

    @Test
    fun `system-disabled notifications report denial and open platform settings`() =
        runTest {
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
    fun `permission grant rechecks system availability before enabling notifications`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            var systemState = unavailableState(runtimePermissionGranted = false)
            val viewModel =
                NotificationSettingsViewModel(
                    settingsRepository = repository,
                    systemNotificationStateProvider = SystemNotificationStateProvider { systemState },
                ).also { it.onPageEntered() }
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            viewModel.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            val request = viewModel.nextPermissionRequest()
            systemState = availableState()

            viewModel.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
            advanceUntilIdle()

            assertEquals(listOf(true), repository.systemNotificationWrites)
        }

    @Test
    fun `unsolicited permission grants do not change notification preference`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            val model = viewModel(repository, availableState())
            model.onAction(NotificationSettingsAction.RuntimePermissionGranted(NotificationPermissionRequest()))
            runCurrent()
            assertEquals(emptyList<Boolean>(), repository.systemNotificationWrites)
        }

    @Test
    fun `disabling while permission is pending rejects late grant`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            var system = unavailableState(false)
            val model =
                NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
                    .also { it.onPageEntered() }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
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
    fun `repeated enable and reenable reuse outstanding permission request`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            var system = unavailableState(false)
            val model =
                NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
                    .also { it.onPageEntered() }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            val effects = mutableListOf<NotificationSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
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
    fun `denial after user disables is ignored and next enable can request again`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            val model = viewModel(repository, unavailableState(false))
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            val effects = mutableListOf<NotificationSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
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
    fun `leaving page invalidates queued request and old callbacks cannot consume new request`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            var system = unavailableState(false)
            val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
            val owner = model.onPageEntered()
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            val oldRequest = model.nextPermissionRequest()
            model.onPageExited(owner)
            assertEquals(false, model.canLaunchRuntimePermissionRequest(oldRequest))
            model.onPageEntered()
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            val currentRequest = model.nextPermissionRequest()
            assertTrue(model.canLaunchRuntimePermissionRequest(currentRequest))
            system = availableState()
            model.onAction(NotificationSettingsAction.RuntimePermissionGranted(oldRequest))
            model.onAction(NotificationSettingsAction.RuntimePermissionDenied(oldRequest))
            model.onAction(NotificationSettingsAction.RuntimePermissionRequestNotStarted(oldRequest))
            runCurrent()
            assertTrue(repository.systemNotificationWrites.isEmpty())
            assertTrue(model.canLaunchRuntimePermissionRequest(currentRequest))
            model.onAction(NotificationSettingsAction.RuntimePermissionGranted(currentRequest))
            runCurrent()
            assertEquals(listOf(true), repository.systemNotificationWrites)
        }

    @Test
    fun `notification write failures preserve source state and do not report permission denial`() =
        runTest {
            val repository = RecordingMessageSettingsRepository(save = { throw IOException("synthetic private failure") })
            val model = viewModel(repository, availableState())
            val effects = mutableListOf<NotificationSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            val before = model.uiState.value
            model.onAction(NotificationSettingsAction.SetOptionalMessagesEnabled(false))
            model.onAction(NotificationSettingsAction.SetTopicEnabled(MessageTopic.SECURITY, false))
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            runCurrent()
            assertEquals(
                List(4) {
                    NotificationSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))
                },
                effects,
            )
            assertEquals(before, model.uiState.value)
            assertEquals(emptyList<Boolean>(), repository.systemNotificationWrites)
            assertEquals(emptyList<Pair<MessageTopic, Boolean>>(), repository.topicWrites)
        }

    @Test
    fun `granted permission with storage failure reports save failure only once`() =
        runTest {
            val repository = RecordingMessageSettingsRepository(save = { throw IOException("synthetic failure") })
            var system = unavailableState(false)
            val model =
                NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
                    .also { it.onPageEntered() }
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            val request = model.nextPermissionRequest()
            val effects = mutableListOf<NotificationSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            system = availableState()
            model.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
            model.onAction(NotificationSettingsAction.RuntimePermissionGranted(request))
            runCurrent()
            assertEquals(listOf(NotificationSettingsEffect.SaveFailed(UiErrorMessage(R.string.ui_error_file_io))), effects)
            assertEquals(emptyList<Boolean>(), repository.systemNotificationWrites)
        }

    @Test
    fun `destroyed page discards late notification write failure`() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val repository =
                RecordingMessageSettingsRepository(save = {
                    withContext(NonCancellable) { release.await() }
                    throw IOException("synthetic late failure")
                })
            val model = viewModel(repository, availableState())
            val effects = mutableListOf<NotificationSettingsEffect>()
            backgroundScope.launch { model.effects.collect { effects += it } }
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(false))
            runCurrent()
            ViewModelStore().apply {
                put("notifications", model)
                clear()
            }
            release.complete(Unit)
            runCurrent()
            assertEquals(emptyList<NotificationSettingsEffect>(), effects)
        }

    @Test
    fun `topic action persists the selected domain topic`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            val viewModel = viewModel(repository, availableState())
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.onAction(NotificationSettingsAction.SetTopicEnabled(MessageTopic.SECURITY, false))
            advanceUntilIdle()

            assertEquals(listOf(MessageTopic.SECURITY to false), repository.topicWrites)
        }

    @Test
    fun `configuration recreation reissues the same pending request and commits the granted setting`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            var system = unavailableState(runtimePermissionGranted = false)
            val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { system })
            val oldOwner = model.onPageEntered()
            backgroundScope.launch { model.uiState.collect {} }
            runCurrent()
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            val request = model.nextPermissionRequest()

            model.onPageExited(oldOwner, preservePendingPermissionRequest = true)
            model.onAction(NotificationSettingsAction.RuntimePermissionRequestNotStarted(request))
            val newOwner = model.onPageEntered()
            val restoredRequest = model.nextPermissionRequest()
            system = availableState()
            model.onAction(NotificationSettingsAction.RuntimePermissionGranted(restoredRequest))
            advanceUntilIdle()

            assertTrue(newOwner !== oldOwner)
            assertTrue(restoredRequest === request)
            assertEquals(listOf(true), repository.systemNotificationWrites)
        }

    @Test
    fun `late dispose from an old page owner cannot cancel the new page request`() =
        runTest {
            val repository = RecordingMessageSettingsRepository()
            val model = NotificationSettingsViewModel(repository, SystemNotificationStateProvider { unavailableState(false) })
            val oldOwner = model.onPageEntered()
            model.onAction(NotificationSettingsAction.SetSystemNotificationsEnabled(true))
            model.nextPermissionRequest()

            model.onPageExited(oldOwner, preservePendingPermissionRequest = true)
            val currentOwner = model.onPageEntered()
            val currentRequest = model.nextPermissionRequest()
            model.onPageExited(oldOwner, preservePendingPermissionRequest = false)

            assertTrue(currentOwner !== oldOwner)
            assertTrue(model.canLaunchRuntimePermissionRequest(currentRequest))
        }

    private fun viewModel(
        repository: RecordingMessageSettingsRepository,
        state: SystemNotificationState,
    ) = NotificationSettingsViewModel(
        settingsRepository = repository,
        systemNotificationStateProvider = SystemNotificationStateProvider { state },
    ).also { it.onPageEntered() }

    private suspend fun NotificationSettingsViewModel.nextPermissionRequest(): NotificationPermissionRequest =
        (effects.first() as NotificationSettingsEffect.RequestRuntimeNotificationPermission).request

    private fun availableState() =
        SystemNotificationState(
            userSettingEnabled = true,
            runtimePermissionGranted = true,
            notificationsEnabledBySystem = true,
            channelEnabled = true,
        )

    private fun unavailableState(runtimePermissionGranted: Boolean) =
        SystemNotificationState(
            userSettingEnabled = true,
            runtimePermissionGranted = runtimePermissionGranted,
            notificationsEnabledBySystem = false,
            channelEnabled = true,
        )

    private class RecordingMessageSettingsRepository(
        override val messages: Flow<MessageSettings> = MutableStateFlow(sampleMessageSettings()),
        private val save: suspend () -> Unit = {},
    ) : MessageSettingsRepository {
        val systemNotificationWrites = mutableListOf<Boolean>()
        val topicWrites = mutableListOf<Pair<MessageTopic, Boolean>>()

        override suspend fun setOptionalMessagesEnabled(enabled: Boolean) = save()

        override suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
            save()
            systemNotificationWrites += enabled
        }

        override suspend fun setTopicEnabled(
            topic: MessageTopic,
            enabled: Boolean,
        ) {
            save()
            topicWrites += topic to enabled
        }

        override suspend fun setTopicMinimumLevel(
            topic: MessageTopic,
            level: MessageLevel,
        ) = Unit
    }
}
