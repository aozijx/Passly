package com.aozijx.passly.core.di

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
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.database.DatabaseLifecycleUseCases
import com.aozijx.passly.domain.usecase.detail.DetailUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.domain.usecase.vault.IconResyncUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

class DomainModule(
    vaultRepository: VaultRepository,
    vaultSearchRepository: VaultSearchRepository,
    otpRepository: OtpRepository,
    faviconRepository: FaviconRepository,
    historyRepository: HistoryRepository,
    systemSettingsRepository: SystemSettingsRepository,
    securitySettingsRepository: SecuritySettingsRepository,
    backupSettingsRepository: BackupSettingsRepository,
    backupRepository: BackupRepository,
    authRepository: AuthRepository,
    userConfigRepository: UserConfigRepository,
    autofillServiceRepository: AutofillServiceRepository,
    databaseLifecycleRepository: DatabaseLifecycleRepository
) {
    internal val vaultUseCases by lazy {
        VaultUseCases(
            vaultRepository = vaultRepository,
            vaultSearchRepository = vaultSearchRepository,
            otpRepository = otpRepository,
            faviconRepository = faviconRepository
        )
    }

    internal val detailUseCases by lazy {
        DetailUseCases(
            vaultRepository = vaultRepository,
            faviconRepository = faviconRepository,
            historyRepository = historyRepository
        )
    }

    internal val systemSettingsUseCases by lazy {
        SystemSettingsUseCases(systemSettingsRepository)
    }

    internal val securitySettingsUseCases by lazy {
        SecuritySettingsUseCases(securitySettingsRepository)
    }

    internal val backupSettingsUseCases by lazy {
        BackupSettingsUseCases(backupSettingsRepository)
    }

    internal val backupUseCases by lazy {
        BackupUseCases(backupRepository)
    }

    internal val authUseCases by lazy {
        AuthUseCases(authRepository)
    }

    internal val userConfigUseCases by lazy {
        UserConfigUseCases(userConfigRepository)
    }

    internal val autofillUseCases by lazy {
        AutofillUseCases(autofillServiceRepository)
    }

    internal val databaseLifecycleUseCases by lazy {
        DatabaseLifecycleUseCases(databaseLifecycleRepository)
    }

    internal val iconResyncUseCases by lazy {
        IconResyncUseCases(vaultRepository)
    }
}