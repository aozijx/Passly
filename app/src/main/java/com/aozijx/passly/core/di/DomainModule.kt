package com.aozijx.passly.core.di

import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.domain.usecase.detail.DetailUseCases
import com.aozijx.passly.domain.usecase.settings.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.SystemSettingsUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

/**
 * 领域层用例模块：负责所有 UseCase 组合（门面类）的初始化
 */
class DomainModule {
    val vaultUseCases by lazy {
        VaultUseCases(
            vaultRepository = DataModule.vaultRepository,
            vaultSearchRepository = DataModule.vaultSearchRepository,
            otpRepository = DataModule.otpRepository,
            faviconRepository = DataModule.faviconRepository
        )
    }

    val detailUseCases by lazy {
        DetailUseCases(
            vaultRepository = DataModule.vaultRepository,
            faviconRepository = DataModule.faviconRepository
        )
    }

    val systemSettingsUseCases by lazy {
        SystemSettingsUseCases(DataModule.settingsRepository)
    }

    val securitySettingsUseCases by lazy {
        SecuritySettingsUseCases(DataModule.settingsRepository)
    }

    val backupSettingsUseCases by lazy {
        BackupSettingsUseCases(DataModule.settingsRepository)
    }

    val userConfigUseCases by lazy {
        UserConfigUseCases(DataModule.userConfigRepository)
    }

    val autofillUseCases by lazy {
        AutofillUseCases(DataModule.autofillServiceRepository)
    }
}