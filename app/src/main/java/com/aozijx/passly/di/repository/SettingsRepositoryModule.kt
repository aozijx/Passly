package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.settings.ProtoAppSettingsRepository
import com.aozijx.passly.domain.repository.settings.AppSettingsRepository
import com.aozijx.passly.domain.repository.settings.IdleTimeoutSettings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsRepositoryModule {

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
}