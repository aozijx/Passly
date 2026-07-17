package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.backup.BackupRepositoryImpl
import com.aozijx.passly.domain.repository.backup.BackupRepository
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
    internal abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository
}
