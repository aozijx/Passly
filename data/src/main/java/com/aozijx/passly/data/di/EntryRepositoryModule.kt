package com.aozijx.passly.data.di

import com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleaner
import com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleanerImpl
import com.aozijx.passly.data.repository.activity.RoomActivityQueryRepository
import com.aozijx.passly.data.repository.activity.RoomActivityRecorder
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepositoryImpl
import com.aozijx.passly.data.repository.entry.RoomEntryCommandRepository
import com.aozijx.passly.data.repository.entry.RoomEntryListQueryRepository
import com.aozijx.passly.data.repository.entry.RoomEntryLinkRepository
import com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository
import com.aozijx.passly.data.repository.entry.RoomSensitiveFieldRepository
import com.aozijx.passly.data.repository.favicon.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.otp.RoomOtpConfigRepository
import com.aozijx.passly.data.repository.search.BlindIndexMaintenance
import com.aozijx.passly.domain.autofill.repository.CredentialServiceRepository
import com.aozijx.passly.domain.entry.repository.ActivityQueryRepository
import com.aozijx.passly.domain.entry.repository.ActivityRecorder
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.domain.entry.repository.EntryLinkRepository
import com.aozijx.passly.domain.entry.repository.EntryQueryRepository
import com.aozijx.passly.domain.entry.repository.FaviconRepository
import com.aozijx.passly.domain.entry.repository.OtpConfigRepository
import com.aozijx.passly.domain.entry.repository.SearchIndexMaintenance
import com.aozijx.passly.domain.entry.repository.SensitiveFieldRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class EntryRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEntryQueryRepository(impl: RoomEntryQueryRepository): EntryQueryRepository

    @Binds
    @Singleton
    abstract fun bindEntryCommandRepository(impl: RoomEntryCommandRepository): EntryCommandRepository

    @Binds
    @Singleton
    abstract fun bindEntryLinkRepository(impl: RoomEntryLinkRepository): EntryLinkRepository

    @Binds
    @Singleton
    abstract fun bindSensitiveFieldRepository(
        impl: RoomSensitiveFieldRepository
    ): SensitiveFieldRepository

    @Binds
    @Singleton
    abstract fun bindEntryListQueryRepository(impl: RoomEntryListQueryRepository): EntryListQueryRepository

    @Binds
    @Singleton
    abstract fun bindActivityQueryRepository(impl: RoomActivityQueryRepository): ActivityQueryRepository

    @Binds
    @Singleton
    abstract fun bindActivityRecorder(impl: RoomActivityRecorder): ActivityRecorder

    @Binds
    @Singleton
    abstract fun bindOtpConfigRepository(impl: RoomOtpConfigRepository): OtpConfigRepository

    @Binds
    @Singleton
    abstract fun bindSearchIndexMaintenance(impl: BlindIndexMaintenance): SearchIndexMaintenance

    @Binds
    @Singleton
    abstract fun bindFaviconRepository(impl: FaviconRepositoryImpl): FaviconRepository

    @Binds
    @Singleton
    abstract fun bindCredentialServiceRepository(impl: CredentialServiceRepositoryImpl): CredentialServiceRepository

    @Binds
    @Singleton
    abstract fun bindVaultDatabaseCleaner(
        impl: VaultDatabaseCleanerImpl
    ): VaultDatabaseCleaner
}
