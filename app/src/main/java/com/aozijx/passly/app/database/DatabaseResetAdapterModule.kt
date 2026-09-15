package com.aozijx.passly.app.database

import com.aozijx.passly.feature.database.reset.DatabaseResetGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseResetAdapterModule {
    @Binds
    @Singleton
    abstract fun bindDatabaseResetGateway(
        implementation: DatabaseResetGatewayAdapter,
    ): DatabaseResetGateway
}
