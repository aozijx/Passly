package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.contract.MessageSettingsSnapshotProvider
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMessageSettingsSnapshotProvider @Inject constructor(
    settingsSource: MessageSettingsSource
) : MessageSettingsSnapshotProvider {
    private val version = AtomicLong(0)
    private val current = AtomicReference<MessageSettingsSnapshot>(MessageSettingsSnapshot.Loading)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            settingsSource.messages
                .collect { settings ->
                    current.set(
                        MessageSettingsSnapshot.Ready(
                            version = version.incrementAndGet(),
                            value = settings
                        )
                    )
                }
        }
    }

    override fun current(): MessageSettingsSnapshot = current.get()
}
