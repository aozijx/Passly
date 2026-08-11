package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import kotlin.math.ln
import kotlin.math.roundToInt

object PasswordStrengthEvaluator {

    private val WEAK_PATTERNS = setOf(
        "123456", "password", "123456789", "12345", "12345678",
        "qwerty", "abc123", "111111", "123123", "admin",
        "letmein", "welcome", "monkey", "dragon", "master"
    )

    fun evaluate(password: String): PasswordStrengthResult {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return PasswordStrengthResult(0, PasswordStrengthLevel.VERY_WEAK)
        if (containsWeakPattern(trimmed)) return PasswordStrengthResult(5, PasswordStrengthLevel.VERY_WEAK)

        val lengthScore = lengthScore(trimmed.length)
        val poolSize = charPoolSize(trimmed)
        val entropyScore = entropyScore(trimmed.length, poolSize)
        val penalty = patternPenalty(trimmed)

        val rawScore = (lengthScore + entropyScore - penalty).coerceIn(0, 100)
        val level = levelOf(rawScore)
        return PasswordStrengthResult(rawScore, level)
    }

    private fun containsWeakPattern(password: String): Boolean {
        val lower = password.lowercase()
        return WEAK_PATTERNS.any { lower.contains(it) }
    }

    private fun lengthScore(length: Int): Int {
        return when {
            length <= 12 -> (length * 4).coerceAtMost(48)
            else -> (48 + (length - 12) * 2).coerceAtMost(60)
        }
    }

    private fun charPoolSize(password: String): Int {
        var pool = 0
        if (password.any { it.isLowerCase() }) pool += 26
        if (password.any { it.isUpperCase() }) pool += 26
        if (password.any { it.isDigit() }) pool += 10
        if (password.any { !it.isLetterOrDigit() && !it.isWhitespace() }) pool += 32
        return pool.coerceAtLeast(1)
    }

    private fun entropyScore(length: Int, poolSize: Int): Int {
        val bits = length * (ln(poolSize.toDouble()) / ln(2.0))
        return (bits / 2.0).roundToInt().coerceIn(0, 52)
    }

    private fun patternPenalty(password: String): Int {
        var penalty = 0
        if (hasSequential(password, 4)) penalty += 15
        if (hasRepeatedChar(password, 4)) penalty += 15
        return penalty
    }

    private fun hasSequential(password: String, length: Int): Boolean {
        val lower = password.lowercase()
        if (lower.length < length) return false
        for (i in 0..lower.length - length) {
            var isSeq = true
            for (j in i until i + length - 1) {
                val c1 = lower[j]
                val c2 = lower[j + 1]
                if (c2.code != c1.code + 1) {
                    isSeq = false
                    break
                }
                if (!c1.isDigit() && !c1.isLetter()) isSeq = false
                if (!c2.isDigit() && !c2.isLetter()) isSeq = false
                if (c1.isDigit() && c2.isDigit()) {
                    continue
                }
                if (c1.isLetter() && c2.isLetter()) {
                    continue
                } else {
                    isSeq = false
                    break
                }
            }
            if (isSeq) return true
        }
        return false
    }

    private fun hasRepeatedChar(password: String, length: Int): Boolean {
        if (password.length < length) return false
        var count = 1
        for (i in 1 until password.length) {
            if (password[i] == password[i - 1]) {
                count++
                if (count >= length) return true
            } else {
                count = 1
            }
        }
        return false
    }

    private fun levelOf(score: Int): PasswordStrengthLevel {
        return when (score) {
            in 0..29 -> PasswordStrengthLevel.VERY_WEAK
            in 30..49 -> PasswordStrengthLevel.WEAK
            in 50..69 -> PasswordStrengthLevel.MEDIUM
            in 70..84 -> PasswordStrengthLevel.GOOD
            else -> PasswordStrengthLevel.STRONG
        }
    }
}
