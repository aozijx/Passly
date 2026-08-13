package com.aozijx.passly.data.di

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.repository.database.DatabaseControllerImpl
import com.aozijx.passly.runtime.session.DatabaseSessionLifecycle
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.data.database.port.DatabaseController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseBindingsModule {
    @Binds
    @Singleton
    abstract fun bindDatabaseController(
        impl: DatabaseControllerImpl
    ): DatabaseController

    @Binds
    @Singleton
    abstract fun bindSessionStateProvider(
        impl: AppDatabaseSession
    ): SessionStateProvider

    @Binds
    @Singleton
    abstract fun bindDatabaseSessionLifecycle(
        impl: AppDatabaseSession
    ): DatabaseSessionLifecycle
}
