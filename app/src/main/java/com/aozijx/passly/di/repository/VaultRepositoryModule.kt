package com.aozijx.passly.di.repository

import com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleaner
import com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleanerImpl
import com.aozijx.passly.data.repository.activity.RoomActivityQueryRepository
import com.aozijx.passly.data.repository.activity.RoomActivityRecorder
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepositoryImpl
import com.aozijx.passly.data.repository.entry.RoomEntryCommandRepository
import com.aozijx.passly.data.repository.entry.RoomEntryListQueryRepository
import com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository
import com.aozijx.passly.data.repository.favicon.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.otp.RoomOtpConfigRepository
import com.aozijx.passly.data.repository.search.BlindIndexMaintenance
import com.aozijx.passly.domain.repository.activity.ActivityQueryRepository
import com.aozijx.passly.domain.repository.activity.ActivityRecorder
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.repository.entry.EntryCommandRepository
import com.aozijx.passly.domain.repository.entry.EntryListQueryRepository
import com.aozijx.passly.domain.repository.entry.EntryQueryRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.repository.otp.OtpConfigRepository
import com.aozijx.passly.domain.repository.search.SearchIndexMaintenance
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEntryQueryRepository(impl: RoomEntryQueryRepository): EntryQueryRepository

    @Binds
    @Singleton
    abstract fun bindEntryCommandRepository(impl: RoomEntryCommandRepository): EntryCommandRepository

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
