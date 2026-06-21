package com.aozijx.passly.domain

import com.aozijx.passly.domain.model.AutofillUiMode
import com.aozijx.passly.domain.model.SwipeActionType

object AppDefaults {
    object Lock {
        const val MIN_TIMEOUT_MS: Long = 10_000L
        const val MAX_TIMEOUT_MS: Long = 300_000L
        const val SLIDER_STEP_MS: Long = 5_000L
        const val SLIDER_MIN_TIMEOUT_MS: Long = 15_000L
        const val DEFAULT_TIMEOUT_MS: Long = 60_000L
        const val MIN_APP_PASSWORD_LOCKOUT_MS: Long = 30_000L
        const val APP_PASSWORD_MAX_FAILED_ATTEMPTS: Int = 5
    }

    object Display {
        const val TAB_THRESHOLD_MIN: Int = 2
        const val TAB_THRESHOLD_MAX: Int = 8
        const val STATUS_BAR_AUTO_HIDE: Boolean = true
        const val TOP_BAR_COLLAPSIBLE: Boolean = true
        const val TAB_BAR_COLLAPSIBLE: Boolean = true
        const val AUTO_DOWNLOAD_ICONS: Boolean = true
        const val DYNAMIC_COLOR: Boolean = true
    }

    object Vault {
        const val SWIPE_ENABLED: Boolean = true
        val SWIPE_LEFT_ACTION: SwipeActionType = SwipeActionType.COPY_PASSWORD
        val SWIPE_RIGHT_ACTION: SwipeActionType = SwipeActionType.DETAIL
        val AUTOFILL_UI_MODE: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE
        const val TAB_BAR_MAX_TABS_WITHOUT_SCROLL: Int = 4
    }

    object Security {
        const val BIOMETRIC_ENABLED: Boolean = true
        const val INVALIDATE_KEY_ON_BIO_CHANGE: Boolean = true
        const val SECURE_CONTENT_ENABLED: Boolean = true
        const val FLIP_TO_LOCK_ENABLED: Boolean = false
        const val FLIP_EXIT_AND_CLEAR_STACK: Boolean = false
        const val LOCK_ON_BACKGROUND: Boolean = true
    }

    object Auth {
        const val MIN_PASSWORD_LENGTH: Int = 6
        const val PREFS_NAME: String = "secure_db_prefs"
        const val KEY_APP_PASSWORD_WRAP: String = "db_phrase_app_wrap"
        const val KEY_APP_PASSWORD_SALT: String = "db_phrase_app_salt"
        const val KEY_APP_PASSWORD_FAILED_COUNT: String = "db_phrase_app_failed_count"
        const val KEY_APP_PASSWORD_LOCKED_UNTIL: String = "db_phrase_app_locked_until"
        const val ERROR_APP_PASSWORD_MISMATCH: String = "应用密码错误"
        const val PASSPHRASE_IV_LENGTH: Int = 12
        const val PASSPHRASE_GCM_TAG_BITS: Int = 128
    }
}