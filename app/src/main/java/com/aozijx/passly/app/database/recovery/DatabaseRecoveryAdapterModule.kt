package com.aozijx.passly.app.database.recovery

import com.aozijx.passly.feature.database.recovery.DatabaseRecoveryGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseRecoveryAdapterModule {
    @Binds
    @Singleton
    abstract fun bindDatabaseRecoveryGateway(
        implementation: DataDatabaseRecoveryGateway,
    ): DatabaseRecoveryGateway
}
