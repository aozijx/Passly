package com.aozijx.passly.di.security

import com.aozijx.passly.feature.auth.VerificationGateway
import com.aozijx.passly.feature.auth.DefaultVerificationGateway
import com.aozijx.passly.security.session.SessionStateProvider
import com.aozijx.passly.security.session.UserSessionManager
import com.aozijx.passly.security.session.UserSessionManagerImpl
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.security.authentication.DefaultAuthenticationManager
import com.aozijx.passly.security.authentication.VaultSessionController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 安全域绑定。
 *
 * SessionStateProvider 管理 Vault 访问状态，Repository 层通过此接口查询。
 * UserSessionManager 管理认证会话生命周期（锁定/解锁/空闲超时）。
 * VerificationGateway 封装生物识别及 AppPassword 认证流程。
 *
 * 依赖方向：Security -> DatabaseSession -> Repository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    @Singleton
    internal abstract fun bindSessionManager(impl: UserSessionManagerImpl): UserSessionManager

    @Binds
    @Singleton
    internal abstract fun bindSessionStateProvider(impl: UserSessionManagerImpl): SessionStateProvider

    @Binds
    @Singleton
    internal abstract fun bindVerificationGateway(impl: DefaultVerificationGateway): VerificationGateway

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
}
