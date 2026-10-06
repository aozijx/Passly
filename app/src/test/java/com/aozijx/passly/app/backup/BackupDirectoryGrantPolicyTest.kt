package com.aozijx.passly.app.backup

import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.core.platform.storage.resolvePersistedDirectoryAccess
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupDirectoryGrantPolicyTest {
    @Test
    fun `null or blank directory is not configured`() {
        assertEquals(
            PersistedDirectoryAccessStatus.NOT_CONFIGURED,
            resolvePersistedDirectoryAccess(
                configured = false,
                uriValid = false,
                hasWriteGrant = false,
                providerAvailable = false,
            ),
        )
    }

    @Test
    fun `persisted writable grant with available provider is granted`() {
        assertEquals(
            PersistedDirectoryAccessStatus.GRANTED,
            resolvePersistedDirectoryAccess(
                configured = true,
                uriValid = true,
                hasWriteGrant = true,
                providerAvailable = true,
            ),
        )
    }

    @Test
    fun `configured directory without persisted write grant is missing`() {
        assertEquals(
            PersistedDirectoryAccessStatus.GRANT_MISSING,
            resolvePersistedDirectoryAccess(
                configured = true,
                uriValid = true,
                hasWriteGrant = false,
                providerAvailable = true,
            ),
        )
    }

    @Test
    fun `malformed uri or unavailable provider is unavailable`() {
        assertEquals(
            PersistedDirectoryAccessStatus.UNAVAILABLE,
            resolvePersistedDirectoryAccess(
                configured = true,
                uriValid = false,
                hasWriteGrant = true,
                providerAvailable = true,
            ),
        )
        assertEquals(
            PersistedDirectoryAccessStatus.UNAVAILABLE,
            resolvePersistedDirectoryAccess(
                configured = true,
                uriValid = true,
                hasWriteGrant = true,
                providerAvailable = false,
            ),
        )
    }
}
