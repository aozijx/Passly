package com.aozijx.passly.core.permission.internal

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultPermissionRequestArbiterTest {
    @Test
    fun onlyOneOwnerCanHoldPermissionDialogLease() {
        val arbiter = DefaultPermissionRequestArbiter()
        val first = arbiter.tryAcquire("scanner")

        assertNotNull(first)
        assertNull(arbiter.tryAcquire("settings"))

        first!!.release()
        assertNotNull(arbiter.tryAcquire("settings"))
    }

    @Test
    fun releasingLeaseTwiceDoesNotReleaseAnotherOwner() {
        val arbiter = DefaultPermissionRequestArbiter()
        val first = arbiter.tryAcquire("scanner")!!
        first.release()
        val second = arbiter.tryAcquire("settings")!!

        first.release()
        assertNull(arbiter.tryAcquire("third"))

        second.release()
        assertNotNull(arbiter.tryAcquire("third"))
    }
}
