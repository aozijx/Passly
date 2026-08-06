package com.aozijx.passly.core.util

import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.model.PasswordStrengthResult
import kotlin.math.ln
import kotlin.math.roundToInt

object PasswordStrengthEngine {

    private val WEAK_PATTERNS = setOf(
        "123456", "password", "123456789", "12345", "12345678",
        "qwerty", "abc123", "111111", "123123", "admin",
        "letmein", "welcome", "monkey", "dragon", "master"
    )

    fun evaluate(password: String): PasswordStrengthResult {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) return PasswordStrengthResult(0, PasswordStrengthLevel.VERY_WEAK)

        if (containsWeakPattern(trimmed)) return PasswordStrengthResult(5, PasswordStrengthLevel.VERY_WEAK)

        // 1. 长度分:不再硬封顶在7,而是连续增长、边际递减(而非直接截断)
        val lengthScore = lengthScore(trimmed.length) // 见下

        // 2. 字符池大小估算 -> 用信息熵近似替代"类别数线性加分"
        val poolSize = charPoolSize(trimmed)
        val entropyScore = entropyScore(trimmed.length, poolSize)

        // 3. 模式惩罚:连续字符、重复字符、键盘序列
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
        // 到12位前每位仍有增益,之后边际递减,而不是7位就封顶
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
        // 近似熵 bits = length * log2(poolSize),再映射到 0~52 区间
        val bits = length * (ln(poolSize.toDouble()) / ln(2.0))
        return (bits / 2.0).roundToInt().coerceIn(0, 52)
    }

    private fun patternPenalty(password: String): Int {
        var penalty = 0
        if (hasSequential(password, 4)) penalty += 15   // "abcd" "1234"
        if (hasRepeatedChar(password, 4)) penalty += 15  // "aaaa"
        return penalty
    }

    /**
     * 检测是否有至少 length 个连续递增的字符（如 "abcd" 或 "1234"）
     * 仅检查小写字母和数字的递增序列，忽略大小写
     */
    private fun hasSequential(password: String, length: Int): Boolean {
        val lower = password.lowercase()
        if (lower.length < length) return false
        for (i in 0..lower.length - length) {
            var isSeq = true
            for (j in i until i + length - 1) {
                val c1 = lower[j]
                val c2 = lower[j + 1]
                // 判断是否为连续数字或字母（ASCII 码连续）
                if (c2.code != c1.code + 1) {
                    isSeq = false
                    break
                }
                // 防止跨边界如 '9'->':' (ASCII 58) 或 'z'->'{' 等
                if (!c1.isDigit() && !c1.isLetter()) isSeq = false
                if (!c2.isDigit() && !c2.isLetter()) isSeq = false
                if (c1.isDigit() && c2.isDigit()) { /* 数字连续 */
                } else if (c1.isLetter() && c2.isLetter()) { /* 字母连续 */
                } else {
                    isSeq = false
                    break
                }
            }
            if (isSeq) return true
        }
        return false
    }

    /**
     * 检测是否有至少 length 个相同字符连续出现
     */
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
