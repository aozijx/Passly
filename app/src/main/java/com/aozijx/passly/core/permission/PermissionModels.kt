package com.aozijx.passly.core.permission

enum class AppPermission {
    Camera,
    Notifications,
    Internet,
    ForegroundService,
    ForegroundServiceDataSync,
    Vibrate,
    Biometric,
    QueryInstalledApps
}

enum class PermissionHandling {
    NotApplicable,
    Manifest,
    Runtime
}

enum class PermissionGrantStatus {
    Granted,
    Denied,
    NotRequired
}

data class PermissionDefinition(
    val permission: AppPermission,
    val androidName: String,
    val availableFromApi: Int = 1,
    val runtimeFromApi: Int? = null
) {
    fun handlingAt(apiLevel: Int): PermissionHandling = when {
        apiLevel < availableFromApi -> PermissionHandling.NotApplicable
        runtimeFromApi != null && apiLevel >= runtimeFromApi -> PermissionHandling.Runtime
        else -> PermissionHandling.Manifest
    }
}

data class PermissionSnapshot(
    val definition: PermissionDefinition,
    val handling: PermissionHandling,
    val status: PermissionGrantStatus,
    val shouldShowRationale: Boolean = false
) {
    val isSatisfied: Boolean
        get() = status != PermissionGrantStatus.Denied

    val canRequest: Boolean
        get() = handling == PermissionHandling.Runtime && status == PermissionGrantStatus.Denied
}

data class PermissionRequestResult(
    val snapshots: List<PermissionSnapshot>
) {
    val allSatisfied: Boolean
        get() = snapshots.all(PermissionSnapshot::isSatisfied)

    operator fun get(permission: AppPermission): PermissionSnapshot? =
        snapshots.firstOrNull { it.definition.permission == permission }
}
