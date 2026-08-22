package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.domain.autofill.port.AutofillStatusRepository
import com.aozijx.passly.domain.autofill.port.CredentialServiceRepository
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
    abstract fun bindAutofillStatusRepository(impl: AutofillStatusRepositoryImpl): AutofillStatusRepository

    @Binds
    @Singleton
    abstract fun bindCredentialServiceRepository(
        impl: CredentialServiceRepositoryImpl,
    ): CredentialServiceRepository
}
