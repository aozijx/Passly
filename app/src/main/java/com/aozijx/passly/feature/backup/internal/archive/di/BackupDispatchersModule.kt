package com.aozijx.passly.feature.backup.internal.archive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class BackupIoDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object BackupDispatchersModule {
    @Provides
    @BackupIoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
