package com.aozijx.passly.feature.autofill.internal.di

import com.aozijx.passly.domain.autofill.port.AutofillHintProvider
import com.aozijx.passly.domain.autofill.port.FieldMatchStrategy
import com.aozijx.passly.feature.autofill.internal.HeuristicMatchStrategy
import com.aozijx.passly.feature.autofill.internal.StrictMatchStrategy
import com.aozijx.passly.feature.autofill.internal.matcher.AutofillHintProviderImpl
import dagger.Binds
import dagger.Module
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
}
