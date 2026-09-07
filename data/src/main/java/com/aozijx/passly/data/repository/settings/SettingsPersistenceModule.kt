package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.repository.settings.ProtoAppSettingsRepository
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.domain.settings.port.AppearanceSettingsRepository
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsRepository
import com.aozijx.passly.domain.settings.port.BackupDirectorySettingsSource
import com.aozijx.passly.domain.settings.port.IdleTimeoutSettings
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsSource
import com.aozijx.passly.domain.settings.port.MessageSettingsRepository
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource

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

    @Binds
    @Singleton
    abstract fun bindLibraryViewSettingsRepository(
        impl: ProtoAppSettingsRepository
    ): LibraryViewSettingsRepository

    @Binds
    @Singleton
    abstract fun bindLibraryViewSettingsSource(
        impl: ProtoAppSettingsRepository
    ): LibraryViewSettingsSource

    @Binds
    @Singleton
    abstract fun bindSecuritySettingsRepository(
        impl: ProtoAppSettingsRepository
    ): SecuritySettingsRepository

    @Binds
    @Singleton
    abstract fun bindSecuritySettingsSource(
        impl: ProtoAppSettingsRepository
    ): SecuritySettingsSource

    @Binds
    @Singleton
    abstract fun bindMessageSettingsRepository(
        impl: ProtoAppSettingsRepository
    ): MessageSettingsRepository

    @Binds
    @Singleton
    abstract fun bindMessageSettingsSource(
        impl: ProtoAppSettingsRepository
    ): MessageSettingsSource

    @Binds
    @Singleton
    abstract fun bindBackupDirectorySettingsRepository(
        impl: ProtoAppSettingsRepository
    ): BackupDirectorySettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupDirectorySettingsSource(
        impl: ProtoAppSettingsRepository
    ): BackupDirectorySettingsSource

    @Binds
    @Singleton
    abstract fun bindInteractionSettingsRepository(
        impl: ProtoAppSettingsRepository
    ): InteractionSettingsRepository

    @Binds
    @Singleton
    abstract fun bindInteractionSettingsSource(
        impl: ProtoAppSettingsRepository
    ): InteractionSettingsSource
}
