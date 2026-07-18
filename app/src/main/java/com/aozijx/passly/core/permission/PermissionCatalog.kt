package com.aozijx.passly.core.permission

import android.Manifest
import android.os.Build

object PermissionCatalog {
    val definitions: List<PermissionDefinition> = listOf(
        PermissionDefinition(
            permission = AppPermission.Camera,
            androidName = Manifest.permission.CAMERA,
            runtimeFromApi = Build.VERSION_CODES.M
        ),
        PermissionDefinition(
            permission = AppPermission.Notifications,
            androidName = Manifest.permission.POST_NOTIFICATIONS,
            availableFromApi = Build.VERSION_CODES.TIRAMISU,
            runtimeFromApi = Build.VERSION_CODES.TIRAMISU
        ),
        PermissionDefinition(
            permission = AppPermission.Internet,
            androidName = Manifest.permission.INTERNET
        ),
        PermissionDefinition(
            permission = AppPermission.ForegroundService,
            androidName = Manifest.permission.FOREGROUND_SERVICE,
            availableFromApi = Build.VERSION_CODES.P
        ),
        PermissionDefinition(
            permission = AppPermission.ForegroundServiceDataSync,
            androidName = Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            availableFromApi = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ),
        PermissionDefinition(
            permission = AppPermission.Vibrate,
            androidName = Manifest.permission.VIBRATE
        ),
        PermissionDefinition(
            permission = AppPermission.Biometric,
            androidName = Manifest.permission.USE_BIOMETRIC,
            availableFromApi = Build.VERSION_CODES.P
        ),
        PermissionDefinition(
            permission = AppPermission.QueryInstalledApps,
            androidName = Manifest.permission.QUERY_ALL_PACKAGES,
            availableFromApi = Build.VERSION_CODES.R
        )
    )

    private val definitionsByPermission = definitions.associateBy(PermissionDefinition::permission)

    fun definition(permission: AppPermission): PermissionDefinition =
        requireNotNull(definitionsByPermission[permission]) {
            "Permission is missing from the catalog: $permission"
        }
}
