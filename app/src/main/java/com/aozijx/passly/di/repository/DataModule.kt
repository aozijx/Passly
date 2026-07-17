package com.aozijx.passly.di.repository

import com.aozijx.passly.data.local.datastore.ProtoDataStoreBootstrapStore
import com.aozijx.passly.security.envelope.BootstrapStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBootstrapStore(impl: ProtoDataStoreBootstrapStore): BootstrapStore

}
