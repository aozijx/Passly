package com.aozijx.passly.di

import com.aozijx.passly.core.diagnostics.DiagnosticsPolicyController
import com.aozijx.passly.data.local.datastore.diagnostics.ProtoDiagnosticsPolicyController
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
    internal abstract fun bindDiagnosticsPolicyController(
        impl: ProtoDiagnosticsPolicyController
    ): DiagnosticsPolicyController
}
