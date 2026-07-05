package com.aozijx.passly.core.auth.apppassword

import com.aozijx.passly.domain.model.AppDefaults

object AppPasswordComplexityPolicy {

    fun validate(password: CharArray) {
        if (password.size < AppDefaults.Auth.MIN_PASSWORD_LENGTH) {
            throw IllegalArgumentException("应用密码至少需要 ${AppDefaults.Auth.MIN_PASSWORD_LENGTH} 位")
        }
        var hasLetter = false
        var hasDigit = false
        var hasSymbol = false
        password.forEach { ch ->
            when {
                ch.isLetter() -> hasLetter = true
                ch.isDigit() -> hasDigit = true
                !ch.isWhitespace() -> hasSymbol = true
            }
        }
        val groupCount = listOf(hasLetter, hasDigit, hasSymbol).count { it }
        if (groupCount < 2) {
            throw IllegalArgumentException("请至少混合两种字符类型：字母、数字或符号")
        }
    }
}