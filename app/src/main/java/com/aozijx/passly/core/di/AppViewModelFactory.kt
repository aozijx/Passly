package com.aozijx.passly.core.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.aozijx.passly.features.detail.DetailViewModel
import com.aozijx.passly.features.main.MainViewModel
import com.aozijx.passly.features.settings.SettingsViewModel
import com.aozijx.passly.features.vault.VaultViewModel

fun appViewModelFactory(application: Application): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val domain = AppContainer.domain
            return when {
                modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                    MainViewModel(
                        application = application,
                        systemSettingsUseCases = domain.systemSettingsUseCases,
                        securitySettingsUseCases = domain.securitySettingsUseCases,
                        authUseCases = domain.authUseCases,
                        databaseLifecycleUseCases = domain.databaseLifecycleUseCases
                    ) as T
                }

                modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                    SettingsViewModel(
                        application = application,
                        systemSettingsUseCases = domain.systemSettingsUseCases,
                        securitySettingsUseCases = domain.securitySettingsUseCases,
                        backupSettingsUseCases = domain.backupSettingsUseCases,
                        backupUseCases = domain.backupUseCases,
                        authUseCases = domain.authUseCases
                    ) as T
                }

                modelClass.isAssignableFrom(VaultViewModel::class.java) -> {
                    VaultViewModel(
                        application = application,
                        vaultUseCases = domain.vaultUseCases,
                        systemSettingsUseCases = domain.systemSettingsUseCases
                    ) as T
                }

                modelClass.isAssignableFrom(DetailViewModel::class.java) -> {
                    DetailViewModel(
                        application = application,
                        detailUseCases = domain.detailUseCases,
                        userConfigUseCases = domain.userConfigUseCases
                    ) as T
                }

                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}