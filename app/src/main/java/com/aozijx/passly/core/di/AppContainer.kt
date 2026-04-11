package com.aozijx.passly.core.di

import com.aozijx.passly.domain.repository.config.SettingsRepository
import com.aozijx.passly.domain.repository.config.UserConfigRepository
import com.aozijx.passly.domain.repository.vault.FaviconRepository
import com.aozijx.passly.domain.repository.vault.HistoryRepository
import com.aozijx.passly.domain.repository.vault.OtpRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.domain.usecase.autofill.AutofillUseCases
import com.aozijx.passly.domain.usecase.detail.DetailUseCases
import com.aozijx.passly.domain.usecase.settings.SettingsUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases

/**
 * 全局依赖容器入口：作为 DataModule 和 DomainModule 的统一门面
 * 保持单例调用习惯，同时在内部实现模块化拆分
 */
object AppContainer {
    
    // --- Data 模块暴露 ---
    val vaultRepository: VaultRepository get() = DataModule.vaultRepository
    val vaultSearchRepository: VaultSearchRepository get() = DataModule.vaultSearchRepository
    val historyRepository: HistoryRepository get() = DataModule.historyRepository
    val otpRepository: OtpRepository get() = DataModule.otpRepository
    val settingsRepository: SettingsRepository get() = DataModule.settingsRepository
    val faviconRepository: FaviconRepository get() = DataModule.faviconRepository
    val userConfigRepository: UserConfigRepository get() = DataModule.userConfigRepository

    // --- Domain 模块暴露 ---
    val vaultUseCases: VaultUseCases get() = DomainModule.vaultUseCases
    val detailUseCases: DetailUseCases get() = DomainModule.detailUseCases
    val settingsUseCases: SettingsUseCases get() = DomainModule.settingsUseCases
    val userConfigUseCases: UserConfigUseCases get() = DomainModule.userConfigUseCases
    val autofillUseCases: AutofillUseCases get() = DomainModule.autofillUseCases
}