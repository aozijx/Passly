package com.aozijx.passly.di

import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.diagnostics.AppLogger
import com.aozijx.passly.core.message.AppMessagePublisher
import com.aozijx.passly.core.message.DefaultAppMessagePublisher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {
    @Provides
    @Singleton
    fun provideAppLogger(): AppLogger = AppLog

    @Provides
    @Singleton
    fun provideAppMessagePublisher(): AppMessagePublisher = DefaultAppMessagePublisher
}
