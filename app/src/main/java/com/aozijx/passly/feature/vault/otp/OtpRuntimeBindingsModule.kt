package com.aozijx.passly.feature.vault.otp

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OtpRuntimeBindingsModule {
    @Binds
    @Singleton
    abstract fun bindOtpCodeRuntime(implementation: OtpCodeRuntime): OtpCodeRuntimePort

    @Binds
    @Singleton
    abstract fun bindOtpRuntimeInvalidator(implementation: OtpCodeRuntime): OtpRuntimeInvalidator
}
