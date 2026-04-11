package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.detail.page.DetailLaunchMode
import com.aozijx.passly.features.detail.page.DetailOpenRequest

internal class VaultDetailCoordinator {
    private val state = VaultDetailStateHolder()

    val addType: AddType get() = state.addType
    val itemToDelete: VaultEntry? get() = state.itemToDelete
    val coordinatorState: VaultDetailCoordinatorState get() = state.detailCoordinatorState

    fun setAddType(type: AddType) { state.addType = type }
    fun setItemToDelete(entry: VaultEntry?) { state.itemToDelete = entry }

    private fun update(transform: (VaultDetailCoordinatorState) -> VaultDetailCoordinatorState) {
        state.detailCoordinatorState = transform(state.detailCoordinatorState)
    }

    fun showDetail(entry: VaultEntry) {
        update {
            it.copy(
                request = DetailOpenRequest(entry = entry, launchMode = DetailLaunchMode.VIEW),
                isIconPickerVisible = false
            )
        }
    }

    fun showDetailForEdit(entry: VaultEntry) {
        val launchMode = if (entry.totpSecret.isNullOrBlank()) {
            DetailLaunchMode.EDIT_FIELDS
        } else {
            DetailLaunchMode.EDIT_TOTP
        }
        update {
            it.copy(
                request = DetailOpenRequest(entry = entry, launchMode = launchMode),
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
        state.detailCoordinatorState.request?.entry?.id == entryId
}
