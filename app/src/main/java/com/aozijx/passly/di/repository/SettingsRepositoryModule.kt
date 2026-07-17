package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.settings.DeviceRepositoryImpl
import com.aozijx.passly.data.repository.settings.PortableRepositoryImpl
import com.aozijx.passly.data.repository.settings.RuntimeRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindIdleTimeoutSettings(
        impl: DeviceRepositoryImpl
    ): IdleTimeoutSettings
}