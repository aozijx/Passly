package com.aozijx.passly.core.di

object AppContainer {

    val domain by lazy {
        DomainModule(
            vaultRepository = lazy { DataModule.vaultRepository },
            vaultSearchRepository = lazy { DataModule.vaultSearchRepository },
            otpRepository = lazy { DataModule.otpRepository },
            faviconRepository = lazy { DataModule.faviconRepository },
            historyRepository = lazy { DataModule.historyRepository },
            systemSettingsRepository = lazy { DataModule.systemSettingsRepository },
            securitySettingsRepository = lazy { DataModule.securitySettingsRepository },
            backupSettingsRepository = lazy { DataModule.backupSettingsRepository },
            backupRepository = lazy { DataModule.backupRepository },
            authRepository = lazy { DataModule.authRepository },
            userConfigRepository = lazy { DataModule.userConfigRepository },
            autofillServiceRepository = lazy { DataModule.autofillServiceRepository },
            databaseLifecycleRepository = lazy { DataModule.databaseLifecycleRepository }
        )
    }
}