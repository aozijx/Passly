package com.aozijx.passly.app.cache

import com.aozijx.passly.feature.settings.general.AppCacheStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CacheModule {
    @Binds
    abstract fun bindAppCacheStore(
        implementation: AndroidAppCacheStore,
    ): AppCacheStore
}