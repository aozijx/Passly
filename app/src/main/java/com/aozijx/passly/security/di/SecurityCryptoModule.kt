package com.aozijx.passly.security.di

import com.aozijx.passly.core.crypto.AesGcmCryptoEngine
import com.aozijx.passly.core.crypto.CryptoEngine
import com.aozijx.passly.core.crypto.FieldKeyProvider
import com.aozijx.passly.security.dek.FieldKeyManager
import com.aozijx.passly.security.dek.SensitiveKeyScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SecurityCryptoModule {
    @Binds
    @Singleton
    abstract fun bindCryptoEngine(implementation: AesGcmCryptoEngine): CryptoEngine

    @Binds
    @Singleton
    abstract fun bindFieldKeyProvider(implementation: FieldKeyManager): FieldKeyProvider

    companion object {
        @dagger.Provides
        @Singleton
        @SensitiveKeyScope
        fun provideSensitiveKeyScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
