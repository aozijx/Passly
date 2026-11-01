package com.aozijx.passly.app.entry.creation

import com.aozijx.passly.domain.entry.port.EntryCreation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class EntryCreationModule {
    @Binds
    abstract fun bindEntryCreation(implementation: CreateEntryUseCase): EntryCreation
}
