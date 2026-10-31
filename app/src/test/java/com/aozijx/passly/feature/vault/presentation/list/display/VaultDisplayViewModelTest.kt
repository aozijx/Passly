package com.aozijx.passly.feature.vault.presentation.list.display

import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsSource
import com.aozijx.passly.testing.MainDispatcherRule
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    @Test
    fun `hierarchy mode changes do not alter the display projection`() = runTest {
        val interfaceSettings = MutableStateFlow(InterfaceSettings())
        val librarySettings = MutableStateFlow(
            LibraryViewSettings(
                sort = EntrySort(EntrySortField.TITLE, SortDirection.ASC),
                entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
            ),
        )
        val interactionSettings = MutableStateFlow(InteractionSettings())
        val viewModel = VaultDisplayViewModel(
            interfaceSettingsRepository = TestInterfaceSettingsRepository(interfaceSettings),
            librarySettingsSource = object : LibraryViewSettingsSource {
                override val libraryViewSettings = librarySettings.asStateFlow()
            },
            interactionSettingsSource = object : InteractionSettingsSource {
                override val interaction = interactionSettings.asStateFlow()
            },
        )
        backgroundScope.launch { viewModel.config.collect {} }
        runCurrent()
        val initial = viewModel.config.value

        librarySettings.value = librarySettings.value.copy(
            entryHierarchyDisplayMode = EntryHierarchyDisplayMode.EXPANDED,
        )
        runCurrent()

        assertEquals(initial, viewModel.config.value)
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

    private class TestInterfaceSettingsRepository(
        override val interfaceSettings: MutableStateFlow<InterfaceSettings>,
    ) : InterfaceSettingsRepository {
        override suspend fun setHideSystemBars(enabled: Boolean) = Unit
        override suspend fun setTopBarCollapsible(enabled: Boolean) = Unit
        override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = Unit
        override suspend fun setAppCornerRadius(radiusDp: Float) = Unit
    }
}
