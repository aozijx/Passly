package com.aozijx.passly.core.di

import com.aozijx.passly.core.auth.VerificationGateway
import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy
import com.aozijx.passly.core.autofill.matcher.HeuristicMatchStrategy
import com.aozijx.passly.core.autofill.matcher.StrictMatchStrategy
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.autofill.pipeline.ResponseFactory
import com.aozijx.passly.security.crypto.VaultLockManager
import com.aozijx.passly.ui.features.verification.VerificationGatewayImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Strict

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Heuristic

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    @Strict
    abstract fun bindStrictStrategy(impl: StrictMatchStrategy): FieldMatchStrategy

    @Binds
    @Singleton
    @Heuristic
    abstract fun bindHeuristicStrategy(impl: HeuristicMatchStrategy): FieldMatchStrategy

    @Binds
    @Singleton
    abstract fun bindVerificationGateway(impl: VerificationGatewayImpl): VerificationGateway

    companion object {
        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main)

        @Provides
        @Singleton
        @Heuristic
        fun provideHeuristicDispatcher(
            vaultLockManager: VaultLockManager,
            candidateResolver: CandidateResolver,
            @Heuristic fieldMatchStrategy: FieldMatchStrategy,
            responseFactory: ResponseFactory,
        ): FillRequestDispatcher = FillRequestDispatcher(
            vaultLockManager,
            candidateResolver,
            fieldMatchStrategy,
            responseFactory,
        )

        @Provides
        @Singleton
        @Strict
        fun provideStrictDispatcher(
            vaultLockManager: VaultLockManager,
            candidateResolver: CandidateResolver,
            @Strict fieldMatchStrategy: FieldMatchStrategy,
            responseFactory: ResponseFactory,
        ): FillRequestDispatcher = FillRequestDispatcher(
            vaultLockManager,
            candidateResolver,
            fieldMatchStrategy,
            responseFactory,
        )
    }
}