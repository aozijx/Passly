package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings

internal data class VaultSettingsSnapshot(
    val interfaceSettings: InterfaceSettings,
    val library: RecoverableReadState<LibraryViewSettings>,
    val interaction: InteractionSettings,
)

internal fun RecoverableReadState<VaultSettingsSnapshot>.toDisplayUiState(): VaultDisplayUiState =
    when (this) {
        RecoverableReadState.Loading -> {
            VaultDisplayUiState.Loading
        }

        is RecoverableReadState.Failed -> {
            VaultDisplayUiState.Failed
        }

        is RecoverableReadState.Ready -> {
            when (val library = value.library) {
                RecoverableReadState.Loading -> {
                    VaultDisplayUiState.Loading
                }

                is RecoverableReadState.Failed -> {
                    VaultDisplayUiState.Failed
                }

                is RecoverableReadState.Ready -> {
                    val settings = value
                    VaultDisplayUiState.Ready(
                        hideSystemBars = settings.interfaceSettings.hideSystemBars,
                        collapseTopBarOnScroll = settings.interfaceSettings.collapseTopBarOnScroll,
                        collapseQuickFilterBarOnScroll = settings.interfaceSettings.collapseQuickFilterBarOnScroll,
                        entryCardPresentations = library.value.entryCardPresentations,
                        isSwipeEnabled = settings.interaction.isSwipeEnabled,
                        isPullToSearchEnabled = settings.interaction.isPullToSearchEnabled,
                        swipeLeftAction = settings.interaction.swipeLeftAction,
                        swipeRightAction = settings.interaction.swipeRightAction,
                        sort = library.value.sort,
                    )
                }
            }
        }
    }
