package com.aozijx.passly.app.permission

import android.content.Context
import androidx.core.content.edit
import com.aozijx.passly.core.permission.contract.PermissionRequestHistory
import com.aozijx.passly.core.permission.model.RuntimePermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesPermissionRequestHistory @Inject constructor(
    @ApplicationContext context: Context
) : PermissionRequestHistory {
    private val preferences = context.getSharedPreferences(
        "runtime_permission_history",
        Context.MODE_PRIVATE
    )

    override fun wasRequested(permission: RuntimePermission): Boolean =
        preferences.getBoolean(permission.name, false)

    override fun markRequested(permission: RuntimePermission) {
        preferences.edit { putBoolean(permission.name, true) }
    }
}
