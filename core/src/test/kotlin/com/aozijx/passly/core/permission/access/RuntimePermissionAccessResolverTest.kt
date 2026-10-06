package com.aozijx.passly.core.permission.access

import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimePermissionAccessResolverTest {
    @Test
    fun `granted permission resolves to granted`() {
        assertEquals(
            RuntimePermissionAccess.GRANTED,
            RuntimePermissionAccessResolver.resolve(
                status = PermissionStatus.GRANTED,
                requestedBefore = true,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `not applicable permission resolves without request action`() {
        assertEquals(
            RuntimePermissionAccess.NOT_APPLICABLE,
            RuntimePermissionAccessResolver.resolve(
                status = PermissionStatus.NOT_APPLICABLE,
                requestedBefore = true,
                shouldShowRationale = true
            )
        )
    }

    @Test
    fun `first denial remains requestable without rationale`() {
        assertEquals(
            RuntimePermissionAccess.REQUESTABLE,
            RuntimePermissionAccessResolver.resolve(
                status = PermissionStatus.DENIED,
                requestedBefore = false,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `denial with rationale remains requestable`() {
        assertEquals(
            RuntimePermissionAccess.REQUESTABLE,
            RuntimePermissionAccessResolver.resolve(
                status = PermissionStatus.DENIED,
                requestedBefore = true,
                shouldShowRationale = true
            )
        )
    }

    @Test
    fun `previous denial without rationale requires settings`() {
        assertEquals(
            RuntimePermissionAccess.SETTINGS_REQUIRED,
            RuntimePermissionAccessResolver.resolve(
                status = PermissionStatus.DENIED,
                requestedBefore = true,
                shouldShowRationale = false
            )
        )
    }
}
