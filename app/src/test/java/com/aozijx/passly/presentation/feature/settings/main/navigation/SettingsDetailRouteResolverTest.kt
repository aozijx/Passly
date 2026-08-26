package com.aozijx.passly.presentation.feature.settings.main.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDetailRouteResolverTest {
    @Test
    fun singlePaneRetainsOutgoingDetailWhileNavigatorReturnsToList() {
        assertEquals(
            SettingsRoute.Appearance,
            resolveSettingsDetailRoute(
                isSinglePane = true,
                navigatorRoute = null,
                retainedDetailRoute = SettingsRoute.Appearance,
            ),
        )
    }

    @Test
    fun navigatorRouteAlwaysWinsWhenDetailIsCurrent() {
        assertEquals(
            SettingsRoute.Interface,
            resolveSettingsDetailRoute(
                isSinglePane = true,
                navigatorRoute = SettingsRoute.Interface,
                retainedDetailRoute = SettingsRoute.Appearance,
            ),
        )
    }

    @Test
    fun expandedLayoutDoesNotRenderAStaleRetainedDetail() {
        assertEquals(
            null,
            resolveSettingsDetailRoute(
                isSinglePane = false,
                navigatorRoute = null,
                retainedDetailRoute = SettingsRoute.Appearance,
            ),
        )
    }
}
