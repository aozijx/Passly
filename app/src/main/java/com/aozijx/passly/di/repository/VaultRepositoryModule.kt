package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.activity.ActivityRepositoryImpl
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepositoryImpl
import com.aozijx.passly.data.repository.command.EntryCommandHandler
import com.aozijx.passly.data.repository.entry.QueryRepositoryImpl
import com.aozijx.passly.data.repository.favicon.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.lookup.LookupRepositoryImpl
import com.aozijx.passly.data.repository.otp.OtpRepositoryImpl
import com.aozijx.passly.domain.repository.activity.CommandActivityRepository
import com.aozijx.passly.domain.repository.activity.QueryActivityRepository
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.repository.entry.EntryCommands
import com.aozijx.passly.domain.repository.entry.QueryRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.repository.lookup.LookupRepository
import com.aozijx.passly.domain.repository.otp.OtpRepository
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
    abstract fun bindQueryRepository(impl: QueryRepositoryImpl): QueryRepository

    @Binds
    @Singleton
    abstract fun bindCredentialServiceRepository(impl: CredentialServiceRepositoryImpl): CredentialServiceRepository

    @Binds
    @Singleton
    abstract fun bindLookupRepository(impl: LookupRepositoryImpl): LookupRepository

    @Binds
    @Singleton
    abstract fun bindQueryActivityRepository(impl: ActivityRepositoryImpl): QueryActivityRepository

    @Binds
    @Singleton
    abstract fun bindCommandActivityRepository(impl: ActivityRepositoryImpl): CommandActivityRepository

    @Binds
    @Singleton
    abstract fun bindOtpRepository(impl: OtpRepositoryImpl): OtpRepository

    @Binds
    @Singleton
    abstract fun bindFaviconRepository(impl: FaviconRepositoryImpl): FaviconRepository

    @Binds
    @Singleton
    abstract fun bindEntryCommands(impl: EntryCommandHandler): EntryCommands

    @Binds
    @Singleton
    abstract fun bindVaultDatabaseCleaner(
        impl: com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleanerImpl
    ): com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleaner
}
