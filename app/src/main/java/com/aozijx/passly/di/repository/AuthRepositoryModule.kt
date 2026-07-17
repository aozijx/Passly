package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.security.AuthRepositoryImpl
import com.aozijx.passly.data.repository.security.RecoveryCodeRepositoryImpl
import com.aozijx.passly.domain.repository.security.AuthRepository
import com.aozijx.passly.domain.repository.security.RecoveryCodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    internal abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    internal abstract fun bindRecoveryCodeRepository(
        impl: RecoveryCodeRepositoryImpl
    ): RecoveryCodeRepository
}
