package com.aozijx.passly.app.di.security

import com.aozijx.passly.security.authentication.KdfRunner
import com.aozijx.passly.security.authentication.SingleThreadKdfRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthenticationExecutionModule {
    @Provides
    @Singleton
    fun provideKdfRunner(): KdfRunner = SingleThreadKdfRunner()
}
