package com.aozijx.passly.core.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AppPermissionManager {
    val definitions: List<PermissionDefinition>

    fun snapshot(permission: AppPermission, activity: Activity? = null): PermissionSnapshot

    fun snapshots(
        permissions: Collection<AppPermission> = AppPermission.entries,
        activity: Activity? = null
    ): List<PermissionSnapshot>

    fun requestableAndroidNames(permissions: Collection<AppPermission>): List<String>

    fun createAppSettingsIntent(): Intent
}

@Singleton
class AndroidAppPermissionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppPermissionManager {
    override val definitions: List<PermissionDefinition>
        get() = PermissionCatalog.definitions

    override fun snapshot(
        permission: AppPermission,
        activity: Activity?
    ): PermissionSnapshot {
        val definition = PermissionCatalog.definition(permission)
        val handling = definition.handlingAt(Build.VERSION.SDK_INT)
        if (handling == PermissionHandling.NotApplicable) {
            return PermissionSnapshot(
                definition = definition,
                handling = handling,
                status = PermissionGrantStatus.NotRequired
            )
        }

        val status = if (
            ContextCompat.checkSelfPermission(context, definition.androidName) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            PermissionGrantStatus.Granted
        } else {
            PermissionGrantStatus.Denied
        }
        val shouldShowRationale = activity != null &&
                handling == PermissionHandling.Runtime &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    definition.androidName
                )

        return PermissionSnapshot(
            definition = definition,
            handling = handling,
            status = status,
            shouldShowRationale = shouldShowRationale
        )
    }

    override fun snapshots(
        permissions: Collection<AppPermission>,
        activity: Activity?
    ): List<PermissionSnapshot> = permissions.map { snapshot(it, activity) }

    override fun requestableAndroidNames(
        permissions: Collection<AppPermission>
    ): List<String> = permissions
        .map(::snapshot)
        .filter(PermissionSnapshot::canRequest)
        .map { it.definition.androidName }

    override fun createAppSettingsIntent(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
