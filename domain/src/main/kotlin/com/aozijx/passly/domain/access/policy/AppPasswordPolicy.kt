package com.aozijx.passly.domain.access.policy

data class AppPasswordPolicy(
    val minimumLength: Int = 6,
    val minimumCharacterGroups: Int = 2,
) {
    init {
        require(minimumLength > 0) { "minimumLength must be positive" }
        require(minimumCharacterGroups in 1..3) {
            "minimumCharacterGroups must be between 1 and 3"
        }
    }

    fun validate(password: CharArray): Set<AppPasswordViolation> = buildSet {
        if (password.size < minimumLength) add(AppPasswordViolation.TOO_SHORT)

        val groups = listOf(
            password.any(Char::isLetter),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
        ).count { it }

        if (groups < minimumCharacterGroups) {
            add(AppPasswordViolation.INSUFFICIENT_CHARACTER_VARIETY)
        }
    }

    fun acceptsLength(length: Int): Boolean = length >= minimumLength

    companion object {
        val DEFAULT = AppPasswordPolicy()
    }
}

enum class AppPasswordViolation {
    TOO_SHORT,
    INSUFFICIENT_CHARACTER_VARIETY,
}
