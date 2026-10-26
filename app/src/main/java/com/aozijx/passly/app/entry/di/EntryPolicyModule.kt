package com.aozijx.passly.app.entry.di

import com.aozijx.passly.domain.entry.policy.DefaultEntryFieldReader
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import dagger.Provides
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App 仅连接 Domain 的纯字段读取规则。
 */
@Module
@InstallIn(SingletonComponent::class)
object EntryPolicyModule {

    @Provides
    @Singleton
    fun provideEntryFieldReader(): EntryFieldReader = DefaultEntryFieldReader()
}
