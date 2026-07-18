package com.aozijx.passly.di.security

import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.security.authentication.DefaultAuthenticationManager
import com.aozijx.passly.security.authentication.VaultSessionController
import com.aozijx.passly.domain.authentication.AuthenticationMethodProvisioner
import com.aozijx.passly.security.authentication.DefaultAuthenticationMethodProvisioner
import com.aozijx.passly.domain.authentication.RecoveryCodeDraftFactory
import com.aozijx.passly.security.authentication.DefaultRecoveryCodeDraftFactory
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
    ): RecoveryCodeDraftFactory

    @Binds
    @Singleton
    internal abstract fun bindAuthenticationManager(
        impl: DefaultAuthenticationManager
    ): AuthenticationManager

    @Binds
    @Singleton
    internal abstract fun bindVaultAccessState(
        impl: VaultSessionController
    ): VaultAccessState

    @Binds
    @Singleton
    internal abstract fun bindAuthenticationMethodProvisioner(
        impl: DefaultAuthenticationMethodProvisioner
    ): AuthenticationMethodProvisioner
}
