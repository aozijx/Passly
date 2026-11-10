package com.aozijx.passly.feature.autofill

import com.aozijx.passly.core.observability.AutofillLogSnapshot
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.LogFieldKeys
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
    fun `host exit and framework cancellation log only source metadata`() =
        runTest {
            val records = mutableListOf<SanitizedLogRecord>()
            val logger = logger(records)

            logger.withRootTrace(Operations.AutofillLegacyRequest) {
                logger.log(LogStatements.Autofill.hostAbandoned(AutofillInteractionSource.LEGACY))
                logger.log(LogStatements.Autofill.requestCancelled(AutofillInteractionSource.LEGACY))
            }

            assertEquals(
                listOf(Events.Autofill.HostAbandoned, Events.Autofill.RequestCancelled),
                records.map { it.event },
            )
            assertTrue(records.all { it.fields.values.keys == setOf(LogFieldKeys.Source) })
        }

    @Test
    fun `typed autofill statements hash target package and retain specific unavailable reason`() =
        runTest {
            val records = mutableListOf<SanitizedLogRecord>()
            val logger = logger(records)
            val context =
                LogStatements.Autofill.context(
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
    fun `legacy boundary statements expose safe stage counts without field contents`() =
        runTest {
            val records = mutableListOf<SanitizedLogRecord>()
            val logger = logger(records)

            logger.withRootTrace(Operations.AutofillLegacyRequest) {
                logger.log(
                    LogStatements.Autofill.Legacy.fillRequestReceived(
                        targetPackage = "com.example.androidproject",
                        request =
                            AutofillLogSnapshot.Legacy.FillRequest(
                                fillContextCount = 1,
                                inlineRequestAvailable = false,
                                requestFlags = 0,
                            ),
                    ),
                )
                logger.log(
                    LogStatements.Autofill.Legacy.structureParsed(
                        targetPackage = "com.example.androidproject",
                        snapshot =
                            AutofillLogSnapshot.Legacy.Structure(
                                stage = AutofillLogSnapshot.Legacy.Structure.Stage.FILL,
                                fillContextCount = 1,
                                traversal =
                                    AutofillLogSnapshot.Legacy.Structure.Traversal(
                                        windowCount = 1,
                                        visitedNodeCount = 17,
                                        truncated = false,
                                    ),
                                fields =
                                    AutofillLogSnapshot.Legacy.Structure.Fields(
                                        editableCount = 2,
                                        hintedCount = 2,
                                        focusedCount = 1,
                                        passwordInputTypeCount = 1,
                                    ),
                                target =
                                    AutofillLogSnapshot.Legacy.Structure.Target(
                                        hasPackage = true,
                                        hasWebDomain = false,
                                    ),
                            ),
                    ),
                )
                logger.log(
                    LogStatements.Autofill.fieldsClassified(
                        source = AutofillInteractionSource.LEGACY,
                        targetPackage = "com.example.androidproject",
                        snapshot =
                            AutofillLogSnapshot.Classification(
                                fieldCount = 2,
                                roles =
                                    AutofillLogSnapshot.Classification.Roles(
                                        username = 1,
                                        password = 1,
                                        otp = 0,
                                    ),
                            ),
                    ),
                )
                logger.log(
                    LogStatements.Autofill.Legacy.responseBuilt(
                        targetPackage = "com.example.androidproject",
                        response = AutofillLogSnapshot.Legacy.Response(present = true, datasetCount = 1, hasSaveInfo = true),
                    ),
                )
                logger.log(
                    LogStatements.Autofill.Legacy.saveRequestReceived(
                        targetPackage = "com.example.androidproject",
                        fillContextCount = 2,
                    ),
                )
            }

            assertEquals(
                listOf(
                    Events.Autofill.Legacy.FillRequestReceived,
                    Events.Autofill.Legacy.StructureParsed,
                    Events.Autofill.FieldsClassified,
                    Events.Autofill.Legacy.ResponseBuilt,
                    Events.Autofill.Legacy.SaveRequestReceived,
                ),
                records.map { it.event },
            )
            assertEquals(
                LogValue.Count(17),
                records[1].fields.values[LogFieldKeys.Autofill.Legacy.VisitedNodeCount],
            )
            assertEquals(
                LogValue.Count(1),
                records[2].fields.values[LogFieldKeys.Autofill.PasswordRoleCount],
            )
            assertEquals(
                LogValue.BooleanValue(true),
                records[3].fields.values[LogFieldKeys.Autofill.Legacy.SaveInfoPresent],
            )
            assertTrue(
                records.all {
                    it.fields.values[LogFieldKeys.TargetPackage] is LogValue.HashedIdentifierValue
                },
            )
            assertFalse(records.joinToString().contains("com.example.androidproject"))
            assertFalse(records.joinToString().contains("请输入密码"))
        }

    @Test
    fun `autofill failure snapshot excludes exception message`() =
        runTest {
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
            assertEquals(
                "IllegalStateException",
                record.error
                    ?.exception
                    ?.type
                    ?.value,
            )
            assertFalse(record.toString().contains("password=secret"))
            assertFalse(record.toString().contains("com.private.app"))
        }

    @Test
    fun `platform commit failure records source without throwable text`() =
        runTest {
            val records = mutableListOf<SanitizedLogRecord>()
            val logger = logger(records)

            logger.withRootTrace(Operations.AutofillCredentialRequest) {
                logger.log(
                    LogStatements.Autofill.platformResultCommitFailed(
                        AutofillInteractionSource.CREDENTIAL_MANAGER,
                        IllegalStateException("password=secret"),
                    ),
                )
            }

            val record = records.single()
            assertEquals(Events.Autofill.ResultDeliveryFailed, record.event)
            assertEquals("CREDENTIAL_MANAGER", (record.fields.values[LogFieldKeys.Source] as LogValue.EnumValue).value.value)
            assertFalse(record.toString().contains("password=secret"))
        }

    private fun logger(records: MutableList<SanitizedLogRecord>) =
        DefaultLogger(
            sanitizer =
                PrivacyPolicy(
                    IdentifierHasher { _, _ -> HashedIdentifier("h1_abcdefghijklmnop") },
                    ContractViolationBehavior.THROW,
                ),
            sinks = listOf(LogSink(records::add)),
            lifecycleSinks = emptyList(),
            idGenerator = SequentialIdGenerator(),
            timeSource =
                object : ObservabilityTimeSource {
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
