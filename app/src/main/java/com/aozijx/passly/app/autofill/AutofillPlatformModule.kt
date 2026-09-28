package com.aozijx.passly.app.autofill

import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AutofillPlatformModule {
    @Binds
    @Singleton
    abstract fun bindAutofillLaunchTarget(
        impl: AndroidAutofillLaunchTarget,
    ): AutofillLaunchTarget

    @Binds
    @Singleton
    abstract fun bindAutofillServiceStatusSource(
        impl: AndroidAutofillServiceStatusSource,
    ): AutofillServiceStatusSource
}
