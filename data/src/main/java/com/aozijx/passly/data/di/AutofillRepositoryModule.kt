package com.aozijx.passly.data.di

import com.aozijx.passly.data.repository.autofill.AutofillStatusRepositoryImpl
import com.aozijx.passly.data.autofill.port.AutofillStatusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AutofillRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAutofillStatusRepository(impl: AutofillStatusRepositoryImpl): AutofillStatusRepository
}
