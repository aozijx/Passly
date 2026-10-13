package com.aozijx.passly.data.repository.autofill

import com.aozijx.passly.domain.autofill.port.AutofillCandidateRepository
import com.aozijx.passly.domain.autofill.port.AutofillCredentialResolver
import com.aozijx.passly.domain.autofill.port.AutofillEntryWriter
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
    abstract fun bindAutofillCandidateRepository(
        impl: RoomAutofillCandidateRepository,
    ): AutofillCandidateRepository

    @Binds
    @Singleton
    abstract fun bindAutofillCredentialResolver(
        impl: RoomAutofillCredentialResolver,
    ): AutofillCredentialResolver

    @Binds
    @Singleton
    abstract fun bindAutofillEntryWriter(
        impl: RoomAutofillEntryWriter,
    ): AutofillEntryWriter
}
