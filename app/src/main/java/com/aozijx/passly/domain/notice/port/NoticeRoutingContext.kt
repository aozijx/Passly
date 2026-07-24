package com.aozijx.passly.domain.notice.port

import com.aozijx.passly.domain.notice.model.AppMessageSettings

/**
 * 路由时一次性读取的设置快照。
 * 保证同一条 [AppNotice] 在整个路由判断中使用一致的上下文。
 */
data class NoticeRoutingContext(
    val settings: AppMessageSettings,
    /** 应用是否在前台 */
    val isForeground: Boolean
)
