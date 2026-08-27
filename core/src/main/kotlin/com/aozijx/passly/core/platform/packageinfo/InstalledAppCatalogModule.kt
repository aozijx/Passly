package com.aozijx.passly.core.platform.packageinfo

import com.aozijx.passly.domain.autofill.port.ApplicationLabelResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class InstalledAppCatalogModule {
    @Binds
    abstract fun bindApplicationLabelResolver(
        implementation: InstalledAppCatalog,
    ): ApplicationLabelResolver
}
