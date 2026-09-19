package com.aozijx.passly.app.entry.favicon

import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FaviconImageProcessorModule {
    @Binds
    @Singleton
    abstract fun bindFaviconImageProcessor(
        implementation: DefaultFaviconImageProcessor,
    ): FaviconImageProcessor
}
