package com.aozijx.passly.security.authentication.di

import android.content.Context
import android.hardware.biometrics.BiometricManager
import com.aozijx.passly.core.session.DekSessionKeySource
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.runtime.session.SessionKeySource
import com.aozijx.passly.security.authentication.DefaultAuthenticationManager
import com.aozijx.passly.security.authentication.DefaultAuthenticationMethodProvisioner
import com.aozijx.passly.security.authentication.DefaultRecoveryCodeDraftFactory
import com.aozijx.passly.security.authentication.KdfRunner
import com.aozijx.passly.security.authentication.SingleThreadKdfRunner
import com.aozijx.passly.security.authentication.VaultSessionController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Composition for authentication, its secure session, and method execution dependencies. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthenticationModule {
    @Binds
    @Singleton
    abstract fun bindAuthenticationManager(
        impl: DefaultAuthenticationManager,
    ): AuthenticationManager

    @Binds
    @Singleton
    abstract fun bindAuthenticationMethodProvisioner(
        impl: DefaultAuthenticationMethodProvisioner,
    ): AuthenticationMethodProvisioner

    @Binds
    @Singleton
    abstract fun bindRecoveryCredentialFactory(
        impl: DefaultRecoveryCodeDraftFactory,
    ): RecoveryCredentialFactory

    @Binds
    @Singleton
    abstract fun bindSecureSessionAccessState(
        impl: VaultSessionController,
    ): SecureSessionAccessState

    @Binds
    @Singleton
    abstract fun bindSessionKeySource(
        impl: DekSessionKeySource,
    ): SessionKeySource

    companion object {
        @Provides
        @Singleton
        fun provideKdfRunner(): KdfRunner = SingleThreadKdfRunner()

        @Provides
        @Singleton
        fun provideBiometricManager(
            @ApplicationContext context: Context,
        ): BiometricManager = context.getSystemService(BiometricManager::class.java)
    }
}
