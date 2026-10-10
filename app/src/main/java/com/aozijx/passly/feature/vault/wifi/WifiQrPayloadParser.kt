package com.aozijx.passly.feature.vault.wifi

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType

object WifiQrPayloadParser {
    private const val PREFIX = "WIFI:"
    private val uniqueKeys = setOf("S", "P", "T", "H")
    private val enterpriseKeys = setOf("E", "PH2", "A", "I")

    fun parse(rawValue: String): WifiQrPayload? {
        if (!rawValue.regionMatches(0, PREFIX, 0, PREFIX.length, ignoreCase = true)) {
            return null
        }
        val fields = parseFields(rawValue.substring(PREFIX.length)) ?: return null
        if (fields.keys.any(enterpriseKeys::contains)) return null

        val ssid = fields["S"]?.takeUnless(String::isBlank) ?: return null
        val securityType = parseSecurityType(fields["T"]) ?: return null
        val isHidden = parseHidden(fields["H"]) ?: return null
        val password = if (securityType == WifiSecurityType.Open) {
            ""
        } else {
            fields["P"].orEmpty()
        }
        return WifiQrPayload(
            ssid = ssid,
            password = password,
            securityType = securityType,
            isHidden = isHidden,
        )
    }

    private fun parseFields(body: String): Map<String, String>? {
        val fields = linkedMapOf<String, String>()
        val key = StringBuilder()
        val value = StringBuilder()
        var readingKey = true
        var escaped = false
        var index = 0

        while (index < body.length) {
            val character = body[index]
            when {
                escaped -> {
                    target(readingKey, key, value).append(character)
                    escaped = false
                }

                character == '\\' -> escaped = true
                character == ':' && readingKey -> readingKey = false
                character == ';' -> {
                    val nextIsTerminal = index + 1 < body.length && body[index + 1] == ';'
                    if (readingKey && key.isEmpty() && nextIsTerminal && index + 1 == body.lastIndex) {
                        return fields
                    }
                    if (readingKey || key.isEmpty()) return null

                    val normalizedKey = key.toString().uppercase()
                    if (normalizedKey in uniqueKeys && normalizedKey in fields) return null
                    fields[normalizedKey] = value.toString()

                    if (nextIsTerminal) {
                        return fields.takeIf { index + 1 == body.lastIndex }
                    }
                    key.clear()
                    value.clear()
                    readingKey = true
                }

                else -> target(readingKey, key, value).append(character)
            }
            index++
        }
        return null
    }

    private fun target(
        readingKey: Boolean,
        key: StringBuilder,
        value: StringBuilder,
    ): StringBuilder = if (readingKey) key else value

    private fun parseSecurityType(rawValue: String?): WifiSecurityType? =
        when (rawValue?.trim()?.uppercase()) {
            null, "NOPASS", "OPEN" -> WifiSecurityType.Open
            "WEP" -> WifiSecurityType.Wep
            "WPA", "WPA2", "WPA3", "SAE" -> WifiSecurityType.WpaPersonal
            else -> null
        }

    private fun parseHidden(rawValue: String?): Boolean? =
        when (rawValue?.trim()?.lowercase()) {
            null, "false" -> false
            "true" -> true
            else -> null
        }
}
