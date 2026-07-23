package com.aozijx.passly.di.repository

import com.aozijx.passly.data.backup.VaultBackupServiceImpl
import com.aozijx.passly.domain.service.backup.VaultBackupService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupRepositoryModule {

    @Binds
    @Singleton
    internal abstract fun bindVaultBackupService(
        impl: VaultBackupServiceImpl
    ): VaultBackupService
}
