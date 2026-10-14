package com.aozijx.passly.data.observability.di

import com.aozijx.passly.core.observability.sink.LogRecordStoreFactory
import com.aozijx.passly.data.observability.EncryptedLogRecordStoreFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ObservabilityPersistenceModule {
    @Binds
    abstract fun bindLogRecordStoreFactory(
        implementation: EncryptedLogRecordStoreFactory,
    ): LogRecordStoreFactory
}
