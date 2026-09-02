package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.domain.autofill.port.AutofillCredentialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AutofillPersistenceModule {

    @Binds
    @Singleton
    abstract fun bindAutofillCredentialRepository(
        impl: AutofillCredentialRepositoryImpl,
    ): AutofillCredentialRepository
}
