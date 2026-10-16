package com.aozijx.passly.core.observability

enum class IdentifierNamespace(val wireName: String) {
    TARGET_PACKAGE("target_package"),
    BROWSER_PACKAGE("browser_package"),
}

sealed interface LogValue {
    data class Count(val value: Long) : LogValue
    data class DurationMs(val value: Long) : LogValue
    data class Ratio(val value: Double) : LogValue
    data class BooleanValue(val value: Boolean) : LogValue
    data class EnumValue(val value: EnumName) : LogValue
    data class ErrorCodeValue(val value: ErrorCode) : LogValue
    data class ReasonCodeValue(val value: ReasonCode) : LogValue
    data class OperationCodeValue(val value: OperationCode) : LogValue
    data class HashedIdentifierValue(val value: HashedIdentifier) : LogValue

    /** Pending-only value. PrivacyPolicy must replace or drop it before Sink delivery. */
    data class HashedIdentifierInput(
        val namespace: IdentifierNamespace,
        internal val rawValue: String,
    ) : LogValue {
        init {
            require(rawValue.isNotBlank()) { "Identifier input cannot be blank" }
        }

        override fun toString(): String = "HashedIdentifierInput(namespace=$namespace)"
    }
}

@JvmInline
value class HashedIdentifier(val value: String) {
    init {
        require(value.matches(Regex("^h[0-9]+_[A-Za-z0-9_-]{16,64}$"))) {
            "Invalid hashed identifier"
        }
    }
}

class LogFields private constructor(
    val values: Map<LogFieldKey, LogValue>,
) {
    override fun equals(other: Any?): Boolean = other is LogFields && values == other.values
    override fun hashCode(): Int = values.hashCode()
    override fun toString(): String = "LogFields(keys=${values.keys.map(LogFieldKey::value)})"

    internal fun withDuration(key: LogFieldKey, value: Long): LogFields {
        require(key !in values) { "Duplicate log field" }
        require(values.size < MAX_FIELDS) { "Too many log fields" }
        return LogFields(values + (key to LogValue.DurationMs(value)))
    }

    class Builder internal constructor() {
        private val values = linkedMapOf<LogFieldKey, LogValue>()

        fun count(key: LogFieldKey, value: Int) = count(key, value.toLong())
        fun count(key: LogFieldKey, value: Long) = put(key, LogValue.Count(value))
        fun durationMs(key: LogFieldKey, value: Long) = put(key, LogValue.DurationMs(value))
        fun ratio(key: LogFieldKey, value: Double) = put(key, LogValue.Ratio(value))
        fun boolean(key: LogFieldKey, value: Boolean) = put(key, LogValue.BooleanValue(value))
        fun enum(key: LogFieldKey, value: Enum<*>) = put(key, LogValue.EnumValue(EnumName(value.name)))
        fun errorCode(key: LogFieldKey, value: ErrorCode) = put(key, LogValue.ErrorCodeValue(value))
        fun reason(key: LogFieldKey, value: ReasonCode) = put(key, LogValue.ReasonCodeValue(value))
        fun operation(key: LogFieldKey, value: OperationCode) = put(key, LogValue.OperationCodeValue(value))
        fun hashedIdentifier(
            key: LogFieldKey,
            namespace: IdentifierNamespace,
            rawValue: String,
        ) = put(key, LogValue.HashedIdentifierInput(namespace, rawValue))

        private fun put(key: LogFieldKey, value: LogValue) {
            require(key !in values) { "Duplicate log field" }
            require(values.size < MAX_FIELDS) { "Too many log fields" }
            values[key] = value
        }

        internal fun build(): LogFields = LogFields(values.toMap())
    }

    companion object {
        private const val MAX_FIELDS = 64
        val Empty = LogFields(emptyMap())

        fun build(block: Builder.() -> Unit): LogFields = Builder().apply(block).build()

        internal fun sanitized(values: Map<LogFieldKey, LogValue>): LogFields = LogFields(values.toMap())

        /** Rehydrates values that have already crossed the privacy boundary. */
        fun fromSanitized(values: Map<LogFieldKey, LogValue>): LogFields {
            require(values.size <= MAX_FIELDS) { "Too many log fields" }
            require(values.values.none { it is LogValue.HashedIdentifierInput }) {
                "Pending identifier input cannot enter a sanitized record"
            }
            return LogFields(values.toMap())
        }
    }
}

fun fields(block: LogFields.Builder.() -> Unit): LogFields = LogFields.build(block)
