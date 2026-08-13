package com.aozijx.passly.core.autofill.di

import com.aozijx.passly.core.autofill.dispatcher.FillRequestDispatcher
import com.aozijx.passly.core.autofill.matcher.FieldMatchStrategy
import com.aozijx.passly.core.autofill.matcher.HeuristicMatchStrategy
import com.aozijx.passly.core.autofill.matcher.StrictMatchStrategy
import com.aozijx.passly.core.autofill.pipeline.CandidateResolver
import com.aozijx.passly.core.autofill.pipeline.ResponseFactory
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.data.settings.port.AppSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
internal abstract class AutofillModule {

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
