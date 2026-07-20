package com.aozijx.passly.di.repository

import com.aozijx.passly.data.repository.autofill.AutofillStatusRepositoryImpl
import com.aozijx.passly.domain.repository.autofill.AutofillStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AutofillRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAutofillStatusRepository(impl: AutofillStatusRepositoryImpl): AutofillStatusRepository
}
