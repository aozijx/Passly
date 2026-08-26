package com.aozijx.passly.app.database.backup

import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupPersistenceAdapterModule {
    @Binds
    @Singleton
    abstract fun bindBackupSnapshotGateway(
        implementation: RoomBackupSnapshotGateway,
    ): BackupSnapshotGateway
}
