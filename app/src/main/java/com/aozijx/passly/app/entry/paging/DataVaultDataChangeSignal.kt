package com.aozijx.passly.app.entry.paging

import com.aozijx.passly.data.local.database.port.EntryDataRefreshNotifier
import com.aozijx.passly.feature.vault.entry.VaultDataChangeSignal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DataVaultDataChangeSignal @Inject constructor(
    private val notifier: EntryDataRefreshNotifier,
) : VaultDataChangeSignal {
    override fun changes(): Flow<Unit> = notifier.events
}
