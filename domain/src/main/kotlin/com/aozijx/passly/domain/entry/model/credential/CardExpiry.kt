package com.aozijx.passly.domain.entry.model.credential

data class CardExpiry(val month: Int, val year: Int) {
    init {
        require(isValid(month, year)) { "Card expiry month or year is invalid" }
    }

    val storageValue: String
        get() = "${month.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"

    companion object {
        private val STORAGE_PATTERN = Regex("([0-9]{2})/([0-9]{4})")

        fun parse(value: String): CardExpiryParseResult {
            val match = STORAGE_PATTERN.matchEntire(value) ?: return CardExpiryParseResult.Invalid
            val month = match.groupValues[1].toInt()
            val year = match.groupValues[2].toInt()
            return if (isValid(month, year)) CardExpiryParseResult.Parsed(CardExpiry(month, year))
            else CardExpiryParseResult.Invalid
        }

        private fun isValid(month: Int, year: Int): Boolean = month in 1..12 && year in 1..9999
    }
}

sealed interface CardExpiryParseResult {
    data class Parsed(val value: CardExpiry) : CardExpiryParseResult
    data object Invalid : CardExpiryParseResult
}
