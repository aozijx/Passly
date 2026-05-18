package com.aozijx.passly.core.di

import com.aozijx.passly.AppContext
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.UserConfigFileStore
import com.aozijx.passly.data.repository.auth.AuthRepositoryImpl
import com.aozijx.passly.data.repository.autofill.AutofillServiceRepositoryImpl
import com.aozijx.passly.data.repository.backup.BackupRepositoryImpl
import com.aozijx.passly.data.repository.database.DatabaseLifecycleRepositoryImpl
import com.aozijx.passly.data.repository.favicon.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.history.HistoryRepositoryImpl
import com.aozijx.passly.data.repository.otp.OtpRepositoryImpl
import com.aozijx.passly.data.repository.settings.BackupSettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.SecuritySettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.SystemSettingsRepositoryImpl
import com.aozijx.passly.data.repository.settings.UserConfigRepositoryImpl
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

/**
 * 数据层依赖模块：负责所有 Repository 实例的初始化与生命周期管理
 */
object DataModule {
    private val appContext = AppContext.get()
    private val database by lazy { AppDatabase.getDatabase(appContext) }

    internal val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(database.vaultEntryDao(), database.vaultHistoryDao())
    }

    internal val vaultSearchRepository: VaultSearchRepository by lazy {
        VaultSearchRepositoryImpl(database.vaultEntryDao())
    }

    internal val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database.vaultHistoryDao())
    }

    internal val otpRepository: OtpRepository by lazy {
        OtpRepositoryImpl()
    }

    internal val autofillServiceRepository: AutofillServiceRepository by lazy {
        AutofillServiceRepositoryImpl(appContext)
    }

    private val securitySettingsRepositoryInstance by lazy {
        SecuritySettingsRepositoryImpl(appContext)
    }

    private val systemSettingsRepositoryInstance by lazy {
        SystemSettingsRepositoryImpl(appContext)
    }

    private val backupSettingsRepositoryInstance by lazy {
        BackupSettingsRepositoryImpl(appContext)
    }

    internal val securitySettingsRepository: SecuritySettingsRepository get() = securitySettingsRepositoryInstance
    internal val systemSettingsRepository: SystemSettingsRepository get() = systemSettingsRepositoryInstance
    internal val backupSettingsRepository: BackupSettingsRepository get() = backupSettingsRepositoryInstance

    internal val faviconRepository: FaviconRepository by lazy {
        FaviconRepositoryImpl(appContext)
    }

    internal val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(appContext)
    }

    internal val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(application = appContext)
    }

    internal val databaseLifecycleRepository: DatabaseLifecycleRepository by lazy {
        DatabaseLifecycleRepositoryImpl(appContext)
    }

    private val userConfigStore by lazy {
        UserConfigFileStore(appContext)
    }

    internal val userConfigRepository: UserConfigRepository by lazy {
        UserConfigRepositoryImpl(userConfigStore)
    }
}