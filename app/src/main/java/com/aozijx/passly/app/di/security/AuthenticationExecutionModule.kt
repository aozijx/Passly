package com.aozijx.passly.app.di.security

import android.content.Context
import android.hardware.biometrics.BiometricManager
import com.aozijx.passly.security.authentication.KdfRunner
import com.aozijx.passly.security.authentication.SingleThreadKdfRunner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthenticationExecutionModule {
    @Provides
    @Singleton
    fun provideKdfRunner(): KdfRunner = SingleThreadKdfRunner()

    @Provides
    @Singleton
    fun provideBiometricManager(
        @ApplicationContext context: Context
    ): BiometricManager = context.getSystemService(BiometricManager::class.java)
}
