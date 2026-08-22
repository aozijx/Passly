package com.aozijx.passly.core.permission.di

import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.internal.AndroidPermissionStatusReader
import com.aozijx.passly.core.permission.internal.DefaultPermissionRequestArbiter
import com.aozijx.passly.core.permission.internal.SharedPreferencesPermissionRequestHistory
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
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
    abstract fun bindStatusReader(impl: AndroidPermissionStatusReader): PermissionStatusReader

    @Binds
    @Singleton
    abstract fun bindRequestArbiter(impl: DefaultPermissionRequestArbiter): PermissionRequestArbiter

    @Binds
    @Singleton
    abstract fun bindRequestHistory(
        impl: SharedPreferencesPermissionRequestHistory
    ): PermissionRequestHistory
}
