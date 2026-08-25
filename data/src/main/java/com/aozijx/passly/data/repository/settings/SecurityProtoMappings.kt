package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.local.datastore.settings.SecurityPreferences
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.SecuritySettings

internal fun readSecurity(p: SecurityPreferences): SecuritySettings =
    SecuritySettings(
        isSecureContentEnabled = p.secureContentEnabled,
        isFlipToLockEnabled = p.flipToLockEnabled,
        isFlipExitAndClearStackEnabled = p.flipExitAndClearStack,
        isLockOnBackground = p.lockOnBackground,
        lockTimeout = p.lockTimeoutMs,
        isInvalidateBiometricKeyOnChange = p.invalidateBiometricKeyOnChange,
        reauthenticateSensitiveCopies = p.reauthenticateSensitiveCopies,
        clipboardClearPolicy = ClipboardClearPolicy(
            enabled = p.clipboardClearEnabled,
            delaySeconds = ClipboardClearPolicy.normalizeDelaySeconds(
                p.clipboardClearDelaySeconds
            ),
        ),
    )
