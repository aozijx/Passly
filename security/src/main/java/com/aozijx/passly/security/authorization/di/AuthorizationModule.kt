package com.aozijx.passly.security.authorization.di

import com.aozijx.passly.domain.access.model.MonotonicClock
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import com.aozijx.passly.security.authorization.AuthorizationPermitRegistry
import com.aozijx.passly.security.authorization.DefaultAuthorizationGate
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Composition for short-lived authorization permits and their monotonic clock. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthorizationModule {
    @Binds
    @Singleton
    abstract fun bindAuthorizationGate(
        impl: DefaultAuthorizationGate,
    ): AuthorizationGate

    @Binds
    @Singleton
    abstract fun bindAuthorizationPermitVerifier(
        impl: AuthorizationPermitRegistry,
    ): AuthorizationPermitVerifier

    @Binds
    @Singleton
    abstract fun bindAuthorizationPermitRevoker(
        impl: AuthorizationPermitRegistry,
    ): AuthorizationPermitRevoker

    companion object {
        @Provides
        @Singleton
        fun provideMonotonicClock(): MonotonicClock =
            MonotonicClock { System.nanoTime() / 1_000_000L }
    }
}
