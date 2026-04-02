package com.example.passly.features.detail

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.model.VaultEntry
import com.aozijx.passly.data.repository.VaultRepository
import com.example.passly.data.local.AppPrefs
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VaultRepository(
        AppDatabase.getDatabase(application).vaultDao(),
        AppPrefs(application)
    )

    var entry by mutableStateOf<VaultEntry?>(null)
        private set

    fun toggleFavorite() {
        entry?.let { current ->
            val updated = current.copy(favorite = !current.favorite)
            viewModelScope.launch {
                repository.update(updated)
                entry = updated
            }
        }
    }
}
