package com.aozijx.passly.app.di

import com.aozijx.passly.core.permission.AndroidAppPermissionManager
import com.aozijx.passly.core.permission.AppPermissionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    @Singleton
    abstract fun bindPermissionManager(
        implementation: AndroidAppPermissionManager
    ): AppPermissionManager
}
