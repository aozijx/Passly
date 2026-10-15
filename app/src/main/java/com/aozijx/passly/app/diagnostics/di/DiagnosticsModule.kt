package com.aozijx.passly.app.diagnostics.di

import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.app.observability.ObservabilityAppErrorReporter
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.core.telemetry.TelemetryReporter
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
    internal abstract fun bindAppErrorReporter(
        impl: ObservabilityAppErrorReporter
    ): AppErrorReporter

    companion object {
        @dagger.Provides
        @Singleton
        fun provideTelemetryReporter(
            runtime: DiagnosticsRuntimeController
        ): TelemetryReporter = runtime.reporter
    }
}
