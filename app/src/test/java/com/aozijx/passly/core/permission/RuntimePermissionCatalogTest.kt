package com.aozijx.passly.core.permission

import android.Manifest
import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.catalog.RuntimePermissionCatalog
import com.aozijx.passly.core.permission.model.RuntimePermission
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePermissionCatalogTest {
    @Test
    fun runtimePermissionsAreDeclaredInManifest() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val declared = Regex(
            """<uses-permission\s+android:name="([^"]+)""""
        ).findAll(manifest).map { it.groupValues[1] }.toSet()

        assertTrue(Manifest.permission.CAMERA in declared)
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in declared)
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

    @Test
    fun manifestCapabilitiesAreNotRuntimePermissions() {
        assertEquals(
            setOf(
                ManifestCapability.INTERNET,
                ManifestCapability.VIBRATE,
                ManifestCapability.USE_BIOMETRIC,
                ManifestCapability.PACKAGE_VISIBILITY
            ),
            ManifestCapability.entries.toSet()
        )
        assertEquals(
            setOf(RuntimePermission.CAMERA, RuntimePermission.POST_NOTIFICATIONS),
            RuntimePermission.entries.toSet()
        )
    }
}
