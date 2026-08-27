package com.aozijx.passly.app.platform.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.aozijx.passly.core.permission.catalog.RuntimePermissionCatalog
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter
import com.aozijx.passly.core.permission.request.PermissionRequestLease

data class PermissionServices(
    val statusReader: PermissionStatusReader,
    val requestArbiter: PermissionRequestArbiter,
    val requestHistory: PermissionRequestHistory
)

val LocalPermissionServices = compositionLocalOf<PermissionServices> {
    error("PermissionServices must be provided at the application UI root")
}

@Composable
fun ProvidePermissionServices(
    services: PermissionServices,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPermissionServices provides services, content = content)
}

@Stable
interface PermissionRequestHost {
    fun status(permission: RuntimePermission): PermissionStatus
    fun shouldShowRationale(permission: RuntimePermission): Boolean
    fun request(permission: RuntimePermission): PermissionRequestStart
}

private data class PendingRequest(
    val permission: RuntimePermission,
    val androidName: String,
    val lease: PermissionRequestLease,
    val requestedBefore: Boolean
)

@Composable
fun rememberPermissionRequestHost(
    owner: String,
    onResult: (RuntimePermission, PermissionRequestOutcome) -> Unit
): PermissionRequestHost {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val services = LocalPermissionServices.current
    val currentOnResult = rememberUpdatedState(onResult)
    val pending = remember { mutableStateOf<PendingRequest?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pending.value ?: return@rememberLauncherForActivityResult
        pending.value = null
        val canAskAgain = !granted && activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                request.androidName
            )
        request.lease.release()
        currentOnResult.value(
            request.permission,
            if (granted) {
                PermissionRequestOutcome.Granted
            } else {
                PermissionRequestOutcome.Denied(
                    canAskAgain = canAskAgain,
                    permanentlyDenied = request.requestedBefore && !canAskAgain
                )
            }
        )
    }

    return remember(owner, activity, services, launcher) {
        object : PermissionRequestHost {
            override fun status(permission: RuntimePermission): PermissionStatus =
                services.statusReader.status(permission)

            override fun shouldShowRationale(permission: RuntimePermission): Boolean {
                val host = activity ?: return false
                val androidName = RuntimePermissionCatalog.androidName(permission) ?: return false
                return ActivityCompat.shouldShowRequestPermissionRationale(host, androidName)
            }

            override fun request(permission: RuntimePermission): PermissionRequestStart {
                if (pending.value != null) return PermissionRequestStart.Busy
                when (services.statusReader.status(permission)) {
                    PermissionStatus.GRANTED -> return PermissionRequestStart.AlreadyGranted
                    PermissionStatus.NOT_APPLICABLE -> return PermissionRequestStart.NotApplicable
                    PermissionStatus.DENIED -> Unit
                }
                if (activity == null) return PermissionRequestStart.HostUnavailable
                val androidName = RuntimePermissionCatalog.androidName(permission)
                    ?: return PermissionRequestStart.NotApplicable
                val lease = services.requestArbiter.tryAcquire(owner)
                    ?: return PermissionRequestStart.Busy
                val requestedBefore = services.requestHistory.wasRequested(permission)
                services.requestHistory.markRequested(permission)
                pending.value = PendingRequest(
                    permission = permission,
                    androidName = androidName,
                    lease = lease,
                    requestedBefore = requestedBefore
                )
                launcher.launch(androidName)
                return PermissionRequestStart.Launched
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
