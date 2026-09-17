package com.aozijx.passly.presentation.feature.settings.main.navigation

import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDestinationTest {
    @Test
    fun everyMainUiGroupRouteKeyMapsToFeatureRoute() {
        SettingsGroup.entries.forEach { group ->
            val route = SettingsDestination.fromRouteKey(group.routeKey)

            assertNotNull(group.name, route)
            assertEquals(group.routeKey, route?.route)
        }
    }

    @Test
    fun unknownRouteKeyIsRejected() {
        assertEquals(null, SettingsDestination.fromRouteKey("settings/unknown"))
    }

    @Test
    fun everyDetailRouteIsRegisteredExactlyOnceWithStableKeys() {
        val routes = settingsDetailRoutes()

        assertEquals(11, routes.size)
        assertEquals(routes.size, routes.distinct().size)
        assertEquals(routes.size, routes.map(SettingsDestination::route).distinct().size)
        assertTrue(SettingsDestination.Main !in routes)
        assertEquals(
            setOf(
                "settings/security", "settings/privacy", "settings/appearance",
                "settings/interface", "settings/interaction", "settings/autofill",
                "settings/data", "settings/backup_restore", "settings/notifications",
                "settings/recovery_code", "settings/general",
            ),
            routes.map(SettingsDestination::route).toSet(),
        )
    }
}
