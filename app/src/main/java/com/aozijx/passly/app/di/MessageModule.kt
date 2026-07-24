package com.aozijx.passly.app.di

import com.aozijx.passly.core.message.AppMessagePublisher
import com.aozijx.passly.core.message.DefaultAppMessagePublisher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessageModule {
    @Provides
    @Singleton
    fun provideAppMessagePublisher(): AppMessagePublisher = DefaultAppMessagePublisher
}
