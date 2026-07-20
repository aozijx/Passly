package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.autofill.CredentialServiceRepositoryImpl
import com.aozijx.passly.data.repository.vault.ActivityRepositoryImpl
import com.aozijx.passly.data.repository.vault.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.vault.LookupRepositoryImpl
import com.aozijx.passly.data.repository.vault.OtpRepositoryImpl
import com.aozijx.passly.data.repository.vault.SnapshotRepositoryImpl
import com.aozijx.passly.data.repository.vault.VaultRepositoryImpl
import com.aozijx.passly.domain.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.repository.otp.OtpRepository
import com.aozijx.passly.domain.repository.vault.ActivityRepository
import com.aozijx.passly.domain.repository.vault.LookupRepository
import com.aozijx.passly.domain.repository.vault.SnapshotRepository
import com.aozijx.passly.domain.repository.vault.VaultRepository
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
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

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
