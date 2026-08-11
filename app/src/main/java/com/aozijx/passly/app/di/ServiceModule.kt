package com.aozijx.passly.app.di

import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy
import com.aozijx.passly.core.autofill.matcher.HeuristicMatchStrategy
import com.aozijx.passly.core.autofill.matcher.StrictMatchStrategy
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.autofill.pipeline.ResponseFactory
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
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

    companion object {
        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        @Provides
        @Singleton
        @Heuristic
        fun provideHeuristicDispatcher(
            sessionState: SecureSessionAccessState,
            candidateResolver: CandidateResolver,
            @Heuristic fieldMatchStrategy: FieldMatchStrategy,
            responseFactory: ResponseFactory,
            settingsRepository: AppSettingsRepository,
        ): FillRequestDispatcher = FillRequestDispatcher(
            sessionState,
            candidateResolver,
            fieldMatchStrategy,
            responseFactory,
            settingsRepository,
        )

        @Provides
        @Singleton
        @Strict
        fun provideStrictDispatcher(
            sessionState: SecureSessionAccessState,
            candidateResolver: CandidateResolver,
            @Strict fieldMatchStrategy: FieldMatchStrategy,
            responseFactory: ResponseFactory,
            settingsRepository: AppSettingsRepository,
        ): FillRequestDispatcher = FillRequestDispatcher(
            sessionState,
            candidateResolver,
            fieldMatchStrategy,
            responseFactory,
            settingsRepository,
        )
    }
}
