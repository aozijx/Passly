package com.aozijx.passly.di.security

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 加密相关绑定。
 *
 * 后续启用：
 * - FieldEncryptor
 * - BlobSerializer
 * - SearchKeyProvider
 * - AadProvider
 * - HistoryEncryptor
 * - AttachmentEncryptor
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    // 后续启用：
    //
    // @Binds @Singleton
    // internal abstract fun bindFieldEncryptor(impl: FieldEncryptorImpl): FieldEncryptor
    //
    // @Binds @Singleton
    // internal abstract fun bindBlobSerializer(impl: BlobSerializerImpl): BlobSerializer
    //
    // @Binds @Singleton
    // internal abstract fun bindSearchKeyProvider(impl: SearchKeyProviderImpl): SearchKeyProvider
    //
    // @Binds @Singleton
    // internal abstract fun bindAadProvider(impl: AadProviderImpl): AadProvider
    //
    // @Binds @Singleton
    // internal abstract fun bindHistoryEncryptor(impl: HistoryEncryptorImpl): HistoryEncryptor
    //
    // @Binds @Singleton
    // internal abstract fun bindAttachmentEncryptor(impl: AttachmentEncryptorImpl): AttachmentEncryptor
}
