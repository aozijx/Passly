package com.aozijx.passly.core.permission.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aozijx.passly.core.permission.catalog.RuntimePermissionCatalog
import com.aozijx.passly.core.permission.contract.PermissionStatusReader
import com.aozijx.passly.core.permission.model.PermissionStatus
import com.aozijx.passly.core.permission.model.RuntimePermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPermissionStatusReader @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PermissionStatusReader {
    override fun status(permission: RuntimePermission): PermissionStatus {
        val androidName = RuntimePermissionCatalog.androidName(permission)
            ?: return PermissionStatus.NOT_APPLICABLE
        return if (
            ContextCompat.checkSelfPermission(context, androidName) == PackageManager.PERMISSION_GRANTED
        ) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
}
