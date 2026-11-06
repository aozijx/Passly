package com.aozijx.passly.data.repository.backup

import com.aozijx.passly.domain.backup.port.VaultBackupStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupPersistenceModule {
    @Binds
    @Singleton
    abstract fun bindVaultBackupStore(implementation: RoomVaultBackupStore): VaultBackupStore
}
