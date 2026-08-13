package com.aozijx.passly.feature.backup.internal.archive.di

import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.backup.internal.archive.AndroidBackupFileStore
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveServiceImpl
import com.aozijx.passly.feature.backup.internal.archive.format.BackupExportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.BackupImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.bitwarden.BitwardenJsonImportAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.encrypted.PasslyEncryptedFormatAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.json.PasslyJsonFormatAdapter
import com.aozijx.passly.feature.backup.internal.archive.format.text.ReadableTextFormatAdapter
import com.aozijx.passly.feature.backup.internal.archive.io.BackupFileStore
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupExportStorageSupport
import com.aozijx.passly.domain.backup.service.BackupArchiveService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupModule {

    @Binds
    @Singleton
    internal abstract fun bindBackupArchiveService(
        impl: BackupArchiveServiceImpl
    ): BackupArchiveService

    @Binds
    @Singleton
    internal abstract fun bindBackupStorageSupport(
        impl: BackupExportStorageSupport
    ): BackupStorageSupport

    @Binds
    @Singleton
    internal abstract fun bindBackupFileStore(
        impl: AndroidBackupFileStore
    ): BackupFileStore

    @Binds
    @IntoSet
    internal abstract fun bindEncryptedExporter(
        adapter: PasslyEncryptedFormatAdapter
    ): BackupExportAdapter

    @Binds
    @IntoSet
    internal abstract fun bindEncryptedImporter(
        adapter: PasslyEncryptedFormatAdapter
    ): BackupImportAdapter

    @Binds
    @IntoSet
    internal abstract fun bindJsonExporter(
        adapter: PasslyJsonFormatAdapter
    ): BackupExportAdapter

    @Binds
    @IntoSet
    internal abstract fun bindJsonImporter(
        adapter: PasslyJsonFormatAdapter
    ): BackupImportAdapter

    @Binds
    @IntoSet
    internal abstract fun bindBitwardenJsonImporter(
        adapter: BitwardenJsonImportAdapter
    ): BackupImportAdapter

    @Binds
    @IntoSet
    internal abstract fun bindTextExporter(
        adapter: ReadableTextFormatAdapter
    ): BackupExportAdapter
}
