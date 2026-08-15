package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.data.repository.autofill.CredentialServiceRepository
import com.aozijx.passly.data.repository.autofill.AutofillStatusRepositoryImpl
import com.aozijx.passly.data.repository.autofill.CredentialServiceRepositoryImpl
import com.aozijx.passly.data.repository.autofill.AutofillStatusRepository
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
