package com.aozijx.passly.security.di

import com.aozijx.passly.core.crypto.FieldKeyProvider
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveKeyScope
import com.aozijx.passly.security.dek.SensitiveDataKeyManager
import com.aozijx.passly.domain.access.port.SensitiveKeyFreshnessState
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SensitiveKeyModule {
    @Binds
    @Singleton
    abstract fun bindFieldKeyProvider(implementation: FieldKeyManager): FieldKeyProvider

    @Binds
    @Singleton
    abstract fun bindSensitiveKeyFreshnessState(
        implementation: SensitiveDataKeyManager,
    ): SensitiveKeyFreshnessState

    companion object {
        @Provides
        @Singleton
        @SensitiveKeyScope
        fun provideSensitiveKeyScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
