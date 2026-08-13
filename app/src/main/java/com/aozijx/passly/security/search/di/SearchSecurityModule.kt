package com.aozijx.passly.security.search.di

import com.aozijx.passly.security.search.DefaultTokenizer
import com.aozijx.passly.security.search.Tokenizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 搜索索引使用的安全分词绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SearchSecurityModule {

    @Binds
    @Singleton
    internal abstract fun bindTokenizer(impl: DefaultTokenizer): Tokenizer
}
