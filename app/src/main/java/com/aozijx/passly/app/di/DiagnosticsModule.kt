package com.aozijx.passly.app.di

import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.core.telemetry.TelemetryEmitter
import com.aozijx.passly.core.telemetry.TelemetryPolicyController
import com.aozijx.passly.data.local.datastore.diagnostics.ProtoTelemetryPolicyController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {
    @Binds
    @Singleton
    internal abstract fun bindTelemetryPolicyController(
        impl: ProtoTelemetryPolicyController
    ): TelemetryPolicyController

    companion object {
        @dagger.Provides
        @Singleton
        fun provideTelemetryEmitter(
            runtime: DiagnosticsRuntimeController
        ): TelemetryEmitter = runtime.emitter
    }
}
