package com.aozijx.passly.feature.settings.presentation.main.navigation

import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDetailRouteResolverTest {
    @Test
    fun diagnosticsUsesForwardTransitionFromDataManagement() {
        assertEquals(
            SettingsDetailTransitionDirection.Forward,
            resolveSettingsDetailTransitionDirection(
                initial = SettingsDestination.DataManagement,
                target = SettingsDestination.Diagnostics,
            ),
        )
    }

    @Test
    fun diagnosticsBackUsesReverseTransitionToDataManagement() {
        assertEquals(
            SettingsDetailTransitionDirection.Backward,
            resolveSettingsDetailTransitionDirection(
                initial = SettingsDestination.Diagnostics,
                target = SettingsDestination.DataManagement,
            ),
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Test
    fun nestedDetailBackReturnsToItsParentDetail() {
        assertEquals(
            BackNavigationBehavior.PopLatest,
            settingsBackNavigationBehavior,
        )
    }

    @Test
    fun detailTargetEntersAboveList() {
        assertEquals(
            SettingsSinglePaneTargetLayer.Foreground,
            resolveSettingsSinglePaneTargetLayer(
                initial = SettingsSinglePanePage.List,
                target = SettingsSinglePanePage.Detail,
            ),
        )
    }

    @Test
    fun listTargetEntersBehindExitingDetail() {
        assertEquals(
            SettingsSinglePaneTargetLayer.Background,
            resolveSettingsSinglePaneTargetLayer(
                initial = SettingsSinglePanePage.Detail,
                target = SettingsSinglePanePage.List,
            ),
        )
    }

    @Test
    fun unchangedSinglePanePageUsesDefaultLayer() {
        SettingsSinglePanePage.entries.forEach { page ->
            assertEquals(
                SettingsSinglePaneTargetLayer.Default,
                resolveSettingsSinglePaneTargetLayer(initial = page, target = page),
            )
        }
    }

    @Test
    fun singlePaneRetainsOutgoingDetailWhileNavigatorReturnsToList() {
        assertEquals(
            SettingsDestination.Appearance,
            resolveSettingsDetailRoute(
                isSinglePane = true,
                navigatorRoute = null,
                retainedDetailRoute = SettingsDestination.Appearance,
            ),
        )
    }

    @Test
    fun navigatorRouteAlwaysWinsWhenDetailIsCurrent() {
        assertEquals(
            SettingsDestination.Interface,
            resolveSettingsDetailRoute(
                isSinglePane = true,
                navigatorRoute = SettingsDestination.Interface,
                retainedDetailRoute = SettingsDestination.Appearance,
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
                retainedDetailRoute = SettingsDestination.Appearance,
            ),
        )
    }
}
