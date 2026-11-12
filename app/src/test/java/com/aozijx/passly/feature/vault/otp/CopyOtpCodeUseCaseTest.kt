package com.aozijx.passly.feature.vault.otp

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.clipboard.port.SensitiveClipboardWriter
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.port.ActivityRecorder
import com.aozijx.passly.feature.vault.copy.SensitiveCopyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyOtpCodeUseCaseTest {
    @Test
    fun codeIsRequestedAfterAuthorizationAndRecordedAfterCopy() =
        runTest {
            val gate = RecordingAuthorizationGate(allowed = true)
            val runtime = RecordingOtpRuntime("123456")
            val clipboard = RecordingClipboard()
            val activity = RecordingActivityRecorder()

            val result = CopyOtpCodeUseCase(gate, clipboard, activity, runtime)(ENTRY_ID) { true }

            assertEquals(SensitiveCopyResult.Copied, result)
            assertEquals(AuthorizationScope.Global(AuthenticationPurpose.COPY_SECRET), gate.scope)
            assertEquals(1, runtime.copyRequests)
            assertEquals(listOf("123456"), clipboard.values)
            assertEquals(listOf(ENTRY_ID.value to ActivityType.COPY_OTP_CODE), activity.records)
        }

    @Test
    fun cancelledAuthorizationDoesNotRequestCodeOrWriteActivity() =
        runTest {
            val gate = RecordingAuthorizationGate(allowed = false)
            val runtime = RecordingOtpRuntime("123456")
            val clipboard = RecordingClipboard()
            val activity = RecordingActivityRecorder()

            val result = CopyOtpCodeUseCase(gate, clipboard, activity, runtime)(ENTRY_ID) { true }

            assertEquals(SensitiveCopyResult.NotAuthorized, result)
            assertEquals(0, runtime.copyRequests)
            assertTrue(clipboard.values.isEmpty())
            assertTrue(activity.records.isEmpty())
        }

    private class RecordingAuthorizationGate(
        private val allowed: Boolean,
    ) : AuthorizationGate {
        var scope: AuthorizationScope? = null

        override suspend fun <T> authorize(
            scope: AuthorizationScope,
            input: AuthInput,
            block: suspend (AuthorizationPermit) -> T,
        ): AuthorizationResult<T> {
            this.scope = scope
            return if (allowed) {
                AuthorizationResult.Allowed(block(object : AuthorizationPermit {}))
            } else {
                AuthorizationResult.Cancelled
            }
        }
    }

    private class RecordingOtpRuntime(
        private val code: String?,
    ) : OtpCodeRuntimePort {
        override val states = MutableStateFlow<Map<String, OtpCodeState>>(emptyMap())
        var copyRequests = 0

        override fun acquire(entryId: String) = OtpLease {}

        override suspend fun codeForCopy(entryId: String): String? {
            copyRequests++
            return code
        }

        override suspend fun rollbackLastHotpCounter(entryId: String) = false

        override fun entryRemoved(entryId: String) = Unit

        override fun entryChanged(entryId: String) = Unit
    }

    private class RecordingClipboard : SensitiveClipboardWriter {
        val values = mutableListOf<String>()

        override suspend fun writeSensitive(text: String) {
            values += text
        }
    }

    private class RecordingActivityRecorder : ActivityRecorder {
        val records = mutableListOf<Pair<String, ActivityType>>()

        override suspend fun recordUsage(
            entryId: String,
            type: ActivityType,
            affectedFields: Set<com.aozijx.passly.domain.entry.model.FieldKey>,
        ): AppResult<Unit> {
            records += entryId to type
            return AppResult.Success(Unit)
        }

        override suspend fun deleteByEntryId(entryId: String) = Unit

        override suspend fun deleteBefore(timestamp: Long) = Unit
    }

    private companion object {
        val ENTRY_ID = EntryId("otp-entry")
    }
}
