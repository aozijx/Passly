package com.aozijx.passly.app.entry.di

import com.aozijx.passly.domain.entry.port.RelatedPasswordOperations
import com.aozijx.passly.domain.entry.port.EntryDetailQuery
import com.aozijx.passly.feature.vault.detail.EntryDetailQueryImpl
import com.aozijx.passly.feature.vault.detail.RelatedPasswordOperationsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class VaultDetailDomainBindingsModule {
    @Binds
    abstract fun bindEntryDetailQuery(implementation: EntryDetailQueryImpl): EntryDetailQuery

    @Binds
    abstract fun bindRelatedPasswordOperations(implementation: RelatedPasswordOperationsImpl): RelatedPasswordOperations
}
