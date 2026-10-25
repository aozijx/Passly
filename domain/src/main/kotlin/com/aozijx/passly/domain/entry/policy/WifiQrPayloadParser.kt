package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType

object WifiQrPayloadParser {
    private const val PREFIX = "WIFI:"
    private val uniqueKeys = setOf("S", "P", "T", "H")
    private val enterpriseKeys = setOf("E", "PH2", "A", "I")

    fun parse(rawValue: String): WifiQrParseResult {
        if (!rawValue.regionMatches(0, PREFIX, 0, PREFIX.length, ignoreCase = true)) {
            return WifiQrParseResult.NotWifi
        }
        val fields = when (val result = parseFields(rawValue.substring(PREFIX.length))) {
            is FieldParseResult.Parsed -> result.fields
            FieldParseResult.Malformed -> return WifiQrParseResult.Malformed
        }
        if (fields.keys.any(enterpriseKeys::contains)) return WifiQrParseResult.Unsupported

        val ssid = fields["S"]?.takeUnless(String::isBlank)
            ?: return WifiQrParseResult.Malformed
        val securityType = when (val result = parseSecurityType(fields["T"])) {
            is SecurityParseResult.Supported -> result.value
            SecurityParseResult.Unsupported -> return WifiQrParseResult.Unsupported
        }
        val isHidden = when (val result = parseHidden(fields["H"])) {
            is HiddenParseResult.Supported -> result.value
            HiddenParseResult.Malformed -> return WifiQrParseResult.Malformed
        }
        val password = if (securityType == WifiSecurityType.Open) {
            ""
        } else {
            fields["P"].orEmpty()
        }
        return WifiQrParseResult.Parsed(
            WifiQrPayload(
                ssid = ssid,
                password = password,
                securityType = securityType,
                isHidden = isHidden,
            ),
        )
    }

    private fun parseFields(body: String): FieldParseResult {
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
                        return FieldParseResult.Parsed(fields)
                    }
                    if (readingKey || key.isEmpty()) return FieldParseResult.Malformed

                    val normalizedKey = key.toString().uppercase()
                    if (normalizedKey in uniqueKeys && normalizedKey in fields) {
                        return FieldParseResult.Malformed
                    }
                    fields[normalizedKey] = value.toString()

                    if (nextIsTerminal) {
                        return if (index + 1 == body.lastIndex) {
                            FieldParseResult.Parsed(fields)
                        } else {
                            FieldParseResult.Malformed
                        }
                    }
                    key.clear()
                    value.clear()
                    readingKey = true
                }

                else -> target(readingKey, key, value).append(character)
            }
            index++
        }
        return FieldParseResult.Malformed
    }

    private fun target(
        readingKey: Boolean,
        key: StringBuilder,
        value: StringBuilder,
    ): StringBuilder = if (readingKey) key else value

    private fun parseSecurityType(rawValue: String?): SecurityParseResult =
        when (rawValue?.trim()?.uppercase()) {
            null, "NOPASS", "OPEN" -> SecurityParseResult.Supported(WifiSecurityType.Open)
            "WEP" -> SecurityParseResult.Supported(WifiSecurityType.Wep)
            "WPA", "WPA2", "WPA3", "SAE" ->
                SecurityParseResult.Supported(WifiSecurityType.WpaPersonal)
            else -> SecurityParseResult.Unsupported
        }

    private fun parseHidden(rawValue: String?): HiddenParseResult =
        when (rawValue?.trim()?.lowercase()) {
            null, "false" -> HiddenParseResult.Supported(false)
            "true" -> HiddenParseResult.Supported(true)
            else -> HiddenParseResult.Malformed
        }

    private sealed interface FieldParseResult {
        data class Parsed(val fields: Map<String, String>) : FieldParseResult
        data object Malformed : FieldParseResult
    }

    private sealed interface SecurityParseResult {
        data class Supported(val value: WifiSecurityType) : SecurityParseResult
        data object Unsupported : SecurityParseResult
    }

    private sealed interface HiddenParseResult {
        data class Supported(val value: Boolean) : HiddenParseResult
        data object Malformed : HiddenParseResult
    }
}
