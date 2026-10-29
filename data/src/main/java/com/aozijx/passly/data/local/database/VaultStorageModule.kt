package com.aozijx.passly.data.local.database

import com.aozijx.passly.data.local.database.maintenance.DatabaseCleaner
import com.aozijx.passly.data.local.database.maintenance.DatabaseCleanerImpl
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.local.datastore.ProtoVaultBootstrapStore
import com.aozijx.passly.data.local.autofill.AutofillInboxRecordStoreImpl
import com.aozijx.passly.data.local.database.port.VaultDatabaseResetController
import com.aozijx.passly.domain.access.port.AutofillInboxBootstrapStore
import com.aozijx.passly.domain.access.port.AutofillInboxRecordStore
import com.aozijx.passly.domain.access.port.VaultBootstrapStore
import com.aozijx.passly.domain.access.port.BiometricConfigurationSource
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.data.local.database.port.DatabaseResetController
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
    abstract fun bindBiometricConfigurationSource(impl: ProtoVaultBootstrapStore): BiometricConfigurationSource

    @Binds
    @Singleton
    abstract fun bindDatabaseResetController(
        impl: VaultDatabaseResetController
    ): DatabaseResetController

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
    abstract fun bindAutofillInboxBootstrapStore(
        impl: ProtoVaultBootstrapStore,
    ): AutofillInboxBootstrapStore

    @Binds
    @Singleton
    abstract fun bindAutofillInboxRecordStore(
        impl: AutofillInboxRecordStoreImpl,
    ): AutofillInboxRecordStore
}
