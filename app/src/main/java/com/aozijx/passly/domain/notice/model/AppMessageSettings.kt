package com.aozijx.passly.domain.notice.model

data class AppMessageSettings(
    val optionalMessagesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val topicSettings: Map<NoticeTopic, TopicMessageSettings> = defaultTopicSettings()
) {
    fun allows(notice: AppNotice): Boolean {
        // required 或 CRITICAL 级别不受总开关影响
        if (notice.required || notice.level == NoticeLevel.CRITICAL) return true

        // 总开关关闭则阻止所有可选提醒
        if (!optionalMessagesEnabled) return false

        // 分类开关
        val topicSetting = topicSettings[notice.topic] ?: TopicMessageSettings()
        if (!topicSetting.enabled) return false

        // 低于最低级别不投递
        return notice.level.level >= topicSetting.minimumLevel.level
    }

    fun allowsSystemNotification(): Boolean = systemNotificationsEnabled
}

data class TopicMessageSettings(
    val enabled: Boolean = true,
    val minimumLevel: NoticeLevel = NoticeLevel.INFO
)

fun defaultTopicSettings(): Map<NoticeTopic, TopicMessageSettings> = mapOf(
    // 一般提醒：剪贴板清除、应用关闭提醒 — 可以完全关闭
    NoticeTopic.CLIPBOARD to TopicMessageSettings(),
    NoticeTopic.APP_LIFECYCLE to TopicMessageSettings(),
    // 后台任务：图标下载、备份完成 — 可以完全关闭
    NoticeTopic.ICON_DOWNLOAD to TopicMessageSettings(),
    NoticeTopic.BACKUP to TopicMessageSettings(),
    // 安全：Warning 可选，Critical 不可关闭
    NoticeTopic.SECURITY to TopicMessageSettings(minimumLevel = NoticeLevel.WARNING),
    // 数据：不应静默
    NoticeTopic.DATABASE to TopicMessageSettings(minimumLevel = NoticeLevel.ERROR)
)

private val NoticeLevel.level: Int get() = ordinal // INFO=0, SUCCESS=1, WARNING=2, ERROR=3, CRITICAL=4
