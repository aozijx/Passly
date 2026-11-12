package com.aozijx.passly.app.entry.creation

import com.aozijx.passly.domain.autofill.port.AutofillEntryCreation
import com.aozijx.passly.domain.entry.port.EntryCreation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class EntryCreationModule {
    @Binds
    abstract fun bindEntryCreation(implementation: CreateEntryUseCase): EntryCreation

    @Binds
    abstract fun bindAutofillEntryCreation(implementation: CreateEntryUseCase): AutofillEntryCreation
}
