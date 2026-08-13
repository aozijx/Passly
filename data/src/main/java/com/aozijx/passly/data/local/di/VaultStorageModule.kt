package com.aozijx.passly.data.local.di

import com.aozijx.passly.data.local.database.maintenance.DatabaseCleaner
import com.aozijx.passly.data.local.database.maintenance.DatabaseCleanerImpl
import com.aozijx.passly.data.local.database.recovery.DatabaseRecoveryRepositoryImpl
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.datastore.ProtoVaultBootstrapStore
import com.aozijx.passly.data.repository.database.DatabaseControllerImpl
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.data.database.port.DatabaseController
import com.aozijx.passly.data.database.port.DatabaseRecoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class VaultStorageModule {
    @Binds
    @Singleton
    abstract fun bindDatabaseController(
        impl: DatabaseControllerImpl
    ): DatabaseController

    @Binds
    @Singleton
    abstract fun bindSessionStateProvider(
        impl: AppDatabaseSession
    ): SessionStateProvider

    @Binds
    @Singleton
    abstract fun bindDatabaseSessionLifecycle(
        impl: AppDatabaseSession
    ): DatabaseSessionLifecycle

    @Binds
    @Singleton
    abstract fun bindDatabaseCleaner(
        impl: DatabaseCleanerImpl,
    ): DatabaseCleaner

    @Binds
    @Singleton
    abstract fun bindVaultBootstrapStore(
        impl: ProtoVaultBootstrapStore,
    ): VaultBootstrapStore

    @Binds
    @Singleton
    abstract fun bindDatabaseRecoveryRepository(
        impl: DatabaseRecoveryRepositoryImpl,
    ): DatabaseRecoveryRepository
}
