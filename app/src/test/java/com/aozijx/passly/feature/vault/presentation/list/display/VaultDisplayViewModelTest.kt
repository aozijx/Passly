package com.aozijx.passly.feature.vault.presentation.list.display

import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsSource
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VaultDisplayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `remains loading until every persisted display preference arrives`() = runTest {
        val viewModel = VaultDisplayViewModel(
            interfaceSettingsRepository = EmptyInterfaceSettingsRepository,
            librarySettingsSource = EmptyLibraryViewSettingsSource,
            interactionSettingsSource = EmptyInteractionSettingsSource,
        )

        assertEquals(VaultDisplayUiState.Loading, viewModel.config.value)
    }

    private object EmptyInterfaceSettingsRepository : InterfaceSettingsRepository {
        override val interfaceSettings = emptyFlow<InterfaceSettings>()
        override suspend fun setHideSystemBars(enabled: Boolean) = Unit
        override suspend fun setTopBarCollapsible(enabled: Boolean) = Unit
        override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = Unit
        override suspend fun setAppCornerRadius(radiusDp: Float) = Unit
    }

    private object EmptyLibraryViewSettingsSource : LibraryViewSettingsSource {
        override val libraryViewSettings = emptyFlow<LibraryViewSettings>()
    }

    private object EmptyInteractionSettingsSource : InteractionSettingsSource {
        override val interaction = emptyFlow<InteractionSettings>()
    }
}
