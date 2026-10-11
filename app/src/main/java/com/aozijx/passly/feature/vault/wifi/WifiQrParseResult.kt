package com.aozijx.passly.feature.vault.wifi

sealed interface WifiQrParseResult {
    data class Parsed(val payload: WifiQrPayload) : WifiQrParseResult

    data object NotWifi : WifiQrParseResult

    data object Malformed : WifiQrParseResult

    data object Unsupported : WifiQrParseResult
}
