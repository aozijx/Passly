package com.aozijx.passly.app.platform.permission

import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.internal.DefaultPermissionRequestArbiter
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.RuntimePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PermissionRequestSessionTest {
    @Test
    fun `closing pending host frees global lease and ignores late result`() {
        val arbiter = DefaultPermissionRequestArbiter()
        val session = PermissionRequestSession("scanner", arbiter, History())
        assertEquals(PermissionRequestStart.Launched, session.request(RuntimePermission.CAMERA, "camera") {
            PermissionRequestStart.Launched
        })
        assertNull(arbiter.tryAcquire("other"))
        session.close()
        session.close()
        val nextLease = requireNotNull(arbiter.tryAcquire("settings"))
        session.complete { _, _, _ -> error("late result delivered") }
        assertNull(arbiter.tryAcquire("third"))
        assertEquals(PermissionRequestStart.HostUnavailable, session.request(RuntimePermission.CAMERA, "camera") {
            error("closed session launched")
        })
        nextLease.release()
    }

    @Test
    fun `history failure and launcher failure both release lease`() {
        for (failHistory in listOf(false, true)) {
            val arbiter = DefaultPermissionRequestArbiter()
            val session = PermissionRequestSession("scanner", arbiter, History(failHistory))
            assertThrows(IllegalStateException::class.java) {
                session.request(RuntimePermission.CAMERA, "camera") { error("synthetic launch failure") }
            }
            assertNotNull(arbiter.tryAcquire("settings"))
        }
    }

    @Test
    fun `unavailable launcher can retry without leaving a pending request`() {
        val session = PermissionRequestSession("scanner", DefaultPermissionRequestArbiter(), History())
        assertEquals(PermissionRequestStart.HostUnavailable, session.request(RuntimePermission.CAMERA, "camera") {
            PermissionRequestStart.HostUnavailable
        })
        assertEquals(PermissionRequestStart.Launched, session.request(RuntimePermission.CAMERA, "camera") {
            PermissionRequestStart.Launched
        })
        assertEquals(PermissionRequestStart.Busy, session.request(RuntimePermission.CAMERA, "camera") {
            error("duplicate launch")
        })
        session.close()
    }

    @Test
    fun `result releases lease before rationale and consumer can fail`() {
        val arbiter = DefaultPermissionRequestArbiter()
        val history = History()
        val session = PermissionRequestSession("scanner", arbiter, history)
        session.request(RuntimePermission.CAMERA, "camera") { PermissionRequestStart.Launched }
        assertThrows(IllegalArgumentException::class.java) {
            session.complete { permission, name, requestedBefore ->
                assertEquals(RuntimePermission.CAMERA, permission)
                assertEquals("camera", name)
                assertEquals(false, requestedBefore)
                val nextLease = requireNotNull(arbiter.tryAcquire("settings"))
                nextLease.release()
                throw IllegalArgumentException("synthetic consumer failure")
            }
        }
        session.complete { _, _, _ -> error("duplicate result") }
        session.request(RuntimePermission.CAMERA, "camera") { PermissionRequestStart.Launched }
        session.complete { _, _, requestedBefore -> assertEquals(true, requestedBefore) }
        assertNotNull(arbiter.tryAcquire("other"))
    }

    private class History(private val fail: Boolean = false) : PermissionRequestHistory {
        private val permissions = mutableSetOf<RuntimePermission>()
        override fun wasRequested(permission: RuntimePermission) = permission in permissions
        override fun markRequested(permission: RuntimePermission) {
            check(!fail) { "synthetic history failure" }
            permissions += permission
        }
    }
}
