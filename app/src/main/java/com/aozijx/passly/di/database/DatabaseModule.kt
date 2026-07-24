package com.aozijx.passly.di.database

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.repository.database.DatabaseControllerImpl
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.diagnostics.repository.DatabaseController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库相关绑定。
 *
 * AppDatabase / DatabaseProvider 通过 @Inject constructor 由 Hilt 自动提供。
 * DAO 通过 AppDatabase 获取，无需显式绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    internal abstract fun bindDatabaseController(
        impl: DatabaseControllerImpl
    ): DatabaseController

    @Binds
    @Singleton
    internal abstract fun bindSessionStateProvider(
        impl: UnifiedSessionManager
    ): SessionStateProvider

}
