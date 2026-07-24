package com.aozijx.passly.di

import com.aozijx.passly.data.backup.AndroidBackupFileStore
import com.aozijx.passly.data.backup.VaultBackupServiceImpl
import com.aozijx.passly.data.backup.format.BackupExportAdapter
import com.aozijx.passly.data.backup.format.BackupImportAdapter
import com.aozijx.passly.data.backup.format.bitwarden.BitwardenJsonImportAdapter
import com.aozijx.passly.data.backup.format.encrypted.PasslyEncryptedFormatAdapter
import com.aozijx.passly.data.backup.format.json.PasslyJsonFormatAdapter
import com.aozijx.passly.data.backup.format.text.ReadableTextFormatAdapter
import com.aozijx.passly.data.backup.io.BackupFileStore
import com.aozijx.passly.domain.backup.service.VaultBackupService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    internal abstract fun bindVaultBackupService(
        impl: VaultBackupServiceImpl
    ): VaultBackupService

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
