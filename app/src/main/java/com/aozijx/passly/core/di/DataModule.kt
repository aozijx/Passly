package com.aozijx.passly.core.di

import com.aozijx.passly.AppContext
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.config.UserConfigFileStore
import com.aozijx.passly.data.repository.autofill.AutofillServiceDataRepository
import com.aozijx.passly.data.repository.favicon.FaviconDataRepository
import com.aozijx.passly.data.repository.history.HistoryDataRepository
import com.aozijx.passly.data.repository.otp.OtpDataRepository
import com.aozijx.passly.data.repository.settings.SettingsDataRepository
import com.aozijx.passly.data.repository.settings.UserConfigDataRepository
import com.aozijx.passly.data.repository.vault.VaultDataRepository
import com.aozijx.passly.data.repository.vault.VaultSearchDataRepository
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository
import com.aozijx.passly.domain.repository.settings.SettingsRepository
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
        VaultDataRepository(database.vaultEntryDao())
    }

    internal val vaultSearchRepository: VaultSearchRepository by lazy {
        VaultSearchDataRepository(database.vaultEntryDao())
    }

    internal val historyRepository: HistoryRepository by lazy {
        HistoryDataRepository(database.vaultHistoryDao())
    }

    internal val otpRepository: OtpRepository by lazy {
        OtpDataRepository()
    }

    internal val autofillServiceRepository: AutofillServiceRepository by lazy {
        AutofillServiceDataRepository(appContext)
    }

    internal val settingsRepository: SettingsRepository by lazy {
        SettingsDataRepository(appContext.preference)
    }

    internal val faviconRepository: FaviconRepository by lazy {
        FaviconDataRepository(appContext)
    }

    private val userConfigStore by lazy {
        UserConfigFileStore(appContext)
    }

    internal val userConfigRepository: UserConfigRepository by lazy {
        UserConfigDataRepository(userConfigStore)
    }
}