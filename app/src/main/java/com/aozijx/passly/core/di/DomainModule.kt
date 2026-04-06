package com.aozijx.passly.core.di

import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.domain.usecase.settings.SettingsUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

/**
 * 领域层用例模块：负责所有 UseCase 组合（门面类）的初始化
 */
object DomainModule {
    val vaultUseCases: VaultUseCases by lazy {
        VaultUseCases(
            vaultRepository = DataModule.vaultRepository,
            vaultSearchRepository = DataModule.vaultSearchRepository,
            historyRepository = DataModule.historyRepository,
            otpRepository = DataModule.otpRepository,
            faviconRepository = DataModule.faviconRepository
        )
    }

    val settingsUseCases: SettingsUseCases by lazy {
        SettingsUseCases(DataModule.settingsRepository)
    }

    val userConfigUseCases: UserConfigUseCases by lazy {
        UserConfigUseCases(DataModule.userConfigRepository)
    }

    val autofillUseCases: AutofillUseCases by lazy {
        AutofillUseCases(DataModule.autofillServiceRepository)
    }
}
