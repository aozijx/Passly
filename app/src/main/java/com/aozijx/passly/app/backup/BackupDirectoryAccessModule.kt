package com.aozijx.passly.app.backup

import com.aozijx.passly.feature.settings.backup.BackupDirectoryAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupDirectoryAccessModule {
    @Binds
    abstract fun bindBackupDirectoryAccess(
        implementation: AndroidBackupDirectoryAccess,
    ): BackupDirectoryAccess
}
