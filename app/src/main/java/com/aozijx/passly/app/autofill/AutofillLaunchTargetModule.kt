package com.aozijx.passly.app.autofill

import com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AutofillLaunchTargetModule {
    @Binds
    @Singleton
    abstract fun bindAutofillLaunchTarget(
        impl: AndroidAutofillLaunchTarget,
    ): AutofillLaunchTarget
}
