package com.aozijx.passly.data.di

import com.aozijx.passly.data.local.datastore.ProtoDataStoreBootstrapStore
import com.aozijx.passly.security.envelope.BootstrapStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BootstrapStoreModule {

    @Binds
    @Singleton
    abstract fun bindBootstrapStore(impl: ProtoDataStoreBootstrapStore): BootstrapStore

}
