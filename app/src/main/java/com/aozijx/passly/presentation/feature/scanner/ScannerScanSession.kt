package com.aozijx.passly.presentation.feature.scanner

internal class ScannerScanSession {
    private var lastAcceptedValue: String? = null

    fun accept(rawValue: String): Boolean {
        if (rawValue.isBlank() || rawValue == lastAcceptedValue) return false
        lastAcceptedValue = rawValue
        return true
    }

    fun reset() {
        lastAcceptedValue = null
    }
}
