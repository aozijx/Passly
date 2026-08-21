package com.aozijx.passly.app.entry.service

import com.aozijx.passly.domain.entry.port.FaviconRepository
import com.aozijx.passly.domain.entry.service.FaviconService
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EntryServiceModule {

    @Provides
    @Singleton
    fun provideFaviconService(faviconRepository: FaviconRepository): FaviconService {
        return FaviconService(faviconRepository)
    }
}
