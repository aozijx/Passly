package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.activity.ActivityRepositoryImpl
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepositoryImpl
import com.aozijx.passly.data.repository.entry.RecordEntryUsageFacadeImpl
import com.aozijx.passly.data.repository.entry.VaultEntryRepositoryImpl
import com.aozijx.passly.data.repository.favicon.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.lookup.LookupRepositoryImpl
import com.aozijx.passly.data.repository.otp.OtpRepositoryImpl
import com.aozijx.passly.data.repository.snapshot.SnapshotRepositoryImpl
import com.aozijx.passly.domain.repository.activity.ActivityRepository
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.repository.entry.RecordEntryUsageFacade
import com.aozijx.passly.domain.repository.entry.VaultEntryRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.repository.lookup.LookupRepository
import com.aozijx.passly.domain.repository.otp.OtpRepository
import com.aozijx.passly.domain.repository.snapshot.SnapshotRepository
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
    abstract fun bindVaultEntryRepository(impl: VaultEntryRepositoryImpl): VaultEntryRepository

    @Binds
    @Singleton
    abstract fun bindRecordEntryUsageFacade(impl: RecordEntryUsageFacadeImpl): RecordEntryUsageFacade

    @Binds
    @Singleton
    abstract fun bindCredentialServiceRepository(impl: CredentialServiceRepositoryImpl): CredentialServiceRepository

    @Binds
    @Singleton
    abstract fun bindLookupRepository(impl: LookupRepositoryImpl): LookupRepository

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindOtpRepository(impl: OtpRepositoryImpl): OtpRepository

    @Binds
    @Singleton
    abstract fun bindFaviconRepository(impl: FaviconRepositoryImpl): FaviconRepository

    @Binds
    @Singleton
    abstract fun bindSnapshotRepository(impl: SnapshotRepositoryImpl): SnapshotRepository
}
