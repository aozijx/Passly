package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppNotice
import kotlinx.coroutines.flow.Flow

/**
 * 应用内消息流。
 * UI 层 collect 此流以获得需展示的 [AppNotice]。
 * 仅包含路由到 [NoticeTarget.IN_APP] 的消息。
 */
interface InAppNoticeStream {
    val notices: Flow<AppNotice>
}
