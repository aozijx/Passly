package com.aozijx.passly.app.di.security

import com.aozijx.passly.security.search.DefaultTokenizer
import com.aozijx.passly.security.search.Tokenizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 加密相关绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    internal abstract fun bindTokenizer(impl: DefaultTokenizer): Tokenizer
}
