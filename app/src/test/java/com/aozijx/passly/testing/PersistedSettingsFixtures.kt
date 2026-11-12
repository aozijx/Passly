package com.aozijx.passly.testing

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.CardDensity
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.domain.settings.model.TopicMessageSettings

internal fun sampleAppearanceSettings() =
    AppearanceSettings(
        themeMode = ThemeMode.SYSTEM,
        isDynamicColor = true,
        themeKey = "default",
        language = AppLanguage.SYSTEM,
        fontFamily = FontFamilyMode.APP_BUNDLED,
    )

internal fun sampleInteractionSettings() =
    InteractionSettings(
        isSwipeEnabled = false,
        isPullToSearchEnabled = false,
        swipeLeftAction = SwipeActionType.COPY_PASSWORD,
        swipeRightAction = SwipeActionType.DELETE,
    )

internal fun sampleSecuritySettings() =
    SecuritySettings(
        isSecureContentEnabled = true,
        isFlipToLockEnabled = true,
        isFlipExitAndClearStackEnabled = false,
        isLockOnBackground = false,
        lockTimeout = 60_000L,
        reauthenticateSensitiveCopies = true,
        clipboardClearPolicy = ClipboardClearPolicy(enabled = true, delaySeconds = 30),
    )

internal fun sampleMessageSettings(
    optionalMessagesEnabled: Boolean = true,
    systemNotificationsEnabled: Boolean = true,
    topicSettings: Map<MessageTopic, TopicMessageSettings> = emptyMap(),
) = MessageSettings(
    optionalMessagesEnabled = optionalMessagesEnabled,
    systemNotificationsEnabled = systemNotificationsEnabled,
    topicSettings = completeMessageTopicSettings() + topicSettings,
)

internal fun completeMessageTopicSettings() =
    mapOf(
        MessageTopic.CLIPBOARD to TopicMessageSettings(true, MessageLevel.INFO),
        MessageTopic.APP_LIFECYCLE to TopicMessageSettings(true, MessageLevel.INFO),
        MessageTopic.BACKUP to TopicMessageSettings(true, MessageLevel.INFO),
        MessageTopic.SECURITY to TopicMessageSettings(true, MessageLevel.WARNING),
        MessageTopic.DATABASE to TopicMessageSettings(true, MessageLevel.ERROR),
    )

internal fun sampleCardPresentation(
    entryTypeKey: String,
    variantKey: String,
) = EntryCardPresentation(
    entryTypeKey = entryTypeKey,
    variantKey = variantKey,
    density = CardDensity.STANDARD,
    showIcon = true,
    showFavorite = true,
    showSecondaryText = true,
    showQuickAction = true,
)
