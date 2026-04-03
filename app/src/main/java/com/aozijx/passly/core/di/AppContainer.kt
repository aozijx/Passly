package com.aozijx.passly.core.di

import com.aozijx.passly.AppContext
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.config.UserConfigFileStore
import com.aozijx.passly.data.repository.FaviconDataRepository
import com.aozijx.passly.data.repository.SettingsDataRepository
import com.aozijx.passly.data.repository.UserConfigDataRepository
import com.aozijx.passly.data.repository.VaultDataRepository
import com.aozijx.passly.domain.repository.FaviconRepository
import com.aozijx.passly.domain.repository.SettingsRepository
import com.aozijx.passly.domain.repository.UserConfigRepository
import com.aozijx.passly.domain.repository.VaultRepository
import com.aozijx.passly.domain.usecase.settings.SettingsUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

object AppContainer {
    private val appContext = AppContext.get()
    private val database by lazy { AppDatabase.getDatabase(appContext) }

    val vaultRepository: VaultRepository by lazy { VaultDataRepository(database.vaultDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsDataRepository(appContext.preference) }
    val faviconRepository: FaviconRepository by lazy { FaviconDataRepository(appContext) }
    private val userConfigStore: UserConfigFileStore by lazy { UserConfigFileStore(appContext) }
    val userConfigRepository: UserConfigRepository by lazy { UserConfigDataRepository(userConfigStore) }

    val vaultUseCases: VaultUseCases by lazy { VaultUseCases(vaultRepository, faviconRepository) }
    val settingsUseCases: SettingsUseCases by lazy { SettingsUseCases(settingsRepository) }
    val userConfigUseCases: UserConfigUseCases by lazy { UserConfigUseCases(userConfigRepository) }
}
