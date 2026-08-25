package com.aozijx.passly.core.platform.packageinfo

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface InstalledAppServicesProvider {
    fun getInstalledAppCatalog(): InstalledAppCatalog
    fun getInstalledAppIconLoader(): InstalledAppIconLoader
}
