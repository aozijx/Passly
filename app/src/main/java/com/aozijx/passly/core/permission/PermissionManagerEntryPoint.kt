package com.aozijx.passly.core.permission

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PermissionManagerEntryPoint {
    val appPermissionManager: AppPermissionManager
}