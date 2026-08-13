package com.aozijx.passly.security.search

import com.aozijx.passly.domain.entry.model.lookup.LookupField

data class TokenGram(val gram: String, val length: Int)

data class TokenizerConfig(
    val enabled: Boolean,
    val minGram: Int,
    val maxGram: Int
) {
    companion object {
        val DISABLED = TokenizerConfig(false, 0, 0)
    }
}

interface Tokenizer {

    fun getConfig(field: LookupField): TokenizerConfig

    fun normalize(text: String): String

    fun tokenize(text: String, field: LookupField): List<TokenGram>

    fun tokenizeQuery(query: String): List<TokenGram>

    fun generateEdgeNgrams(token: String, minLen: Int = 2, maxLen: Int = Int.MAX_VALUE): List<TokenGram>
}