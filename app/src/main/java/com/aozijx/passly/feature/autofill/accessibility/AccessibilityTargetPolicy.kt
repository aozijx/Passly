package com.aozijx.passly.feature.autofill.accessibility

internal object AccessibilityTargetPolicy {
    private const val MAX_AGE_MS = 30_000L

    fun canFill(
        expectedPackage: String,
        expectedWindowId: Int,
        capturedAtMs: Long,
        currentPackage: String,
        currentWindowId: Int,
        nowMs: Long,
    ): Boolean = expectedPackage == currentPackage &&
        expectedWindowId == currentWindowId &&
        nowMs - capturedAtMs in 0..MAX_AGE_MS
}
