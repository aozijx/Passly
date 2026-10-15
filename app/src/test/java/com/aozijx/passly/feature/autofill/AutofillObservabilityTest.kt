package com.aozijx.passly.feature.autofill

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillObservabilityTest {
    @Test
    fun `legacy and credential sources share the complete typed event contract`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val failure = IllegalStateException("secret.example")

        listOf(
            AutofillInteractionSource.LEGACY to Operations.AutofillLegacyRequest,
            AutofillInteractionSource.CREDENTIAL_MANAGER to Operations.AutofillCredentialRequest,
        ).forEach { (source, operation) ->
            logger.withRootTrace(operation) {
                val context = LogStatements.Autofill.context(source, "com.example.target")
                logger.log(LogStatements.Autofill.discoveryStarted(context))
                logger.log(LogStatements.Autofill.discoveryCompleted(context))
                logger.log(LogStatements.Autofill.discoveryUnavailable(Reasons.NoMatchingEntry, context))
                logger.log(LogStatements.Autofill.authenticationStarted())
                logger.log(LogStatements.Autofill.authenticationCompleted())
                logger.log(LogStatements.Autofill.resultDeliveryStarted())
                logger.log(LogStatements.Autofill.resultDelivered())
                logger.log(LogStatements.Autofill.resultDeliveryFailed(failure))
                logger.log(LogStatements.Autofill.sessionCloseStarted())
                logger.log(LogStatements.Autofill.sessionClosed())
                logger.log(LogStatements.Autofill.sessionCloseFailed(failure))
                logger.log(LogStatements.Autofill.saveStarted())
                logger.log(LogStatements.Autofill.saveStaged())
                logger.log(LogStatements.Autofill.saveCompleted())
                logger.log(LogStatements.Autofill.saveUnavailable(Reasons.StorageFailure))
                logger.log(LogStatements.Autofill.saveFailed(failure))
            }
        }

        val expectedEvents = listOf(
            Events.Autofill.DiscoveryStarted,
            Events.Autofill.DiscoveryCompleted,
            Events.Autofill.DiscoveryUnavailable,
            Events.Autofill.AuthenticationStarted,
            Events.Autofill.AuthenticationCompleted,
            Events.Autofill.ResultDeliveryStarted,
            Events.Autofill.ResultDelivered,
            Events.Autofill.ResultDeliveryFailed,
            Events.Autofill.SessionCloseStarted,
            Events.Autofill.SessionClosed,
            Events.Autofill.SessionCloseFailed,
            Events.Autofill.SaveStarted,
            Events.Autofill.SaveStaged,
            Events.Autofill.SaveCompleted,
            Events.Autofill.SaveFailed,
            Events.Autofill.SaveFailed,
        )
        assertEquals(expectedEvents + expectedEvents, records.map { it.event })
        assertTrue(records.filter { it.level == LogLevel.WARN }.all { it.error?.reason != null })
        assertFalse(records.joinToString().contains("com.example.target"))
        assertFalse(records.joinToString().contains("secret.example"))
    }

    @Test
    fun `typed autofill statements hash target package and retain specific unavailable reason`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val context = LogStatements.Autofill.context(
            AutofillInteractionSource.LEGACY,
            "com.secret.target",
        )

        logger.withRootTrace(Operations.AutofillLegacyRequest) {
            logger.log(LogStatements.Autofill.discoveryStarted(context))
            logger.log(
                LogStatements.Autofill.discoveryUnavailable(Reasons.NoMatchingEntry, context),
            )
        }

        assertEquals(
            listOf(Events.Autofill.DiscoveryStarted, Events.Autofill.DiscoveryUnavailable),
            records.map { it.event },
        )
        assertEquals(Reasons.NoMatchingEntry, records.last().error?.reason)
        assertTrue(
            records.all {
                it.fields.values[LogFieldKeys.TargetPackage] is LogValue.HashedIdentifierValue
            },
        )
        assertFalse(records.joinToString().contains("com.secret.target"))
    }

    @Test
    fun `autofill failure snapshot excludes exception message`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)

        logger.withRootTrace(Operations.AutofillCredentialRequest) {
            logger.log(
                LogStatements.Autofill.saveFailed(
                    IllegalStateException("password=secret package=com.private.app"),
                ),
            )
        }

        val record = records.single()
        assertEquals(Events.Autofill.SaveFailed, record.event)
        assertEquals("IllegalStateException", record.error?.exception?.type?.value)
        assertFalse(record.toString().contains("password=secret"))
        assertFalse(record.toString().contains("com.private.app"))
    }

    private fun logger(records: MutableList<SanitizedLogRecord>) = DefaultLogger(
        sanitizer = PrivacyPolicy(
            IdentifierHasher { _, _ -> HashedIdentifier("h1_abcdefghijklmnop") },
            ContractViolationBehavior.THROW,
        ),
        sinks = listOf(LogSink(records::add)),
        lifecycleSinks = emptyList(),
        idGenerator = SequentialIdGenerator(),
        timeSource = object : ObservabilityTimeSource {
            override fun wallTimeMs() = 1_700_000_000_000L
            override fun elapsedRealtimeMs() = 100L
        },
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
        secureSessionProvider = SecureSessionProvider { SecureSessionId("secure_session") },
    )

    private class SequentialIdGenerator : ObservabilityIdGenerator {
        private var next = 0
        private fun id(prefix: String) = "${prefix}_${++next}_value"
        override fun eventId() = EventId(id("event"))
        override fun traceId() = TraceId(id("trace"))
        override fun spanId() = SpanId(id("span"))
    }
}
