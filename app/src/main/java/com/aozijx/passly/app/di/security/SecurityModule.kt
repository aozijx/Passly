package com.aozijx.passly.app.di.security

import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.domain.access.port.AuthenticationMethodProvisioner
import com.aozijx.passly.domain.access.model.RecoveryCredentialFactory
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.security.authentication.DefaultAuthenticationManager
import com.aozijx.passly.security.authentication.DefaultAuthenticationMethodProvisioner
import com.aozijx.passly.security.authentication.DefaultRecoveryCodeDraftFactory
import com.aozijx.passly.security.authentication.VaultSessionController
import com.aozijx.passly.security.authorization.AuthorizationPermitRegistry
import com.aozijx.passly.security.authorization.DefaultAuthorizationGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 安全域绑定。
 *
 * 认证编排和 Vault 会话状态分别通过领域契约暴露，调用方不依赖安全层实现。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    @Singleton
    internal abstract fun bindRecoveryCodeDraftFactory(
        impl: DefaultRecoveryCodeDraftFactory
    ): RecoveryCredentialFactory

    @Binds
    @Singleton
    internal abstract fun bindAuthenticationManager(
        impl: DefaultAuthenticationManager
    ): AuthenticationManager

    @Binds
    @Singleton
    internal abstract fun bindAuthorizationGate(
        impl: DefaultAuthorizationGate
    ): AuthorizationGate

    @Binds
    @Singleton
    internal abstract fun bindAuthorizationPermitVerifier(
        impl: AuthorizationPermitRegistry
    ): AuthorizationPermitVerifier

    @Binds
    @Singleton
    internal abstract fun bindAuthorizationPermitRevoker(
        impl: AuthorizationPermitRegistry
    ): AuthorizationPermitRevoker

    @Binds
    @Singleton
    internal abstract fun bindSecureSessionAccessState(
        impl: VaultSessionController
    ): SecureSessionAccessState

    @Binds
    @Singleton
    internal abstract fun bindAuthenticationMethodProvisioner(
        impl: DefaultAuthenticationMethodProvisioner
    ): AuthenticationMethodProvisioner
}
