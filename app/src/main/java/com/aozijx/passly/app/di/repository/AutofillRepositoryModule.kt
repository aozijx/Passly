package com.aozijx.passly.app.di.repository

import com.aozijx.passly.data.repository.autofill.AutofillStatusRepositoryImpl
import com.aozijx.passly.domain.autofill.repository.AutofillStatusRepository
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
