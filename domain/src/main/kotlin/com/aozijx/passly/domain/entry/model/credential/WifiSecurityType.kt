package com.aozijx.passly.domain.entry.model.credential

enum class WifiSecurityType(
    val storedValue: String,
) {
    Open("nopass"),
    Wep("WEP"),
    WpaPersonal("WPA"),
    ;

    companion object {
        fun parseStoredValue(value: String?): WifiSecurityTypeParseResult {
            val normalized = value?.trim().orEmpty()
            if (normalized.isEmpty()) return WifiSecurityTypeParseResult.Missing

            return when (normalized.uppercase()) {
                "NOPASS", "OPEN" -> WifiSecurityTypeParseResult.Supported(Open)
                "WEP" -> WifiSecurityTypeParseResult.Supported(Wep)
                "WPA", "WPA2", "WPA3", "SAE" ->
                    WifiSecurityTypeParseResult.Supported(WpaPersonal)
                else -> WifiSecurityTypeParseResult.Unsupported(normalized)
            }
        }
    }
}

sealed interface WifiSecurityTypeParseResult {
    data class Supported(val value: WifiSecurityType) : WifiSecurityTypeParseResult

    data object Missing : WifiSecurityTypeParseResult

    data class Unsupported(val storedValue: String) : WifiSecurityTypeParseResult
}
