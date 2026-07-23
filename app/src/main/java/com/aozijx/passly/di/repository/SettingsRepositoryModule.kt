package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.settings.DeviceRepositoryImpl
import com.aozijx.passly.data.repository.settings.PortableRepositoryImpl
import com.aozijx.passly.data.repository.settings.ProtoAppSettingsRepository
import com.aozijx.passly.data.repository.settings.RuntimeRepositoryImpl
import com.aozijx.passly.domain.repository.settings.AppSettingsRepository
import com.aozijx.passly.domain.repository.settings.DeviceRepository
import com.aozijx.passly.domain.repository.settings.IdleTimeoutSettings
import com.aozijx.passly.domain.repository.settings.PortableRepository
import com.aozijx.passly.domain.repository.settings.RuntimeRepository

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

    // 以下旧绑定保留用于过渡，后续逐步迁移到 AppSettingsRepository 后删除
    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        impl: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindPortableRepository(
        impl: PortableRepositoryImpl
    ): PortableRepository

    @Binds
    @Singleton
    abstract fun bindRuntimeRepository(
        impl: RuntimeRepositoryImpl
    ): RuntimeRepository
}