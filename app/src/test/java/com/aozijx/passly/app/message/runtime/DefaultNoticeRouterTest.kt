package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.contract.AppVisibility
import com.aozijx.passly.app.message.contract.NoticeRoutingContext
import com.aozijx.passly.app.message.contract.NoticeTarget
import com.aozijx.passly.app.message.contract.RouteReason
import com.aozijx.passly.app.message.contract.SystemNotificationState
import com.aozijx.passly.app.message.contract.defaultNoticeCodePolicy
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.NoticeTopic
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.TopicMessageSettings
import com.aozijx.passly.testing.sampleMessageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNoticeRouterTest {
    private val router = DefaultNoticeRouter()

    @Test
    fun optionalMessageHonorsMasterTopicAndMinimumLevel() {
        val notice = newAppNotice(NoticeCode.CLIPBOARD_CLEARED)
        val masterOff =
            context(
                settings = sampleMessageSettings(optionalMessagesEnabled = false),
            )
        assertEquals(
            RouteReason.MASTER_DISABLED,
            router.route(notice, defaultNoticeCodePolicy(notice.code), masterOff).reason,
        )

        val topicOff =
            context(
                settings =
                    sampleMessageSettings(
                        topicSettings =
                            mapOf(
                                MessageTopic.CLIPBOARD to
                                    TopicMessageSettings(
                                        enabled = false,
                                        minimumLevel = MessageLevel.INFO,
                                    ),
                            ),
                    ),
            )
        assertEquals(
            RouteReason.TOPIC_DISABLED,
            router.route(notice, defaultNoticeCodePolicy(notice.code), topicOff).reason,
        )

        val levelFiltered =
            context(
                settings =
                    sampleMessageSettings(
                        topicSettings =
                            mapOf(
                                MessageTopic.CLIPBOARD to
                                    TopicMessageSettings(
                                        enabled = true,
                                        minimumLevel = MessageLevel.ERROR,
                                    ),
                            ),
                    ),
            )
        assertEquals(
            RouteReason.BELOW_MINIMUM_LEVEL,
            router.route(notice, defaultNoticeCodePolicy(notice.code), levelFiltered).reason,
        )
    }

    @Test
    fun mandatoryMessageBypassesContentFiltersButNotSystemCapability() {
        val notice = newAppNotice(NoticeCode.SECURITY_ACTION_FAILED)
        val plan =
            router.route(
                notice,
                defaultNoticeCodePolicy(notice.code),
                context(
                    settings = sampleMessageSettings(optionalMessagesEnabled = false),
                    systemAvailable = false,
                ),
            )

        assertEquals(RouteReason.FALLBACK_TO_IN_APP, plan.reason)
        assertEquals(setOf(NoticeTarget.IN_APP), plan.targets)
    }

    @Test
    fun optionalMessageIsSuppressedWhenSettingsAreUnavailable() {
        val notice = newAppNotice(NoticeCode.BACKUP_EXPORT_COMPLETED)
        val plan =
            router.route(
                notice,
                defaultNoticeCodePolicy(notice.code),
                context(settings = null),
            )

        assertTrue(plan.targets.isEmpty())
        assertEquals(RouteReason.SETTINGS_UNAVAILABLE, plan.reason)
    }

    @Test
    fun preferSystemUsesSystemAndKeepsRuntimeFallback() {
        val notice = newAppNotice(NoticeCode.SECURITY_ACTION_FAILED)
        val plan =
            router.route(
                notice,
                defaultNoticeCodePolicy(notice.code),
                context(systemAvailable = true),
            )

        assertEquals(setOf(NoticeTarget.SYSTEM), plan.targets)
        assertEquals(NoticeTarget.IN_APP, plan.fallbackTarget)
    }

    @Test
    fun backgroundInAppPreferenceRoutesToSystem() {
        val notice = newAppNotice(NoticeCode.BACKUP_EXPORT_COMPLETED)
        val plan =
            router.route(
                notice,
                defaultNoticeCodePolicy(notice.code),
                context(
                    visibility = AppVisibility.BACKGROUND,
                    systemAvailable = true,
                ),
            )

        assertTrue(NoticeTarget.SYSTEM in plan.targets)
    }

    @Test
    fun backupDirectoryCheckStaysInApp() {
        val notice = newAppNotice(NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED)
        val plan =
            router.route(
                notice,
                defaultNoticeCodePolicy(notice.code),
                context(systemAvailable = true),
            )

        assertEquals(setOf(NoticeTarget.IN_APP), plan.targets)
        assertEquals(NoticeTopic.BACKUP, defaultNoticeCodePolicy(notice.code).topic)
    }

    private fun context(
        settings: MessageSettings? = sampleMessageSettings(),
        visibility: AppVisibility = AppVisibility.FOREGROUND,
        systemAvailable: Boolean = true,
    ) = NoticeRoutingContext(
        settings = settings,
        settingsVersion = 1,
        appVisibility = visibility,
        systemNotificationState =
            SystemNotificationState(
                userSettingEnabled = systemAvailable,
                runtimePermissionGranted = systemAvailable,
                notificationsEnabledBySystem = systemAvailable,
                channelEnabled = systemAvailable,
            ),
    )
}
