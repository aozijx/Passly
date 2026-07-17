package com.aozijx.passly.security.search

import com.aozijx.passly.domain.model.lookup.LookupField
import com.aozijx.passly.domain.model.lookup.LookupFieldValue
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BlindIndexer(
    private val tokenizer: Tokenizer,
    private val searchKeyProvider: SearchKeyProvider
) {

    suspend fun index(
        entryId: String,
        fields: List<LookupFieldValue>
    ): List<BlindIndexRecord> {
        val key = searchKeyProvider.get()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))

        return fields.flatMap { (field, text) ->
            tokenizer.tokenize(text, field).map { tokenGram ->
                val fieldWeight = defaultWeight(field)
                val gramWeight = tokenGram.length
                BlindIndexRecord(
                    entryId = entryId,
                    field = field,
                    keywordHash = mac.doFinal(tokenGram.gram.toByteArray(Charsets.UTF_8)),
                    gramLength = tokenGram.length,
                    weight = fieldWeight * gramWeight
                )
            }
        }
    }

    suspend fun searchHashes(query: String): List<ByteArray> {
        val key = searchKeyProvider.get()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))

        return tokenizer.tokenizeQuery(query).map { tokenGram ->
            mac.doFinal(tokenGram.gram.toByteArray(Charsets.UTF_8))
        }
    }

    companion object {
        private const val HMAC_ALGORITHM = "HmacSHA256"

        fun defaultWeight(field: LookupField): Int = when (field) {
            LookupField.TITLE -> 10
            LookupField.USERNAME -> 8
            LookupField.EMAIL -> 8
            LookupField.DOMAIN -> 7
            LookupField.URL -> 6
            LookupField.PACKAGE -> 6
        }
    }
}

data class BlindIndexRecord(
    val entryId: String,
    val field: LookupField,
    val keywordHash: ByteArray,
    val gramLength: Int,
    val weight: Int
)
