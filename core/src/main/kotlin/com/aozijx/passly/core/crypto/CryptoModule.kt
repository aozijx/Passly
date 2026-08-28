package com.aozijx.passly.core.crypto

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CryptoModule {
    @Binds
    @Singleton
    abstract fun bindCryptoEngine(implementation: AesGcmCryptoEngine): CryptoEngine
}
