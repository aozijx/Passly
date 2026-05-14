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
    private val vaultRepository: Lazy<VaultRepository>,
    private val vaultSearchRepository: Lazy<VaultSearchRepository>,
    private val otpRepository: Lazy<OtpRepository>,
    private val faviconRepository: Lazy<FaviconRepository>,
    private val historyRepository: Lazy<HistoryRepository>,
    private val systemSettingsRepository: Lazy<SystemSettingsRepository>,
    private val securitySettingsRepository: Lazy<SecuritySettingsRepository>,
    private val backupSettingsRepository: Lazy<BackupSettingsRepository>,
    private val backupRepository: Lazy<BackupRepository>,
    private val authRepository: Lazy<AuthRepository>,
    private val userConfigRepository: Lazy<UserConfigRepository>,
    private val autofillServiceRepository: Lazy<AutofillServiceRepository>,
    private val databaseLifecycleRepository: Lazy<DatabaseLifecycleRepository>
) {
    internal val vaultUseCases by lazy {
        VaultUseCases(
            vaultRepository = vaultRepository.value,
            vaultSearchRepository = vaultSearchRepository.value,
            otpRepository = otpRepository.value,
            faviconRepository = faviconRepository.value
        )
    }

    internal val detailUseCases by lazy {
        DetailUseCases(
            vaultRepository = vaultRepository.value,
            faviconRepository = faviconRepository.value,
            historyRepository = historyRepository.value
        )
    }

    internal val systemSettingsUseCases by lazy {
        SystemSettingsUseCases(systemSettingsRepository.value)
    }

    internal val securitySettingsUseCases by lazy {
        SecuritySettingsUseCases(securitySettingsRepository.value)
    }

    internal val backupSettingsUseCases by lazy {
        BackupSettingsUseCases(backupSettingsRepository.value)
    }

    internal val backupUseCases by lazy {
        BackupUseCases(backupRepository.value)
    }

    internal val authUseCases by lazy {
        AuthUseCases(authRepository.value)
    }

    internal val userConfigUseCases by lazy {
        UserConfigUseCases(userConfigRepository.value)
    }

    internal val autofillUseCases by lazy {
        AutofillUseCases(autofillServiceRepository.value)
    }

    internal val databaseLifecycleUseCases by lazy {
        DatabaseLifecycleUseCases(databaseLifecycleRepository.value)
    }

    internal val iconResyncUseCases by lazy {
        IconResyncUseCases(vaultRepository.value)
    }
}