package com.aozijx.passly.data.notice

import com.aozijx.passly.domain.notice.model.AppMessageSettings
import com.aozijx.passly.domain.notice.port.MessageSettingsSnapshotProvider
import com.aozijx.passly.domain.notice.port.VersionedMessageSettings
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMessageSettingsSnapshotProvider @Inject constructor(
    settingsRepository: AppSettingsRepository
) : MessageSettingsSnapshotProvider {
    private val version = AtomicLong(0)
    private val current = AtomicReference(
        VersionedMessageSettings(0, AppMessageSettings())
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            settingsRepository.settings
                .map { it.messages }
                .collect { settings ->
                    current.set(
                        VersionedMessageSettings(
                            version = version.incrementAndGet(),
                            value = settings
                        )
                    )
                }
        }
    }

    override fun current(): VersionedMessageSettings = current.get()
}
