package com.aozijx.passly.presentation.feature.settings.main.navigation

import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsRouteTest {
    @Test
    fun everyMainUiGroupRouteKeyMapsToFeatureRoute() {
        SettingsGroup.entries.forEach { group ->
            val route = SettingsRoute.fromRouteKey(group.routeKey)

            assertNotNull(group.name, route)
            assertEquals(group.routeKey, route?.route)
        }
    }

    @Test
    fun unknownRouteKeyIsRejected() {
        assertEquals(null, SettingsRoute.fromRouteKey("settings/unknown"))
    }
}
