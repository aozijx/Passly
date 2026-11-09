package com.aozijx.passly.feature.vault.presentation.list.display

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.EntryCardPresentation
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsSource
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.sampleCardPresentation
import com.aozijx.passly.testing.sampleInteractionSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
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
    fun `remains loading until every persisted display preference arrives`() =
        runTest {
            val viewModel =
                VaultDisplayViewModel(
                    interfaceSettingsRepository = EmptyInterfaceSettingsRepository,
                    librarySettingsSource = EmptyLibraryViewSettingsSource,
                    interactionSettingsSource = EmptyInteractionSettingsSource,
                )

            assertEquals(VaultDisplayUiState.Loading, viewModel.config.value)
        }

    @Test
    fun `settings failure is visible and retry recollects persisted preferences`() =
        runTest {
            var starts = 0
            val cardPresentations = listOf(sampleCardPresentation("login", "password"))
            val librarySettings =
                flow {
                    starts++
                    if (starts == 1) throw CorruptSettingsDataException(IllegalStateException())
                    emit(
                        LibraryViewSettings(
                            sort = EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
                            entryCardPresentations = cardPresentations,
                            entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                        ),
                    )
                    awaitCancellation()
                }
            val viewModel =
                VaultDisplayViewModel(
                    interfaceSettingsRepository =
                        TestInterfaceSettingsRepository(
                            MutableStateFlow(
                                InterfaceSettings(
                                    hideSystemBars = true,
                                    collapseTopBarOnScroll = false,
                                    collapseQuickFilterBarOnScroll = true,
                                    appCornerRadiusDp = 16f,
                                ),
                            ),
                        ),
                    librarySettingsSource =
                        object : LibraryViewSettingsSource {
                            override val libraryViewSettings = librarySettings
                        },
                    interactionSettingsSource =
                        object : InteractionSettingsSource {
                            override val interaction =
                                MutableStateFlow(
                                    InteractionSettings(
                                        isSwipeEnabled = true,
                                        isPullToSearchEnabled = true,
                                        swipeLeftAction = SwipeActionType.COPY_USERNAME,
                                        swipeRightAction = SwipeActionType.DELETE,
                                    ),
                                )
                        },
                )
            backgroundScope.launch { viewModel.config.collect {} }
            runCurrent()

            assertEquals(
                VaultDisplayUiState.Failed(com.aozijx.passly.app.settings.ReadFailure.CORRUPT_DATA),
                viewModel.config.value,
            )
            viewModel.retry()
            runCurrent()

            assertEquals(2, starts)
            assertEquals(
                VaultDisplayUiState.Ready(
                    layout =
                        VaultLayoutConfig(
                            hideSystemBars = true,
                            collapseTopBarOnScroll = false,
                            collapseQuickFilterBarOnScroll = true,
                        ),
                    style = VaultStyleConfig(entryCardPresentations = cardPresentations),
                    interaction =
                        VaultInteractionConfig(
                            isSwipeEnabled = true,
                            isPullToSearchEnabled = true,
                            swipeLeftAction = SwipeActionType.COPY_USERNAME,
                            swipeRightAction = SwipeActionType.DELETE,
                        ),
                ),
                viewModel.config.value,
            )
        }

    @Test
    fun `hierarchy mode changes do not alter the display projection`() =
        runTest {
            val interfaceSettings =
                MutableStateFlow(
                    InterfaceSettings(
                        hideSystemBars = false,
                        collapseTopBarOnScroll = false,
                        collapseQuickFilterBarOnScroll = true,
                        appCornerRadiusDp = 16f,
                    ),
                )
            val librarySettings =
                MutableStateFlow(
                    LibraryViewSettings(
                        sort = EntrySort(EntrySortField.TITLE, SortDirection.ASC),
                        entryCardPresentations = emptyList(),
                        entryHierarchyDisplayMode = EntryHierarchyDisplayMode.COLLAPSED,
                    ),
                )
            val interactionSettings = MutableStateFlow(sampleInteractionSettings())
            val viewModel =
                VaultDisplayViewModel(
                    interfaceSettingsRepository = TestInterfaceSettingsRepository(interfaceSettings),
                    librarySettingsSource =
                        object : LibraryViewSettingsSource {
                            override val libraryViewSettings = librarySettings.asStateFlow()
                        },
                    interactionSettingsSource =
                        object : InteractionSettingsSource {
                            override val interaction = interactionSettings.asStateFlow()
                        },
                )
            backgroundScope.launch { viewModel.config.collect {} }
            runCurrent()
            val initial = viewModel.config.value

            librarySettings.value =
                librarySettings.value.copy(
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
