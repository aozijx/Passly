package com.aozijx.passly.domain

import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.SwipeActionType

object AppDefaults {
    // 锁定超时
    const val MIN_LOCK_TIMEOUT_MS = 10_000L
    const val MAX_LOCK_TIMEOUT_MS = 300_000L
    const val LOCK_TIMEOUT_SLIDER_STEP_MS = 5_000L
    const val SLIDER_MIN_LOCK_TIMEOUT_MS = 15_000L

    // 标签页阈值
    const val TAB_THRESHOLD_MIN = 2
    const val TAB_THRESHOLD_MAX = 8

    // 界面显示默认值
    const val DISPLAY_STATUS_BAR_AUTO_HIDE = true
    const val DISPLAY_TOP_BAR_COLLAPSIBLE = true
    const val DISPLAY_TAB_BAR_COLLAPSIBLE = true
    const val DISPLAY_AUTO_DOWNLOAD_ICONS = true
    const val DISPLAY_DYNAMIC_COLOR = true

    // 保险箱交互默认值
    const val VAULT_SWIPE_ENABLED = true
    val VAULT_SWIPE_LEFT_ACTION: SwipeActionType = SwipeActionType.COPY_PASSWORD
    val VAULT_SWIPE_RIGHT_ACTION: SwipeActionType = SwipeActionType.DETAIL
    val VAULT_AUTOFILL_UI_MODE: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE
    const val VAULT_TAB_BAR_MAX_TABS_WITHOUT_SCROLL = 4

    // 安全设置默认值
    const val SECURITY_BIOMETRIC_ENABLED = true
    const val SECURITY_PASSWORD_PREFERRED_AUTH_FIRST = true
    const val SECURITY_DEVICE_CREDENTIAL_FALLBACK_ENABLED = true
    const val SECURITY_INVALIDATE_KEY_ON_BIO_CHANGE = true
    const val SECURITY_SECURE_CONTENT_ENABLED = true
    const val SECURITY_FLIP_TO_LOCK_ENABLED = false
    const val SECURITY_FLIP_EXIT_AND_CLEAR_STACK_ENABLED = false
}