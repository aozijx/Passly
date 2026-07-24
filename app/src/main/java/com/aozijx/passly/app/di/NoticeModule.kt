package com.aozijx.passly.app.di

import com.aozijx.passly.data.notice.DefaultAppNoticeDispatcher
import com.aozijx.passly.data.notice.DefaultNoticeDeduplicator
import com.aozijx.passly.data.notice.DefaultNoticeRouter
import com.aozijx.passly.data.notice.StubSystemNotificationGateway
import com.aozijx.passly.domain.notice.port.AppNoticeDispatcher
import com.aozijx.passly.domain.notice.port.AppNoticePublisher
import com.aozijx.passly.domain.notice.port.InAppNoticeStream
import com.aozijx.passly.domain.notice.port.NoticeCodeRegistry
import com.aozijx.passly.domain.notice.port.NoticeDeduplicator
import com.aozijx.passly.domain.notice.port.NoticeRouter
import com.aozijx.passly.domain.notice.port.SystemNotificationGateway
import com.aozijx.passly.domain.notice.port.defaultNoticeCodePolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NoticeModule {

    @Binds
    @Singleton
    abstract fun bindNoticeDeduplicator(
        impl: DefaultNoticeDeduplicator
    ): NoticeDeduplicator

    @Binds
    @Singleton
    abstract fun bindNoticeRouter(
        impl: DefaultNoticeRouter
    ): NoticeRouter

    @Binds
    @Singleton
    abstract fun bindAppNoticeDispatcher(
        impl: DefaultAppNoticeDispatcher
    ): AppNoticeDispatcher

    @Binds
    @Singleton
    abstract fun bindAppNoticePublisher(
        impl: DefaultAppNoticeDispatcher
    ): AppNoticePublisher

    @Binds
    @Singleton
    abstract fun bindInAppNoticeStream(
        impl: DefaultAppNoticeDispatcher
    ): InAppNoticeStream

    @Binds
    @Singleton
    abstract fun bindSystemNotificationGateway(
        impl: StubSystemNotificationGateway
    ): SystemNotificationGateway

    companion object {
        @Provides
        @Singleton
        fun provideNoticeCodeRegistry(): NoticeCodeRegistry = NoticeCodeRegistry { code ->
            defaultNoticeCodePolicy(code)
        }
    }
}
