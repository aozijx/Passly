package com.aozijx.passly.core.platform.clipboard

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ClipboardModule {
    @Binds
    @Singleton
    abstract fun bindSecureClipboard(implementation: AndroidSecureClipboard): SecureClipboard
}
