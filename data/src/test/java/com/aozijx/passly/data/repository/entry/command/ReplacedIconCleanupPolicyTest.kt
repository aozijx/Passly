package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.NotFound
import com.aozijx.passly.core.error.result.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplacedIconCleanupPolicyTest {
    @Test
    fun candidateRequiresAChangedNonBlankPath() {
        assertEquals("old.webp", ReplacedIconCleanupPolicy.candidate("old.webp", "new.webp"))
        assertNull(ReplacedIconCleanupPolicy.candidate("old.webp", "old.webp"))
        assertNull(ReplacedIconCleanupPolicy.candidate(" ", "new.webp"))
        assertNull(ReplacedIconCleanupPolicy.candidate(null, "new.webp"))
    }

    @Test
    fun sharedOrUnknownReferencesKeepTheOldFile() {
        assertTrue(ReplacedIconCleanupPolicy.canDelete(referenceCount = 0))
        assertFalse(ReplacedIconCleanupPolicy.canDelete(referenceCount = 1))
        assertFalse(ReplacedIconCleanupPolicy.canDelete(referenceCount = null))
    }

    @Test
    fun updateSuccessRunsCleanupButFailureDoesNot() = runBlocking {
        var successfulCleanup = false
        AppResult.success(Unit).cleanReplacedIconOnSuccess("old.webp", "new.webp") { old, new ->
            assertEquals("old.webp", old)
            assertEquals("new.webp", new)
            successfulCleanup = true
        }

        var failedCleanup = false
        AppResult.failure(NotFound()).cleanReplacedIconOnSuccess("old.webp", "new.webp") { _, _ ->
            failedCleanup = true
        }

        assertTrue(successfulCleanup)
        assertFalse(failedCleanup)
    }
}
