package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.EnumName
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.IdentifierNamespace
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.PendingError
import com.aozijx.passly.core.observability.PendingLogEvent
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.fields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyTest {
    @Test
    fun `registered entry type enum is accepted for vault create events`() {
        val result = PrivacyPolicy(successfulHasher(), ContractViolationBehavior.REJECT).sanitize(
            PendingLogEvent(
                level = LogLevel.INFO,
                event = Events.Vault.EntryCreateStarted,
                fields = fields { enum(LogFieldKeys.EntryType, TestEntryType.LOGIN) },
            ),
            context(),
        )

        val accepted = result as SanitizationResult.Accepted
        assertEquals(
            LogValue.EnumValue(EnumName("LOGIN")),
            accepted.record.fields.values[LogFieldKeys.EntryType],
        )
    }

    @Test
    fun `sensitive and unregistered field names are rejected`() {
        val policy = PrivacyPolicy(successfulHasher(), ContractViolationBehavior.REJECT)
        val unsafeKeys = listOf("password", "access_token", "otp", "callback_url", "file_path", "target_package_plain")

        unsafeKeys.forEach { key ->
            val pending = PendingLogEvent(
                level = LogLevel.INFO,
                event = Events.Autofill.DiscoveryStarted,
                fields = fields { count(LogFieldKey(key), 1) },
            )

            val result = policy.sanitize(pending, context())

            assertTrue(result is SanitizationResult.Rejected)
            assertFalse(result.toString().contains(key))
        }
    }

    @Test
    fun `password token otp url and path identifier inputs are rejected before hashing`() {
        val policy = PrivacyPolicy(
            IdentifierHasher { _, _ -> error("Hasher must not receive invalid identifiers") },
            ContractViolationBehavior.REJECT,
        )
        val unsafeValues = listOf(
            "master-password",
            "Bearer token.value",
            "123456",
            "https://example.com/login",
            "/data/user/0/com.example/files/key",
        )

        unsafeValues.forEach { value ->
            val result = policy.sanitize(
                PendingLogEvent(
                    level = LogLevel.INFO,
                    event = Events.Autofill.DiscoveryStarted,
                    fields = fields {
                        hashedIdentifier(LogFieldKeys.TargetPackage, IdentifierNamespace.TARGET_PACKAGE, value)
                    },
                ),
                context(),
            )

            assertEquals(
                ContractViolation.INVALID_IDENTIFIER_VALUE,
                (result as SanitizationResult.Rejected).violation,
            )
            assertFalse(result.toString().contains(value))
        }
    }

    @Test
    fun `target package is HMAC transformed before sink record`() {
        val rawPackage = "com.example.bank"
        val policy = PrivacyPolicy(successfulHasher(), ContractViolationBehavior.REJECT)
        val pending = PendingLogEvent(
            level = LogLevel.INFO,
            event = Events.Autofill.DiscoveryStarted,
            fields = fields {
                hashedIdentifier(LogFieldKeys.TargetPackage, IdentifierNamespace.TARGET_PACKAGE, rawPackage)
            },
        )

        val accepted = policy.sanitize(pending, context()) as SanitizationResult.Accepted

        val value = accepted.record.fields.values.getValue(LogFieldKeys.TargetPackage)
        assertEquals(LogValue.HashedIdentifierValue(HashedIdentifier("h1_ABCDEFGHIJKLMNOPQRSTUV")), value)
        assertFalse(accepted.toString().contains(rawPackage))
    }

    @Test
    fun `identifier namespace must match its registered field`() {
        val policy = PrivacyPolicy(
            IdentifierHasher { _, _ -> error("Mismatched namespace must not reach HMAC") },
            ContractViolationBehavior.REJECT,
        )
        val result = policy.sanitize(
            PendingLogEvent(
                level = LogLevel.INFO,
                event = Events.Autofill.DiscoveryStarted,
                fields = fields {
                    hashedIdentifier(
                        LogFieldKeys.TargetPackage,
                        IdentifierNamespace.BROWSER_PACKAGE,
                        "com.example.browser",
                    )
                },
            ),
            context(),
        )

        assertEquals(
            ContractViolation.FIELD_TYPE_MISMATCH,
            (result as SanitizationResult.Rejected).violation,
        )
    }

    @Test
    fun `hasher failure drops hashed field and never falls back to raw or digest`() {
        val rawPackage = "com.example.secret"
        val policy = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT)
        val pending = PendingLogEvent(
            level = LogLevel.INFO,
            event = Events.Autofill.DiscoveryStarted,
            fields = fields {
                hashedIdentifier(LogFieldKeys.TargetPackage, IdentifierNamespace.TARGET_PACKAGE, rawPackage)
            },
        )

        val accepted = policy.sanitize(pending, context()) as SanitizationResult.Accepted

        assertTrue(accepted.record.fields.values.isEmpty())
        assertEquals(1, accepted.droppedFieldCount)
        assertFalse(accepted.toString().contains(rawPackage))
    }

    @Test
    fun `unregistered event is rejected before category routing`() {
        val result = PrivacyPolicy(successfulHasher(), ContractViolationBehavior.REJECT).sanitize(
            PendingLogEvent(
                level = LogLevel.INFO,
                event = EventKey("custom.secret_event"),
            ),
            context(),
        )

        assertEquals(
            ContractViolation.EVENT_NOT_REGISTERED,
            (result as SanitizationResult.Rejected).violation,
        )
        assertFalse(result.toString().contains("custom.secret_event"))
    }

    @Test
    fun `debug violation is a programmer assertion outside business exception handling`() {
        val secret = "token_sentinel"
        val policy = PrivacyPolicy(successfulHasher(), ContractViolationBehavior.THROW)

        val exception = assertThrows(LogContractViolation::class.java) {
            policy.sanitize(
                PendingLogEvent(
                    level = LogLevel.INFO,
                    event = Events.Application.BackgroundLockSkipped,
                    fields = fields { count(LogFieldKey(secret), 1) },
                ),
                context(),
            )
        }

        val failure: Throwable = exception
        assertFalse(failure is Exception)
        assertTrue(exception.message.orEmpty().contains(ContractViolation.FIELD_NOT_REGISTERED.name))
        assertFalse(exception.toString().contains(secret))
    }

    @Test
    fun `throwable messages never survive sanitization`() {
        val secret = "MASTER_PASSWORD_SENTINEL"
        val policy = PrivacyPolicy(successfulHasher(), ContractViolationBehavior.REJECT)
        val result = policy.sanitize(
            PendingLogEvent(
                level = LogLevel.ERROR,
                event = Events.Autofill.ResultDeliveryFailed,
                error = PendingError(
                    errorCode = ErrorCodes.Autofill.AutofillResultDeliveryFailed,
                    reason = Reasons.PlatformCallbackFailed,
                    throwable = IllegalStateException(secret),
                ),
            ),
            context(),
        )

        assertTrue(result is SanitizationResult.Accepted)
        assertFalse(result.toString().contains(secret))
    }

    @Test
    fun `pending model string forms do not echo raw identifier or throwable message`() {
        val rawPackage = "com.example.private"
        val throwableSecret = "RECOVERY_CODE_SENTINEL"
        val pending = PendingLogEvent(
            level = LogLevel.ERROR,
            event = Events.Autofill.ResultDeliveryFailed,
            fields = fields {
                hashedIdentifier(LogFieldKeys.TargetPackage, IdentifierNamespace.TARGET_PACKAGE, rawPackage)
            },
            error = PendingError(
                errorCode = ErrorCodes.Autofill.AutofillResultDeliveryFailed,
                reason = Reasons.PlatformCallbackFailed,
                throwable = IllegalStateException(throwableSecret),
            ),
        )

        assertFalse(pending.toString().contains(rawPackage))
        assertFalse(pending.toString().contains(throwableSecret))
    }

    private fun successfulHasher() = IdentifierHasher { namespace, rawValue ->
        assertEquals(IdentifierNamespace.TARGET_PACKAGE, namespace)
        assertTrue(rawValue.startsWith("com."))
        HashedIdentifier("h1_ABCDEFGHIJKLMNOPQRSTUV")
    }

    private fun context() = SanitizationContext(
        eventId = EventId("event_id_123"),
        timestampMs = 1_700_000_000_000,
        elapsedRealtimeMs = 123,
        processSessionId = ProcessSessionId("process_123"),
        secureSessionId = null,
        trace = TraceSnapshot(TraceId("trace_123"), SpanId("span_1234"), null),
    )

    private enum class TestEntryType { LOGIN }
}
