package com.aozijx.passly.data.diagnostics.di

import com.aozijx.passly.core.telemetry.TelemetryPolicyController
import com.aozijx.passly.core.telemetry.TelemetryFileStoreFactory
import com.aozijx.passly.data.diagnostics.EncryptedTelemetryFileStoreFactory
import com.aozijx.passly.data.local.datastore.diagnostics.ProtoTelemetryPolicyController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TelemetryPersistenceModule {
    @Binds
    @Singleton
    abstract fun bindTelemetryFileStoreFactory(
        impl: EncryptedTelemetryFileStoreFactory
    ): TelemetryFileStoreFactory

    @Binds
    @Singleton
    abstract fun bindTelemetryPolicyController(
        impl: ProtoTelemetryPolicyController
    ): TelemetryPolicyController
}
