package com.aozijx.passly.feature.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessageSettingsUiState(
    val showGeneral: Boolean = true,
    val showIconDownloads: Boolean = true,
    val showClipboardClears: Boolean = true,
    val showAppClose: Boolean = true
)

@HiltViewModel
class MessageSettingsViewModel @Inject constructor(
    private val settings: PortableSettingsUseCases
) : ViewModel() {
    val uiState: StateFlow<MessageSettingsUiState> = combine(
        settings.showGeneralMessages,
        settings.showIconDownloadMessages,
        settings.showClipboardClearMessages,
        settings.showAppCloseMessages
    ) { general, icon, clipboard, close ->
        MessageSettingsUiState(general, icon, clipboard, close)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MessageSettingsUiState()
    )

    fun setShowGeneral(enabled: Boolean) = update { settings.setShowGeneralMessages(enabled) }
    fun setShowIconDownloads(enabled: Boolean) =
        update { settings.setShowIconDownloadMessages(enabled) }
    fun setShowClipboardClears(enabled: Boolean) =
        update { settings.setShowClipboardClearMessages(enabled) }
    fun setShowAppClose(enabled: Boolean) = update { settings.setShowAppCloseMessages(enabled) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
