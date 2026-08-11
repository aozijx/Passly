package com.aozijx.passly.domain.entry.model

enum class PasswordStrengthLevel {
    VERY_WEAK,
    WEAK,
    MEDIUM,
    GOOD,
    STRONG
}

data class PasswordStrengthResult(
    val score: Int,
    val level: PasswordStrengthLevel,
    val crackTimeSeconds: Double = 0.0
)
