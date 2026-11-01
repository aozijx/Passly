package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshotProvider
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Singleton
class DefaultMessageSettingsSnapshotProvider private constructor(
    private val settingsSource: MessageSettingsSource,
    private val scope: CoroutineScope,
    private val retryDelayMillis: Long,
) : MessageSettingsSnapshotProvider {
    @Inject
    constructor(settingsSource: MessageSettingsSource) : this(
        settingsSource = settingsSource,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        retryDelayMillis = DEFAULT_RETRY_DELAY_MS,
    )

    private val version = AtomicLong(0)
    private val current = AtomicReference<MessageSettingsSnapshot>(MessageSettingsSnapshot.Loading)

    init {
        require(retryDelayMillis > 0)
        scope.launch {
            while (isActive) {
                try {
                    settingsSource.messages.collect { settings ->
                        current.set(
                            MessageSettingsSnapshot.Ready(
                                version = version.incrementAndGet(),
                                value = settings,
                            ),
                        )
                    }
                    current.set(MessageSettingsSnapshot.Loading)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    current.set(MessageSettingsSnapshot.Loading)
                }
                delay(retryDelayMillis)
            }
        }
    }

    override fun current(): MessageSettingsSnapshot = current.get()

    internal companion object {
        private const val DEFAULT_RETRY_DELAY_MS = 1_000L

        fun create(
            settingsSource: MessageSettingsSource,
            scope: CoroutineScope,
            retryDelayMillis: Long,
        ) = DefaultMessageSettingsSnapshotProvider(settingsSource, scope, retryDelayMillis)
    }
}
