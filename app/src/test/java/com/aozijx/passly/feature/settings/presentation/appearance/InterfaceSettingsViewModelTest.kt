package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModelStore
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.query.EntrySortField
import com.aozijx.passly.domain.entry.model.query.SortDirection
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterfaceSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `all interface writes report storage failures and retain repository state`() = runTest {
        val repository = Repository { throw IOException("synthetic private diagnostic") }
        val model = InterfaceSettingsViewModel(repository, repository)
        model.onPageEntered()
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        backgroundScope.launch { model.uiState.toList() }
        runCurrent()
        val before = model.uiState.value
        val actions = listOf(
            InterfaceSettingsAction.SetHideSystemBars(true),
            InterfaceSettingsAction.SetTopBarCollapsible(true),
            InterfaceSettingsAction.SetQuickFilterBarCollapsible(true),
            InterfaceSettingsAction.SetAppCornerRadius(24f),
            InterfaceSettingsAction.SetEntryHierarchyDisplayMode(EntryHierarchyDisplayMode.EXPANDED),
        )
        actions.forEach(model::onAction)
        runCurrent()
        assertEquals(List(actions.size) { UiErrorMessage(R.string.ui_error_file_io) }, errors)
        assertEquals(before, model.uiState.value)
    }

    @Test
    fun `hierarchy selection follows repository emissions and success stays silent`() = runTest {
        val stored = CompletableDeferred<Unit>()
        val repository = Repository { stored.await() }
        val model = InterfaceSettingsViewModel(repository, repository)
        model.onPageEntered()
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        backgroundScope.launch { model.uiState.toList() }
        runCurrent()
        val before = model.uiState.value
        model.onAction(InterfaceSettingsAction.SetEntryHierarchyDisplayMode(EntryHierarchyDisplayMode.EXPANDED))
        runCurrent()
        assertEquals(before, model.uiState.value)
        stored.complete(Unit)
        runCurrent()
        assertEquals(before, model.uiState.value)
        repository.libraryViewSettings.value = repository.libraryViewSettings.value.copy(
            entryHierarchyDisplayMode = EntryHierarchyDisplayMode.EXPANDED,
        )
        runCurrent()
        assertEquals(EntryHierarchyDisplayMode.EXPANDED,
            (model.uiState.value as InterfaceSettingsUiState.Ready).entryHierarchyDisplayMode)
        assertEquals(emptyList<UiErrorMessage>(), errors)
    }

    @Test
    fun `destroyed page rejects late write failure`() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = Repository {
            withContext(NonCancellable) { release.await() }
            throw IOException("synthetic late failure")
        }
        val model = InterfaceSettingsViewModel(repository, repository)
        model.onPageEntered()
        val errors = mutableListOf<UiErrorMessage>()
        backgroundScope.launch { model.errors.toList(errors) }
        model.onAction(InterfaceSettingsAction.SetHideSystemBars(true))
        runCurrent()
        ViewModelStore().apply { put("interface", model); clear() }
        release.complete(Unit)
        runCurrent()
        assertEquals(emptyList<UiErrorMessage>(), errors)
    }

    private class Repository(private val save: suspend () -> Unit) :
        InterfaceSettingsRepository, LibraryViewSettingsRepository {
        override val interfaceSettings = MutableStateFlow(InterfaceSettings())
        override val libraryViewSettings = MutableStateFlow(
            LibraryViewSettings(EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC)),
        )
        override suspend fun setHideSystemBars(enabled: Boolean) = save()
        override suspend fun setTopBarCollapsible(enabled: Boolean) = save()
        override suspend fun setQuickFilterBarCollapsible(enabled: Boolean) = save()
        override suspend fun setAppCornerRadius(radiusDp: Float) = save()
        override suspend fun setSort(sort: EntrySort) = Unit
        override suspend fun setEntryHierarchyDisplayMode(mode: EntryHierarchyDisplayMode) = save()
    }
}
