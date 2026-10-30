package com.aozijx.passly.app.observability.di

import android.content.Context
import android.os.SystemClock
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.app.observability.AndroidDiagnosticsExportStorageGateway
import com.aozijx.passly.app.observability.AndroidExportContextProvider
import com.aozijx.passly.app.observability.AndroidKeystoreIdentifierHasher
import com.aozijx.passly.app.observability.AndroidPreviousProcessExitGateway
import com.aozijx.passly.app.observability.AndroidProcessTerminator
import com.aozijx.passly.app.observability.AndroidTraceSink
import com.aozijx.passly.app.observability.CrashHandlerRegistry
import com.aozijx.passly.app.observability.CrashRecordCaptureSink
import com.aozijx.passly.app.observability.DiagnosticsExportStorageGateway
import com.aozijx.passly.app.observability.DiagnosticsLogRecordingPolicy
import com.aozijx.passly.app.observability.ExportClock
import com.aozijx.passly.app.observability.ExportContextProvider
import com.aozijx.passly.app.observability.LogcatSink
import com.aozijx.passly.app.observability.LocalAlertPolicy
import com.aozijx.passly.app.observability.ObservabilityExportService
import com.aozijx.passly.app.observability.ObservabilityRuntimeController
import com.aozijx.passly.app.observability.PolicyLogRecordStore
import com.aozijx.passly.app.observability.PlatformCrashHandlerRegistry
import com.aozijx.passly.app.observability.PreviousExitSource
import com.aozijx.passly.app.observability.PreviousProcessExitReader
import com.aozijx.passly.app.observability.ProcessTerminator
import com.aozijx.passly.app.observability.SharedPreferencesPreviousExitCursor
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.metrics.EventAggregator
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.reporting.AppErrorReporter
import com.aozijx.passly.app.observability.ObservabilityAppErrorReporter
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.core.observability.sink.LogRecordStoreFactory
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsExportGateway
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ObservabilityModule {
    @Provides
    @Singleton
    fun provideLogRecordStore(
        factory: LogRecordStoreFactory,
        policy: DiagnosticsLogRecordingPolicy,
    ): LogRecordStore = PolicyLogRecordStore(factory.create(), policy)

    @Provides
    @Singleton
    fun provideCrashCaptureSink(): CrashRecordCaptureSink = CrashRecordCaptureSink()

    @Provides
    @Singleton
    fun provideLogger(
        store: LogRecordStore,
        crashCapture: CrashRecordCaptureSink,
        eventAggregator: EventAggregator,
        localAlertPolicy: LocalAlertPolicy,
    ): Logger {
        val idGenerator = SecureObservabilityIdGenerator()
        val processSessionId = ProcessSessionId(randomOpaqueId())
        return DefaultLogger(
            sanitizer = PrivacyPolicy(
                identifierHasher = AndroidKeystoreIdentifierHasher(),
                violationBehavior = if (BuildConfig.DEBUG) {
                    ContractViolationBehavior.THROW
                } else {
                    ContractViolationBehavior.REJECT
                },
            ),
            sinks = listOf(crashCapture, localAlertPolicy, LogcatSink(BuildConfig.DEBUG), store),
            lifecycleSinks = listOf(AndroidTraceSink()),
            idGenerator = idGenerator,
            timeSource = object : ObservabilityTimeSource {
                override fun wallTimeMs(): Long = System.currentTimeMillis()
                override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
            },
            processSessionProvider = ProcessSessionProvider { processSessionId },
            secureSessionProvider = SecureSessionProvider { null },
            aggregator = eventAggregator,
        )
    }

    @Provides
    @Singleton
    fun provideEventAggregator(): EventAggregator = EventAggregator()

    @Provides
    fun provideCrashHandlerRegistry(): CrashHandlerRegistry = PlatformCrashHandlerRegistry

    @Provides
    fun provideProcessTerminator(): ProcessTerminator = AndroidProcessTerminator

    @Provides
    @Singleton
    fun providePreviousExitSource(
        @ApplicationContext context: Context,
    ): PreviousExitSource = PreviousProcessExitReader(
        gateway = AndroidPreviousProcessExitGateway(context),
        cursor = SharedPreferencesPreviousExitCursor(context),
    )

    @Provides
    @Singleton
    fun provideExportContextProvider(
        @ApplicationContext context: Context,
    ): ExportContextProvider = AndroidExportContextProvider(context)

    @Provides
    fun provideExportClock(): ExportClock = ExportClock(System::currentTimeMillis)

    @Provides
    @Singleton
    fun provideExportStorageGateway(
        @ApplicationContext context: Context,
    ): DiagnosticsExportStorageGateway = AndroidDiagnosticsExportStorageGateway(context)

    @Provides
    fun provideDiagnosticsLogStore(
        controller: ObservabilityRuntimeController,
    ): DiagnosticsLogStore = controller

    @Provides
    fun provideDiagnosticsExportGateway(
        service: ObservabilityExportService,
    ): DiagnosticsExportGateway = service

    @Provides
    fun provideAppErrorReporter(
        reporter: ObservabilityAppErrorReporter,
    ): AppErrorReporter = reporter

    private fun randomOpaqueId(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        ByteArray(PROCESS_SESSION_ID_BYTES).also(SecureRandom()::nextBytes),
    )

    private const val PROCESS_SESSION_ID_BYTES = 16
}
