package com.aozijx.passly.core.di

import android.app.Application
import android.content.Context
import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.UserConfigFileStore
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.repository.auth.AuthRepositoryImpl
import com.aozijx.passly.data.repository.autofill.AutofillServiceRepositoryImpl
import com.aozijx.passly.data.repository.backup.BackupRepositoryImpl
import com.aozijx.passly.data.repository.otp.OtpRepositoryImpl
import com.aozijx.passly.data.repository.settings.BackupSettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.DatabaseLifecycleRepositoryImpl
import com.aozijx.passly.data.repository.settings.SecuritySettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.SystemSettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.UserConfigRepositoryImpl
import com.aozijx.passly.data.repository.vault.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.vault.HistoryRepositoryImpl
import com.aozijx.passly.data.repository.vault.VaultRepositoryImpl
import com.aozijx.passly.data.repository.vault.VaultSearchRepositoryImpl
import com.aozijx.passly.domain.repository.auth.AuthRepository
import com.aozijx.passly.domain.repository.backup.BackupRepository
import com.aozijx.passly.domain.repository.database.DatabaseLifecycleRepository
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository
import com.aozijx.passly.domain.repository.settings.BackupSettingsRepository
import com.aozijx.passly.domain.repository.settings.SecuritySettingsRepository
import com.aozijx.passly.domain.repository.settings.SystemSettingsRepository
import com.aozijx.passly.domain.repository.userconfig.UserConfigRepository
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.strategy.EntryTypeStrategy
import com.aozijx.passly.domain.strategy.impl.BankCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.IdCardEntryStrategy
import com.aozijx.passly.domain.strategy.impl.PasskeyEntryStrategy
import com.aozijx.passly.domain.strategy.impl.PasswordEntryStrategy
import com.aozijx.passly.domain.strategy.impl.RecoveryCodeEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SeedPhraseEntryStrategy
import com.aozijx.passly.domain.strategy.impl.SshKeyEntryStrategy
import com.aozijx.passly.domain.strategy.impl.TotpEntryStrategy
import com.aozijx.passly.domain.strategy.impl.WiFiEntryStrategy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @ApplicationContext
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Application = context as Application

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseManager: DatabasePassphraseManager
    ): AppDatabase = AppDatabase.getDatabase(context, passphraseManager)

    @Provides
    @Singleton
    fun provideVaultEntryDao(database: AppDatabase): VaultEntryDao =
        database.vaultEntryDao()

    @Provides
    @Singleton
    fun provideVaultHistoryDao(database: AppDatabase): VaultHistoryDao =
        database.vaultHistoryDao()

    @Provides
    @Singleton
    fun provideUserConfigFileStore(
        @ApplicationContext context: Context
    ): UserConfigFileStore = UserConfigFileStore(context)

}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModuleBinds {

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
    abstract fun bindAutofillServiceRepository(impl: AutofillServiceRepositoryImpl): AutofillServiceRepository

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

@Module
@InstallIn(SingletonComponent::class)
abstract class StrategyModule {

    @Binds
    @IntoMap
    @IntKey(0)
    @Singleton
    abstract fun bindPasswordStrategy(impl: PasswordEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(1)
    @Singleton
    abstract fun bindTotpStrategy(impl: TotpEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(2)
    @Singleton
    abstract fun bindPasskeyStrategy(impl: PasskeyEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(4)
    @Singleton
    abstract fun bindRecoveryCodeStrategy(impl: RecoveryCodeEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(3)
    @Singleton
    abstract fun bindWiFiStrategy(impl: WiFiEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(8)
    @Singleton
    abstract fun bindSshKeyStrategy(impl: SshKeyEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(5)
    @Singleton
    abstract fun bindBankCardStrategy(impl: BankCardEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(6)
    @Singleton
    abstract fun bindSeedPhraseStrategy(impl: SeedPhraseEntryStrategy): EntryTypeStrategy

    @Binds
    @IntoMap
    @IntKey(7)
    @Singleton
    abstract fun bindIdCardStrategy(impl: IdCardEntryStrategy): EntryTypeStrategy
}