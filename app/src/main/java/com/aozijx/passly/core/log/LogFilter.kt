package com.aozijx.passly.core.log

interface LogFilter {
    fun filter(message: String): String
    fun filterThrowable(throwable: Throwable): Throwable = throwable // 默认不处理
}

// 默认脱敏实现：替换常见敏感模式
class SensitiveDataFilter : LogFilter {
    // 可配置正则列表
    private val patterns = listOf(
        "password\\s*[:=]\\s*\\S+" to "password=***",
        "token\\s*[:=]\\s*\\S+" to "token=***",
        "api_key\\s*[:=]\\s*\\S+" to "api_key=***",
        "\\w+@\\w+\\.\\w+" to "***@***.***",
        "phone\\s*[:=]\\s*\\d{11}" to "phone=***",
        "\\b\\d{11}\\b" to "***",
    )

    override fun filter(message: String): String {
        var result = message
        patterns.forEach { (regex, replacement) ->
            result = result.replace(Regex(regex, RegexOption.IGNORE_CASE), replacement)
        }
        return result
    }
}

// 空过滤器（不过滤）
class NoOpFilter : LogFilter {
    override fun filter(message: String) = message
}