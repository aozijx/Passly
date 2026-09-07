package com.aozijx.passly.feature.autofill.internal.di

import com.aozijx.passly.domain.autofill.port.AutofillGrantStore
import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.autofill.internal.CandidateRetriever
import com.aozijx.passly.feature.autofill.internal.FillRequestDispatcher
import com.aozijx.passly.feature.autofill.internal.HeuristicMatchStrategy
import com.aozijx.passly.feature.autofill.internal.StrictMatchStrategy
import com.aozijx.passly.feature.autofill.internal.matcher.AutofillHintProviderImpl
import com.aozijx.passly.feature.autofill.shared.AutofillRequestSession
import com.aozijx.passly.feature.autofill.shared.AutofillSessionGrantStore
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

    @Binds
    @Singleton
    abstract fun bindAutofillHintProvider(impl: AutofillHintProviderImpl): AutofillHintProvider

    companion object {
        @Provides
        fun provideAutofillRequestSession(
            authenticationManager: AuthenticationManager,
            vaultAccessState: SecureSessionAccessState,
            grantStore: AutofillGrantStore,
        ): AutofillRequestSession = AutofillRequestSession(
            authenticationManager,
            vaultAccessState,
            grantStore,
        )

        @Provides
        @Singleton
        fun provideAutofillGrantStore(): AutofillGrantStore = AutofillSessionGrantStore()

        @Provides
        @Singleton
        @Heuristic
        fun provideHeuristicDispatcher(
            sessionState: SecureSessionAccessState,
            candidateRetriever: CandidateRetriever,
            settingsSource: InteractionSettingsSource,
            grantStore: AutofillGrantStore,
            @Heuristic fieldMatchStrategy: FieldMatchStrategy,
        ): FillRequestDispatcher = FillRequestDispatcher(
            sessionState,
            candidateRetriever,
            settingsSource,
            grantStore,
            fieldMatchStrategy
        )

        @Provides
        @Singleton
        @Strict
        fun provideStrictDispatcher(
            sessionState: SecureSessionAccessState,
            candidateRetriever: CandidateRetriever,
            settingsSource: InteractionSettingsSource,
            grantStore: AutofillGrantStore,
            @Strict fieldMatchStrategy: FieldMatchStrategy,
        ): FillRequestDispatcher = FillRequestDispatcher(
            sessionState,
            candidateRetriever,
            settingsSource,
            grantStore,
            fieldMatchStrategy
        )
    }
}
