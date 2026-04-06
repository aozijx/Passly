package com.aozijx.passly.core.di

import com.aozijx.passly.AppContext
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.config.UserConfigFileStore
import com.aozijx.passly.data.repository.AutofillServiceDataRepository
import com.aozijx.passly.data.repository.FaviconDataRepository
import com.aozijx.passly.data.repository.HistoryDataRepository
import com.aozijx.passly.data.repository.OtpDataRepository
import com.aozijx.passly.data.repository.SettingsDataRepository
import com.aozijx.passly.data.repository.UserConfigDataRepository
import com.aozijx.passly.data.repository.VaultDataRepository
import com.aozijx.passly.data.repository.VaultSearchDataRepository
import com.aozijx.passly.domain.repository.config.SettingsRepository
import com.aozijx.passly.domain.repository.config.UserConfigRepository
import com.aozijx.passly.domain.repository.service.AutofillServiceRepository
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

    val vaultRepository: VaultRepository by lazy {
        VaultDataRepository(database.vaultEntryDao())
    }

    val vaultSearchRepository: VaultSearchRepository by lazy {
        VaultSearchDataRepository(database.vaultEntryDao())
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryDataRepository(database.vaultHistoryDao())
    }

    val otpRepository: OtpRepository by lazy {
        OtpDataRepository()
    }

    val autofillServiceRepository: AutofillServiceRepository by lazy {
        AutofillServiceDataRepository(appContext)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsDataRepository(appContext.preference)
    }

    val faviconRepository: FaviconRepository by lazy {
        FaviconDataRepository(appContext)
    }

    private val userConfigStore: UserConfigFileStore by lazy {
        UserConfigFileStore(appContext)
    }

    val userConfigRepository: UserConfigRepository by lazy {
        UserConfigDataRepository(userConfigStore)
    }
}
