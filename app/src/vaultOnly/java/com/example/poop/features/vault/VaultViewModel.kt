package com.example.poop.features.vault

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.core.common.AddType
import com.example.poop.core.common.VaultTab
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.local.VaultPrefs
import com.example.poop.data.model.VaultEntry
import com.example.poop.data.repository.VaultRepository
import com.example.poop.features.vault.TotpState
import com.example.poop.core.util.TwoFAUtils
import com.example.poop.core.util.VaultFileUtils
import com.example.poop.util.Logcat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 保险箱主界面业务逻辑
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository by lazy {
        val database = AppDatabase.getDatabase(application)
        VaultRepository(database.vaultDao(), VaultPrefs(application))
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)
    var showTOTPCode by mutableStateOf(true)

    private val _totpStates = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val totpStates: StateFlow<Map<Int, TotpState>> = _totpStates

    var addType by mutableStateOf(AddType.NONE)
    var detailItem by mutableStateOf<VaultEntry?>(null)
    var itemToDelete by mutableStateOf<VaultEntry?>(null)
    var showIconPicker by mutableStateOf(false)

    val availableCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultEntry>> =
        combine(_searchQuery, _selectedCategory) { query, category ->
            Pair(query, category)
        }.flatMapLatest { (query, category) ->
            val baseFlow = when {
                query.isNotEmpty() -> repository.searchEntries(query)
                category != null -> repository.getEntriesByCategory(category)
                else -> repository.allEntries
            }
            baseFlow
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startTotpRefresher()
    }

    private fun startTotpRefresher() {
        viewModelScope.launch {
            while (true) {
                val currentMap = _totpStates.value
                if (currentMap.isNotEmpty()) {
                    val entries = vaultItems.value
                    _totpStates.update { current ->
                        current.mapValues { (id, state) ->
                            val entry = entries.find { it.id == id } ?: return@mapValues state
                            val secret = state.decryptedSecret ?: return@mapValues state
                            val period = entry.totpPeriod.coerceAtLeast(1)
                            val remaining = period - ((System.currentTimeMillis() / 1000) % period)
                            val code = TwoFAUtils.generateTotp(secret, if (entry.totpAlgorithm == "STEAM") 5 else entry.totpDigits, entry.totpPeriod, entry.totpAlgorithm)
                            state.copy(code = code, progress = remaining.toFloat() / period)
                        }
                    }
                }
                delay(500)
            }
        }
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) { onResult(""); return }
        authenticate(activity, "查看详情", "", null) {
            try { onResult(CryptoManager.decrypt(encryptedData)) } catch (e: Exception) { onResult(null) }
        }
    }

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (List<String?>) -> Unit
    ) {
        if (encryptedList.isEmpty()) { onResult(emptyList()); return }
        authenticate(activity, "查看详情", "", null) {
            try {
                val results = encryptedList.map { if (it.isEmpty()) "" else CryptoManager.decrypt(it) }
                onResult(results)
            } catch (e: Exception) {
                onResult(encryptedList.map { null })
            }
        }
    }

    fun ensureTotpUnlocked(
        activity: FragmentActivity, 
        entry: VaultEntry,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit
    ) {
        if (_totpStates.value.containsKey(entry.id)) return
        val encrypted = entry.totpSecret ?: return
        try {
            val decrypted = CryptoManager.decrypt(encrypted)
            unlockTotp(entry, decrypted)
            return
        } catch (_: Exception) {}
        authenticate(activity, "解密验证", entry.title, null) {
            try {
                val decrypted = CryptoManager.decrypt(encrypted)
                unlockTotp(entry, decrypted)
            } catch (e: Exception) {
                Logcat.e("VaultViewModel", "Decrypt failed", e)
            }
        }
    }

    fun showDetail(entry: VaultEntry) { detailItem = entry }
    fun dismissDetail() { detailItem = null }
    fun addItem(entry: VaultEntry) { viewModelScope.launch { repository.insert(entry); addType = AddType.NONE } }
    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            repository.update(entry)
            if (detailItem?.id == entry.id) detailItem = entry
            showIconPicker = false
            _totpStates.update { it - entry.id }
        }
    }
    fun confirmDelete() {
        itemToDelete?.let { entry ->
            viewModelScope.launch {
                entry.iconCustomPath?.let { VaultFileUtils.deleteImage(it) }
                repository.delete(entry)
                itemToDelete = null
                _totpStates.update { it - entry.id }
            }
        }
    }

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }
    fun toggleSearch(active: Boolean) { isSearchActive = active; if (!active) _searchQuery.value = "" }
    fun unlockTotp(entry: VaultEntry, decryptedSecret: String) {
        _totpStates.update { it + (entry.id to TotpState("------", 1f, decryptedSecret)) }
    }
}
