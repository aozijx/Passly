package com.aozijx.passly.core.di

object AppContainer {

    val domain by lazy {
        DomainModule(
            vaultRepository = DataModule.vaultRepository,
            vaultSearchRepository = DataModule.vaultSearchRepository,
            otpRepository = DataModule.otpRepository,
            faviconRepository = DataModule.faviconRepository,
            historyRepository = DataModule.historyRepository,
            systemSettingsRepository = DataModule.systemSettingsRepository,
            securitySettingsRepository = DataModule.securitySettingsRepository,
            backupSettingsRepository = DataModule.backupSettingsRepository,
            backupRepository = DataModule.backupRepository,
            authRepository = DataModule.authRepository,
            userConfigRepository = DataModule.userConfigRepository,
            autofillServiceRepository = DataModule.autofillServiceRepository,
            databaseLifecycleRepository = DataModule.databaseLifecycleRepository
        )
    }
}