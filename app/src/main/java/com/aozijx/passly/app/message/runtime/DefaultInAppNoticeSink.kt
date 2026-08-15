package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.contract.InAppNoticeStream
import com.aozijx.passly.app.message.contract.NoticeSink
import com.aozijx.passly.app.message.contract.NoticeTarget
import com.aozijx.passly.app.message.contract.SinkResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultInAppNoticeSink @Inject constructor() : NoticeSink, InAppNoticeStream {
    private val channel = Channel<AppNotice>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val target: NoticeTarget = NoticeTarget.IN_APP
    override val notices: Flow<AppNotice> = channel.receiveAsFlow()

    override suspend fun deliver(notice: AppNotice): SinkResult {
        channel.send(notice)
        return SinkResult.Delivered
    }
}
