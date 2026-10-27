package com.aozijx.passly.app.platform.permission

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.internal.DefaultPermissionRequestArbiter
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PermissionRequestHostTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun disposingHostReleasesLeaseAndRetainedHostCannotLaunch() {
        val visible = mutableStateOf(true)
        val registry = RecordingRegistry()
        val arbiter = DefaultPermissionRequestArbiter()
        val services = services(arbiter)
        val hosts = mutableListOf<PermissionRequestHost>()
        val results = mutableListOf<PermissionRequestOutcome>()
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                ProvidePermissionServices(services) {
                    if (visible.value) {
                        val host = rememberPermissionRequestHost("scanner") { _, result -> results += result }
                        SideEffect { hosts += host }
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(PermissionRequestStart.Launched, hosts.last().request(RuntimePermission.CAMERA))
            visible.value = false
        }
        compose.runOnIdle {
            val lease = requireNotNull(arbiter.tryAcquire("other"))
            assertEquals(PermissionRequestStart.HostUnavailable, hosts.last().request(RuntimePermission.CAMERA))
            registry.dispatchResult(registry.requestCodes.single(), true)
            assertEquals(emptyList<PermissionRequestOutcome>(), results)
            lease.release()
        }
    }

    @Test
    fun changingOwnerRegistersIndependentLauncherAndRejectsOldResult() {
        val owner = mutableStateOf("first")
        val registry = RecordingRegistry()
        val arbiter = DefaultPermissionRequestArbiter()
        val services = services(arbiter)
        val hosts = mutableMapOf<String, PermissionRequestHost>()
        val results = mutableListOf<Pair<String, PermissionRequestOutcome>>()
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner(registry)) {
                ProvidePermissionServices(services) {
                    val name = owner.value
                    val host = rememberPermissionRequestHost(name) { _, result -> results += name to result }
                    SideEffect { hosts[name] = host }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(PermissionRequestStart.Launched, hosts.getValue("first").request(RuntimePermission.CAMERA))
            owner.value = "second"
        }
        compose.runOnIdle {
            assertEquals(PermissionRequestStart.HostUnavailable, hosts.getValue("first").request(RuntimePermission.CAMERA))
            assertEquals(PermissionRequestStart.Launched, hosts.getValue("second").request(RuntimePermission.CAMERA))
            registry.dispatchResult(registry.requestCodes.first(), true)
            assertEquals(emptyList<Pair<String, PermissionRequestOutcome>>(), results)
            assertNull(arbiter.tryAcquire("third"))
            registry.dispatchResult(registry.requestCodes.last(), true)
            assertEquals(listOf("second" to PermissionRequestOutcome.Granted), results)
            requireNotNull(arbiter.tryAcquire("third")).release()
        }
    }

    private fun services(arbiter: DefaultPermissionRequestArbiter) = PermissionServices(
        statusReader = PermissionStatusReader { PermissionStatus.DENIED },
        requestArbiter = arbiter,
        requestHistory = object : PermissionRequestHistory {
            override fun wasRequested(permission: RuntimePermission) = false
            override fun markRequested(permission: RuntimePermission) = Unit
        },
    )

    private fun registryOwner(registry: ActivityResultRegistry) = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = registry
    }

    private class RecordingRegistry : ActivityResultRegistry() {
        val requestCodes = mutableListOf<Int>()
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            requestCodes += requestCode
        }
    }
}
