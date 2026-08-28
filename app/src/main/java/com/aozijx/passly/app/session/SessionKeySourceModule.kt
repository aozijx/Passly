package com.aozijx.passly.app.session

import com.aozijx.passly.runtime.session.SessionKeySource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionKeySourceModule {
    @Binds
    @Singleton
    abstract fun bindSessionKeySource(
        implementation: DekSessionKeySource,
    ): SessionKeySource
}
