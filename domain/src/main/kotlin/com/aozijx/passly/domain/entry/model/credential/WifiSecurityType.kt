package com.aozijx.passly.domain.entry.model.credential

enum class WifiSecurityType(
    val storedValue: String,
) {
    Open("nopass"),
    Wep("WEP"),
    WpaPersonal("WPA"),
    ;

    companion object {
        fun fromStoredValue(value: String?): WifiSecurityType =
            when (value?.trim()?.uppercase()) {
                "NOPASS", "OPEN" -> Open
                "WEP" -> Wep
                else -> WpaPersonal
            }
    }
}
