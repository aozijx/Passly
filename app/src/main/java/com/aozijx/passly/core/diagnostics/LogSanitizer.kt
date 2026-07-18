package com.aozijx.passly.core.diagnostics

internal object LogSanitizer {
    private val forbiddenKeys = Regex(
        "password|passphrase|recovery|secret|token|credential|username|path|uri|url|domain|email|phone",
        RegexOption.IGNORE_CASE
    )
    private val sensitivePatterns = listOf(
        Regex("(?i)(password|passphrase|token|secret|recovery[_ -]?code)\\s*[:=]\\s*\\S+"),
        Regex("https?://\\S+", RegexOption.IGNORE_CASE),
        Regex("[A-Za-z]:\\\\[^\\s]+"),
        Regex("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}")
    )

    fun sanitize(event: LogEvent): SanitizedLogEvent {
        val safeFields = event.fields.entries
            .filterNot { forbiddenKeys.containsMatchIn(it.key) }
            .associate { (key, value) ->
                key.take(48) to redact(value).take(160)
            }
        val frames = event.throwable?.stackTrace
            ?.asSequence()
            ?.filter { it.className.startsWith("com.aozijx.passly") }
            ?.take(12)
            ?.map { "${it.className}.${it.methodName}:${it.lineNumber}" }
            ?.toList()
            .orEmpty()
        return SanitizedLogEvent(
            level = event.level,
            category = event.category,
            name = redact(event.name).take(96),
            fields = safeFields,
            throwableType = event.throwable?.javaClass?.simpleName,
            appStackFrames = frames,
            correlationId = event.correlationId,
            timestampMs = event.timestampMs
        )
    }

    private fun redact(input: String): String = sensitivePatterns.fold(input) { value, regex ->
        regex.replace(value, "[REDACTED]")
    }
}
