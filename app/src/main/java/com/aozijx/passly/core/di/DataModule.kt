package com.aozijx.passly.core.di

import com.aozijx.passly.data.repository.auth.AuthRepositoryImpl
import com.aozijx.passly.data.repository.autofill.AutofillServiceRepositoryImpl
import com.aozijx.passly.data.repository.backup.BackupRepositoryImpl
import com.aozijx.passly.data.repository.settings.BackupSettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.DatabaseLifecycleRepositoryImpl
import com.aozijx.passly.data.repository.settings.SecuritySettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.SystemSettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.UserConfigRepositoryImpl
import com.aozijx.passly.data.repository.vault.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.vault.HistoryRepositoryImpl
import com.aozijx.passly.data.repository.vault.OtpRepositoryImpl
import com.aozijx.passly.data.repository.vault.VaultRepositoryImpl
import com.aozijx.passly.data.repository.vault.VaultSearchRepositoryImpl
import com.aozijx.passly.domain.repository.auth.AuthRepository
import com.aozijx.passly.domain.repository.backup.BackupRepository
import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository
import com.aozijx.passly.domain.repository.settings.BackupSettingsRepository
import com.aozijx.passly.domain.repository.settings.SecuritySettingsRepository
import com.aozijx.passly.domain.repository.settings.SystemSettingsRepository
import com.aozijx.passly.domain.repository.userconfig.UserConfigRepository
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultAutofillRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.security.crypto.VaultLockManager
import com.aozijx.passly.security.crypto.VaultLockManagerImpl
import com.aozijx.passly.security.envelope.EnvelopeStore
import com.aozijx.passly.security.envelope.SharedPrefsEnvelopeStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModuleBinds {

    @Binds
    @Singleton
    internal abstract fun bindVaultLockManager(impl: VaultLockManagerImpl): VaultLockManager

    @Binds
    @Singleton
    abstract fun bindEnvelopeStore(impl: SharedPrefsEnvelopeStore): EnvelopeStore

    @Binds
    @Singleton
    internal abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds
    @Singleton
    abstract fun bindVaultSearchRepository(impl: VaultSearchRepositoryImpl): VaultSearchRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindOtpRepository(impl: OtpRepositoryImpl): OtpRepository

    @Binds
    @Singleton
    abstract fun bindVaultAutofillRepository(impl: AutofillServiceRepositoryImpl): VaultAutofillRepository

    @Binds
    @Singleton
    abstract fun bindSecuritySettingsRepository(impl: SecuritySettingsRepositoryImpl): SecuritySettingsRepository

    @Binds
    @Singleton
    abstract fun bindSystemSettingsRepository(impl: SystemSettingsRepositoryImpl): SystemSettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupSettingsRepository(impl: BackupSettingsRepositoryImpl): BackupSettingsRepository

    @Binds
    @Singleton
    abstract fun bindFaviconRepository(impl: FaviconRepositoryImpl): FaviconRepository

    @Binds
    @Singleton
    internal abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    internal abstract fun bindDatabaseLifecycleRepository(impl: DatabaseLifecycleRepositoryImpl): DatabaseLifecycleRepository

    @Binds
    @Singleton
    abstract fun bindUserConfigRepository(impl: UserConfigRepositoryImpl): UserConfigRepository
}