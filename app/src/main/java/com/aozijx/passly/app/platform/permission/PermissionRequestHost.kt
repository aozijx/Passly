package com.aozijx.passly.app.platform.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.aozijx.passly.core.permission.access.RuntimePermissionAccessResolver
import com.aozijx.passly.core.permission.catalog.RuntimePermissionCatalog
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.permission.request.PermissionRequestArbiter

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
    fun access(permission: RuntimePermission): RuntimePermissionAccess
    fun shouldShowRationale(permission: RuntimePermission): Boolean
    fun request(permission: RuntimePermission): PermissionRequestStart
}

@Composable
fun rememberPermissionRequestHost(
    owner: String,
    onResult: (RuntimePermission, PermissionRequestOutcome) -> Unit
): PermissionRequestHost {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val services = LocalPermissionServices.current
    val session = remember(owner, activity, services) {
        PermissionRequestSession(owner, services.requestArbiter, services.requestHistory)
    }
    val launcher = key(session) {
        val currentOnResult = rememberUpdatedState(onResult)
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            session.complete { permission, androidName, requestedBefore ->
                val canAskAgain = !granted && activity != null &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, androidName)
                currentOnResult.value(
                    permission,
                    if (granted) {
                        PermissionRequestOutcome.Granted
                    } else {
                        PermissionRequestOutcome.Denied(
                            canAskAgain = canAskAgain,
                            permanentlyDenied = requestedBefore && !canAskAgain,
                        )
                    },
                )
            }
        }
    }
    DisposableEffect(session) {
        onDispose { session.close() }
    }

    return remember(session, activity, services, launcher) {
        object : PermissionRequestHost {
            override fun status(permission: RuntimePermission): PermissionStatus =
                services.statusReader.status(permission)

            override fun access(permission: RuntimePermission): RuntimePermissionAccess =
                RuntimePermissionAccessResolver.resolve(
                    status = status(permission),
                    requestedBefore = services.requestHistory.wasRequested(permission),
                    shouldShowRationale = shouldShowRationale(permission)
                )

            override fun shouldShowRationale(permission: RuntimePermission): Boolean {
                val host = activity ?: return false
                val androidName = RuntimePermissionCatalog.androidName(permission) ?: return false
                return ActivityCompat.shouldShowRequestPermissionRationale(host, androidName)
            }

            override fun request(permission: RuntimePermission): PermissionRequestStart {
                if (session.isClosed) return PermissionRequestStart.HostUnavailable
                if (session.hasPendingRequest) return PermissionRequestStart.Busy
                when (services.statusReader.status(permission)) {
                    PermissionStatus.GRANTED -> return PermissionRequestStart.AlreadyGranted
                    PermissionStatus.NOT_APPLICABLE -> return PermissionRequestStart.NotApplicable
                    PermissionStatus.DENIED -> Unit
                }
                if (activity == null) return PermissionRequestStart.HostUnavailable
                val androidName = RuntimePermissionCatalog.androidName(permission)
                    ?: return PermissionRequestStart.NotApplicable
                return session.request(permission, androidName) { name ->
                    try {
                        launcher.launch(name)
                        PermissionRequestStart.Launched
                    } catch (_: IllegalStateException) {
                        PermissionRequestStart.HostUnavailable
                    } catch (_: ActivityNotFoundException) {
                        PermissionRequestStart.HostUnavailable
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
