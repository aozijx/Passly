package com.aozijx.passly.core.di

import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.domain.usecase.detail.DetailUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

/**
 * 领域层用例模块：负责所有 UseCase 组合（门面类）的初始化
 */
class DomainModule {
    internal val vaultUseCases by lazy {
        VaultUseCases(
            vaultRepository = DataModule.vaultRepository,
            vaultSearchRepository = DataModule.vaultSearchRepository,
            otpRepository = DataModule.otpRepository,
            faviconRepository = DataModule.faviconRepository
        )
    }

    internal val detailUseCases by lazy {
        DetailUseCases(
            vaultRepository = DataModule.vaultRepository,
            faviconRepository = DataModule.faviconRepository
        )
    }

    internal val systemSettingsUseCases by lazy {
        SystemSettingsUseCases(DataModule.settingsRepository)
    }

    internal val securitySettingsUseCases by lazy {
        SecuritySettingsUseCases(DataModule.settingsRepository)
    }

    internal val backupSettingsUseCases by lazy {
        BackupSettingsUseCases(DataModule.settingsRepository)
    }

    internal val userConfigUseCases by lazy {
        UserConfigUseCases(DataModule.userConfigRepository)
    }

    internal val autofillUseCases by lazy {
        AutofillUseCases(DataModule.autofillServiceRepository)
    }
}