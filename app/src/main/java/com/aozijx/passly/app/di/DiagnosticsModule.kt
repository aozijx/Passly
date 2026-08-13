package com.aozijx.passly.app.di

import com.aozijx.passly.app.diagnostics.DiagnosticsRuntimeController
import com.aozijx.passly.app.diagnostics.TelemetryAppErrorReporter
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.reporting.AppErrorReporter
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
        impl: TelemetryAppErrorReporter
    ): AppErrorReporter

    companion object {
        @dagger.Provides
        @Singleton
        fun provideTelemetryReporter(
            runtime: DiagnosticsRuntimeController
        ): TelemetryReporter = runtime.reporter
    }
}
