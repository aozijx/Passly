package com.aozijx.passly.app.clipboard

import com.aozijx.passly.domain.clipboard.port.OwnedClipboardCleaner
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ClipboardModule {
    @Binds
    abstract fun bindSensitiveClipboardWriter(
        implementation: ClipboardCopyController,
    ): SensitiveClipboardWriter

    @Binds
    abstract fun bindOwnedClipboardCleaner(
        implementation: ClipboardCopyController,
    ): OwnedClipboardCleaner
}
