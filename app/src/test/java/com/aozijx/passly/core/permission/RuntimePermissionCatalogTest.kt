package com.aozijx.passly.core.permission

import android.Manifest
import com.aozijx.passly.core.permission.catalog.RuntimePermissionCatalog
import com.aozijx.passly.core.permission.model.RuntimePermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimePermissionCatalogTest {
    @Test
    fun cameraPermissionMapsToAndroidCameraPermission() {
        assertEquals(
            Manifest.permission.CAMERA,
            RuntimePermissionCatalog.androidName(RuntimePermission.CAMERA)
        )
    }

    @Test
    fun notificationPermissionIsNotApplicableBeforeApi33() {
        assertNull(
            RuntimePermissionCatalog.androidName(
                RuntimePermission.POST_NOTIFICATIONS,
                apiLevel = 32
            )
        )
        assertEquals(
            Manifest.permission.POST_NOTIFICATIONS,
            RuntimePermissionCatalog.androidName(
                RuntimePermission.POST_NOTIFICATIONS,
                apiLevel = 33
            )
        )
    }

}
