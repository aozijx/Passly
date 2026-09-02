package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.repository.settings.ProtoAppSettingsRepository
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsPersistenceModule {

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        impl: ProtoAppSettingsRepository
    ): AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindIdleTimeoutSettings(
        impl: ProtoAppSettingsRepository
    ): IdleTimeoutSettings

    @Binds
    @Singleton
    abstract fun bindAppearanceSettingsRepository(
        impl: ProtoAppSettingsRepository
    ): AppearanceSettingsRepository

    @Binds
    @Singleton
    abstract fun bindInterfaceSettingsRepository(
        impl: ProtoAppSettingsRepository
    ): InterfaceSettingsRepository
}
