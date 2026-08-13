package com.aozijx.passly.app.di

import com.aozijx.passly.app.message.presentation.AndroidNoticeTextResolver
import com.aozijx.passly.app.message.runtime.ProcessAppVisibilityProvider
import com.aozijx.passly.app.message.system.AndroidSystemNotificationGateway
import com.aozijx.passly.app.message.runtime.DefaultAppNoticeDispatcher
import com.aozijx.passly.app.message.runtime.DefaultInAppNoticeSink
import com.aozijx.passly.app.message.runtime.DefaultMessageSettingsSnapshotProvider
import com.aozijx.passly.app.message.runtime.DefaultNoticeDeduplicator
import com.aozijx.passly.app.message.runtime.DefaultNoticeRouter
import com.aozijx.passly.domain.notice.port.AppNoticeDispatcher
import com.aozijx.passly.domain.notice.port.AppNoticePublisher
import com.aozijx.passly.domain.notice.port.AppVisibilityProvider
import com.aozijx.passly.domain.notice.port.InAppNoticeStream
import com.aozijx.passly.domain.notice.port.MessageSettingsSnapshotProvider
import com.aozijx.passly.domain.notice.port.NoticeCodeRegistry
import com.aozijx.passly.domain.notice.port.NoticeDeduplicator
import com.aozijx.passly.domain.notice.port.NoticeRouter
import com.aozijx.passly.domain.notice.port.NoticeSink
import com.aozijx.passly.domain.notice.port.NoticeTextResolver
import com.aozijx.passly.domain.notice.port.SystemNotificationGateway
import com.aozijx.passly.domain.notice.port.SystemNotificationStateProvider
import com.aozijx.passly.domain.notice.port.defaultNoticeCodePolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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
    abstract fun bindNoticeRouter(impl: DefaultNoticeRouter): NoticeRouter

    @Binds
    @Singleton
    abstract fun bindDispatcher(impl: DefaultAppNoticeDispatcher): AppNoticeDispatcher

    @Binds
    @Singleton
    abstract fun bindPublisher(impl: DefaultAppNoticeDispatcher): AppNoticePublisher

    @Binds
    @Singleton
    abstract fun bindInAppStream(impl: DefaultInAppNoticeSink): InAppNoticeStream

    @Binds
    @IntoSet
    abstract fun bindInAppSink(impl: DefaultInAppNoticeSink): NoticeSink

    @Binds
    @Singleton
    abstract fun bindSystemGateway(
        impl: AndroidSystemNotificationGateway
    ): SystemNotificationGateway

    @Binds
    @IntoSet
    abstract fun bindSystemSink(impl: AndroidSystemNotificationGateway): NoticeSink

    @Binds
    @Singleton
    abstract fun bindSystemStateProvider(
        impl: AndroidSystemNotificationGateway
    ): SystemNotificationStateProvider

    @Binds
    @Singleton
    abstract fun bindSettingsProvider(
        impl: DefaultMessageSettingsSnapshotProvider
    ): MessageSettingsSnapshotProvider

    @Binds
    @Singleton
    abstract fun bindVisibilityProvider(
        impl: ProcessAppVisibilityProvider
    ): AppVisibilityProvider

    @Binds
    @Singleton
    abstract fun bindTextResolver(impl: AndroidNoticeTextResolver): NoticeTextResolver

    companion object {
        @Provides
        @Singleton
        fun provideNoticeCodeRegistry(): NoticeCodeRegistry =
            NoticeCodeRegistry(::defaultNoticeCodePolicy)
    }
}
