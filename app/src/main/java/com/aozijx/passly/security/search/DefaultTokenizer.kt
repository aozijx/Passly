package com.aozijx.passly.security.search

import com.aozijx.passly.domain.model.lookup.LookupField
import java.text.Normalizer

class DefaultTokenizer : Tokenizer {

    private companion object {
        const val QUERY_MIN_LENGTH = 2
        const val USERNAME_MAX_GRAM = 6
        const val EMAIL_MAX_GRAM = 6
        const val DEFAULT_MIN_GRAM = 2
        val SEPARATORS = Regex("[,;:!?.@#\$%^&*()\\[\\]{}\"'~/\\\\|<>+=_\\-]")
        val WHITESPACE = Regex("\\s+")
    }

    override fun getConfig(field: LookupField): TokenizerConfig = when (field) {
        LookupField.TITLE -> TokenizerConfig(true, DEFAULT_MIN_GRAM, Int.MAX_VALUE)
        LookupField.DOMAIN -> TokenizerConfig(true, DEFAULT_MIN_GRAM, Int.MAX_VALUE)
        LookupField.USERNAME -> TokenizerConfig(true, DEFAULT_MIN_GRAM, USERNAME_MAX_GRAM)
        LookupField.EMAIL -> TokenizerConfig(true, DEFAULT_MIN_GRAM, EMAIL_MAX_GRAM)
        LookupField.URL -> TokenizerConfig(true, DEFAULT_MIN_GRAM, Int.MAX_VALUE)
        LookupField.PACKAGE -> TokenizerConfig(true, DEFAULT_MIN_GRAM, Int.MAX_VALUE)
    }

    override fun normalize(text: String): String {
        var result = text
        result = removeProtocol(result)
        result = removeWww(result)
        result = Normalizer.normalize(result, Normalizer.Form.NFKC)
        result = result.lowercase()
        result = result.trim()
        result = result.replace(Regex("\\s+"), " ")
        return result
    }

    override fun tokenize(text: String, field: LookupField): List<TokenGram> {
        val config = getConfig(field)
        if (!config.enabled) return emptyList()

        val normalized = normalize(text)
        if (normalized.isEmpty()) return emptyList()

        val grams = mutableSetOf<TokenGram>()

        val rawTokens = normalized
            .split(SEPARATORS)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { it.split(WHITESPACE) }
            .filter { it.isNotEmpty() }
            .toList()

        for (token in rawTokens) {
            grams.addAll(generateEdgeNgrams(token, config.minGram, config.maxGram))
        }

        return grams.toList()
    }

    override fun tokenizeQuery(query: String): List<TokenGram> {
        val normalized = normalize(query)
        if (normalized.length < QUERY_MIN_LENGTH) return emptyList()

        val grams = mutableSetOf<TokenGram>()

        val rawTokens = normalized
            .split(SEPARATORS)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { it.split(WHITESPACE) }
            .filter { it.length >= QUERY_MIN_LENGTH }
            .toList()

        for (token in rawTokens) {
            grams.add(TokenGram(token, token.length))
        }

        return grams.toList()
    }

    override fun generateEdgeNgrams(token: String, minLen: Int, maxLen: Int): List<TokenGram> {
        if (token.length < minLen) return emptyList()

        val effectiveMax = minOf(token.length, maxLen)
        val result = mutableListOf<TokenGram>()

        for (n in minLen..effectiveMax) {
            result.add(TokenGram(token.substring(0, n), n))
        }

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