package com.aozijx.passly.data.repository.access

import com.aozijx.passly.domain.access.port.AuthenticationAttemptStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthenticationAttemptPersistenceModule {
    @Binds
    @Singleton
    abstract fun bindAuthenticationAttemptStore(impl: ProtoAuthenticationAttemptStore): AuthenticationAttemptStore
}
