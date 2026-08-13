package com.aozijx.passly.security.search

import java.text.Normalizer

object SearchNormalizer {

    fun normalize(text: String): String {
        var result = text
        result = removeProtocol(result)
        result = removeWww(result)
        result = Normalizer.normalize(result, Normalizer.Form.NFKC)
        result = result.lowercase()
        result = result.trim()
        result = result.replace(Regex("\\s+"), " ")
        return result
    }

    private fun removeProtocol(text: String): String {
        return text.replace(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://"), "")
            .replace(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:"), "")
    }

    private fun removeWww(text: String): String {
        return text.replace(Regex("^www\\."), "")
    }
}