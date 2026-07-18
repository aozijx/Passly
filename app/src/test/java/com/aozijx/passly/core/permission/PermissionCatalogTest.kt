package com.aozijx.passly.core.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PermissionCatalogTest {
    @Test
    fun catalogMatchesManifestPermissionDeclarations() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val declaredPermissions = Regex(
            """<uses-permission\s+android:name="([^"]+)""""
        ).findAll(manifest).map { it.groupValues[1] }.toSet()
        val catalogPermissions = PermissionCatalog.definitions
            .map(PermissionDefinition::androidName)
            .toSet()

        assertEquals(declaredPermissions, catalogPermissions)
    }

    @Test
    fun catalogIdentifiersAndAndroidNamesAreUnique() {
        val definitions = PermissionCatalog.definitions

        assertEquals(definitions.size, definitions.map { it.permission }.toSet().size)
        assertEquals(definitions.size, definitions.map { it.androidName }.toSet().size)
        assertEquals(AppPermission.entries.toSet(), definitions.map { it.permission }.toSet())
    }

    @Test
    fun runtimeAndManifestPoliciesRespectApiLevel() {
        assertEquals(
            PermissionHandling.Runtime,
            PermissionCatalog.definition(AppPermission.Camera).handlingAt(31)
        )
        assertEquals(
            PermissionHandling.NotApplicable,
            PermissionCatalog.definition(AppPermission.Notifications).handlingAt(32)
        )
        assertEquals(
            PermissionHandling.Runtime,
            PermissionCatalog.definition(AppPermission.Notifications).handlingAt(33)
        )
        assertEquals(
            PermissionHandling.NotApplicable,
            PermissionCatalog.definition(AppPermission.ForegroundServiceDataSync).handlingAt(33)
        )
        assertEquals(
            PermissionHandling.Manifest,
            PermissionCatalog.definition(AppPermission.ForegroundServiceDataSync).handlingAt(34)
        )
        assertTrue(
            PermissionCatalog.definitions
                .filter { it.runtimeFromApi == null }
                .none { it.handlingAt(36) == PermissionHandling.Runtime }
        )
    }
}
