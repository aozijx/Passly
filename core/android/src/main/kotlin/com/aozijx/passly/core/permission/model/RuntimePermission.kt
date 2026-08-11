package com.aozijx.passly.core.permission.model

enum class RuntimePermission {
    CAMERA,
    POST_NOTIFICATIONS
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_APPLICABLE
}

sealed interface PermissionRequestStart {
    data object Launched : PermissionRequestStart
    data object AlreadyGranted : PermissionRequestStart
    data object Busy : PermissionRequestStart
    data object NotApplicable : PermissionRequestStart
    data object HostUnavailable : PermissionRequestStart
}

sealed interface PermissionRequestOutcome {
    data object Granted : PermissionRequestOutcome

    data class Denied(
        val canAskAgain: Boolean,
        val permanentlyDenied: Boolean
    ) : PermissionRequestOutcome
}
