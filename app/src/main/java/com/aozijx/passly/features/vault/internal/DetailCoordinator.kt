package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.detail.page.DetailLaunchMode
import com.aozijx.passly.features.detail.page.DetailOpenRequest
import com.aozijx.passly.features.vault.model.AddType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class DetailCoordinator {
    private val state = DetailState()
    private val _coordinatorState = MutableStateFlow(VaultDetailCoordinatorState())
    val coordinatorStateFlow: StateFlow<VaultDetailCoordinatorState> = _coordinatorState

    val addType: AddType? get() = state.addType
    val itemToDelete: VaultEntry? get() = state.itemToDelete
    val coordinatorState: VaultDetailCoordinatorState get() = _coordinatorState.value

    fun setAddType(type: AddType?) { state.addType = type }
    fun setItemToDelete(entry: VaultEntry?) { state.itemToDelete = entry }

    private fun update(transform: (VaultDetailCoordinatorState) -> VaultDetailCoordinatorState) {
        val next = transform(_coordinatorState.value)
        _coordinatorState.value = next
        state.detailCoordinatorState = next
    }

    fun showDetail(entry: VaultEntry) {
        update {
            it.copy(
                request = DetailOpenRequest(entry = entry, launchMode = DetailLaunchMode.VIEW),
                isIconPickerVisible = false
            )
        }
    }

    fun dismissDetail() = update { it.copy(request = null, isIconPickerVisible = false) }
    fun showIconPicker() = update { it.copy(isIconPickerVisible = true) }
    fun hideIconPicker() = update { it.copy(isIconPickerVisible = false) }

    fun updateEntry(entry: VaultEntry) {
        update { current ->
            val request = current.request
            if (request?.entry?.id == entry.id) {
                current.copy(request = request.copy(entry = entry), isIconPickerVisible = false)
            } else {
                current.copy(isIconPickerVisible = false)
            }
        }
    }

    fun isViewingEntry(entryId: Int): Boolean =
        _coordinatorState.value.request?.entry?.id == entryId
}