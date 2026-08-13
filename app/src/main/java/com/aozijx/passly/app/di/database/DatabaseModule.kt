package com.aozijx.passly.app.di.database

import com.aozijx.passly.core.session.DekSessionKeySource
import com.aozijx.passly.runtime.session.SessionKeySource
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
    internal abstract fun bindSessionKeySource(
        impl: DekSessionKeySource
    ): SessionKeySource

}
