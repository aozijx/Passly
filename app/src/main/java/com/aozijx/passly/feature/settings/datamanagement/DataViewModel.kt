package com.aozijx.passly.feature.settings.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.repository.settings.DeviceRepository
import com.aozijx.passly.domain.repository.settings.PortableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val lastExportFileName: String? = null
)

sealed interface DataUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataUiAction
    data object ClearBackupDirectory : DataUiAction
}

@HiltViewModel
class DataViewModel @Inject constructor(
    private val portableRepository: PortableRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _config = MutableStateFlow(DataUiState())
    val config: StateFlow<DataUiState> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                portableRepository.isAutoDownloadIcons,
                deviceRepository.backupDirectoryUri,
                deviceRepository.lastBackupExportFileName
            ) { adi, bdu, lef ->
                DataUiState(
                    isAutoDownloadIcons = adi,
                    directoryUri = bdu,
                    lastExportFileName = lef
                )
            }.collect { _config.value = it }
        }
    }

    fun onAction(action: DataUiAction) {
        when (action) {
            is DataUiAction.SetAutoDownloadIcons -> viewModelScope.launch {
                portableRepository.setAutoDownloadIcons(action.enabled)
            }

            is DataUiAction.SetBackupDirectoryUri -> viewModelScope.launch {
                deviceRepository.setBackupDirectoryUri(action.uri)
            }

            is DataUiAction.ClearBackupDirectory -> viewModelScope.launch {
                deviceRepository.clearBackupDirectoryUri()
            }
        }
    }
}
