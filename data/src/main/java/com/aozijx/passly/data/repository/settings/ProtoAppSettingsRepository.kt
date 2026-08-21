package com.aozijx.passly.data.repository.settings

import android.content.Context
import com.aozijx.passly.data.local.datastore.appSettingsDataStore
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProtoAppSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AppSettingsRepository {

    private val dataStore = context.applicationContext.appSettingsDataStore

    override val settings: Flow<AppSettingsSnapshot> =
        dataStore.data.map { proto ->
            AppSettingsSnapshot(
                appearance = readAppearance(proto.appearance),
                interfacePrefs = readInterface(proto.interfacePrefs),
                security = readSecurity(proto.security),
                interaction = readInteraction(proto.interaction),
                vault = readVault(proto.vaultView),
                messages = decodeMessageSettings(
                    proto.message.takeIf { proto.hasMessage() }
                ),
                backup = readBackup(proto.backup)
            )
        }

    // ================================================================
    // Convenience flows
    // ================================================================

    override val lockTimeout: Flow<Long> =
        dataStore.data.map { proto ->
            if (proto.hasSecurity()) proto.security.lockTimeoutMs
            else 60000L
        }

    override val isLockOnBackground: Flow<Boolean> =
        dataStore.data.map { proto ->
            if (proto.hasSecurity()) proto.security.lockOnBackground
            else false
        }

    // ================================================================
    // update
    // ================================================================

    override suspend fun update(command: SettingsCommand) {
        dataStore.updateData { proto -> proto.applyCommand(command) }
    }
}
