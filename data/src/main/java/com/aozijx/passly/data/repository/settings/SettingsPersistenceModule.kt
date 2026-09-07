package com.aozijx.passly.data.repository.settings

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
    abstract fun bindIdleTimeoutSettings(
        impl: ProtoSettingsStore
    ): IdleTimeoutSettings

    @Binds
    @Singleton
    abstract fun bindAppearanceSettingsRepository(
        impl: ProtoSettingsStore
    ): AppearanceSettingsRepository

    @Binds
    @Singleton
    abstract fun bindInterfaceSettingsRepository(
        impl: ProtoSettingsStore
    ): InterfaceSettingsRepository

    @Binds
    @Singleton
    abstract fun bindLibraryViewSettingsRepository(
        impl: ProtoSettingsStore
    ): LibraryViewSettingsRepository

    @Binds
    @Singleton
    abstract fun bindLibraryViewSettingsSource(
        impl: ProtoSettingsStore
    ): LibraryViewSettingsSource

    @Binds
    @Singleton
    abstract fun bindSecuritySettingsRepository(
        impl: ProtoSettingsStore
    ): SecuritySettingsRepository

    @Binds
    @Singleton
    abstract fun bindSecuritySettingsSource(
        impl: ProtoSettingsStore
    ): SecuritySettingsSource

    @Binds
    @Singleton
    abstract fun bindMessageSettingsRepository(
        impl: ProtoSettingsStore
    ): MessageSettingsRepository

    @Binds
    @Singleton
    abstract fun bindMessageSettingsSource(
        impl: ProtoSettingsStore
    ): MessageSettingsSource

    @Binds
    @Singleton
    abstract fun bindBackupDirectorySettingsRepository(
        impl: ProtoSettingsStore
    ): BackupDirectorySettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupDirectorySettingsSource(
        impl: ProtoSettingsStore
    ): BackupDirectorySettingsSource

    @Binds
    @Singleton
    abstract fun bindInteractionSettingsRepository(
        impl: ProtoSettingsStore
    ): InteractionSettingsRepository

    @Binds
    @Singleton
    abstract fun bindInteractionSettingsSource(
        impl: ProtoSettingsStore
    ): InteractionSettingsSource
}
