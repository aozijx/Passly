package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.repository.activity.RoomActivityQueryRepository
import com.aozijx.passly.data.repository.activity.RoomActivityRecorder
import com.aozijx.passly.data.repository.entry.RoomEntryCommandRepository
import com.aozijx.passly.data.repository.entry.RoomEntryListQueryRepository
import com.aozijx.passly.data.repository.entry.RoomEntryLinkRepository
import com.aozijx.passly.data.repository.entry.RoomEntryQueryRepository
import com.aozijx.passly.data.repository.entry.RoomSensitiveFieldRepository
import com.aozijx.passly.data.repository.entry.paging.EntryPagingStore
import com.aozijx.passly.data.repository.entry.paging.RoomEntryPagingStore
import com.aozijx.passly.data.repository.favicon.FaviconRepositoryImpl
import com.aozijx.passly.data.repository.otp.RoomOtpConfigRepository
import com.aozijx.passly.data.repository.search.BlindIndexMaintenance
import com.aozijx.passly.domain.entry.port.ActivityQueryRepository
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryLinkRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.FaviconRepository
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.port.SearchIndexMaintenance
import com.aozijx.passly.domain.entry.port.SensitiveFieldRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class EntryPersistenceModule {

    @Binds
    @Singleton
    abstract fun bindEntryQueryRepository(impl: RoomEntryQueryRepository): EntryQueryRepository

    @Binds
    @Singleton
    abstract fun bindEntryCommandRepository(impl: RoomEntryCommandRepository): EntryCommandRepository

    @Binds
    @Singleton
    abstract fun bindEntryLinkRepository(impl: RoomEntryLinkRepository): EntryLinkRepository

    @Binds
    @Singleton
    abstract fun bindSensitiveFieldRepository(
        impl: RoomSensitiveFieldRepository
    ): SensitiveFieldRepository

    @Binds
    @Singleton
    abstract fun bindEntryListQueryRepository(impl: RoomEntryListQueryRepository): EntryListQueryRepository

    @Binds
    @Singleton
    abstract fun bindEntryPagingStore(impl: RoomEntryPagingStore): EntryPagingStore

    @Binds
    @Singleton
    abstract fun bindActivityQueryRepository(impl: RoomActivityQueryRepository): ActivityQueryRepository

    @Binds
    @Singleton
    abstract fun bindActivityRecorder(impl: RoomActivityRecorder): ActivityRecorder

    @Binds
    @Singleton
    abstract fun bindOtpConfigRepository(impl: RoomOtpConfigRepository): OtpConfigRepository

    @Binds
    @Singleton
    abstract fun bindSearchIndexMaintenance(impl: BlindIndexMaintenance): SearchIndexMaintenance

    @Binds
    @Singleton
    abstract fun bindFaviconRepository(impl: FaviconRepositoryImpl): FaviconRepository

}
