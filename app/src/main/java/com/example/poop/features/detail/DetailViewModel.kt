package com.example.poop.features.detail

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.local.AppPrefs
import com.example.poop.data.model.VaultEntry
import com.example.poop.data.repository.VaultRepository
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VaultRepository(
        AppDatabase.getDatabase(application).vaultDao(),
        AppPrefs(application)
    )

    var entry by mutableStateOf<VaultEntry?>(null)
        private set

    // 用于显示明文的状态（安全起见，不持久化）
    var revealedUsername by mutableStateOf<String?>(null)
    var revealedPassword by mutableStateOf<String?>(null)

    fun loadEntry(id: Int) {
        viewModelScope.launch {
            // 这里可以从 repository 按 ID 获取，目前假设从外部传入或查询
            // repository.getEntryById(id).collect { entry = it }
        }
    }

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