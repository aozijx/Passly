package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshotProvider
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.app.settings.SettingsReadRetrySignal
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Singleton
class DefaultMessageSettingsSnapshotProvider private constructor(
    private val settingsSource: MessageSettingsSource,
    private val scope: CoroutineScope,
    private val retryDelayMillis: Long,
    private val retrySignal: SettingsReadRetrySignal,
) : MessageSettingsSnapshotProvider {
    @Inject
    constructor(
        settingsSource: MessageSettingsSource,
        retrySignal: SettingsReadRetrySignal = SettingsReadRetrySignal(),
    ) : this(
        settingsSource = settingsSource,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        retryDelayMillis = DEFAULT_RETRY_DELAY_MS,
        retrySignal = retrySignal,
    )

    private val version = AtomicLong(0)
    private val current = AtomicReference<MessageSettingsSnapshot>(MessageSettingsSnapshot.Loading)
    private val settingsRead =
        RecoverableRead(
            source = settingsSource.messages,
            scope = scope,
            retryDelayMillis = retryDelayMillis,
            sharedRetrySignal = retrySignal,
        )

    init {
        require(retryDelayMillis > 0)
        scope.launch {
            settingsRead.updates.collect { state ->
                when (state) {
                    RecoverableReadState.Loading -> {
                        current.set(MessageSettingsSnapshot.Loading)
                    }

                    is RecoverableReadState.Failed -> {
                        current.set(MessageSettingsSnapshot.Failed(state.failure))
                    }

                    is RecoverableReadState.Ready -> {
                        val settings = state.value
                        current.set(
                            MessageSettingsSnapshot.Ready(
                                version = version.incrementAndGet(),
                                value = settings,
                            ),
                        )
                    }
                }
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
            retrySignal: SettingsReadRetrySignal = SettingsReadRetrySignal(),
        ) = DefaultMessageSettingsSnapshotProvider(settingsSource, scope, retryDelayMillis, retrySignal)
    }
}
