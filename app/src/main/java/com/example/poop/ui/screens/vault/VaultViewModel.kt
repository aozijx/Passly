package com.example.poop.ui.screens.vault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultCategory
import com.example.poop.data.VaultItem
import com.example.poop.util.CryptoManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()

    // 读取时：将数据库里的密文解密为明文显示
    val vaultItems: StateFlow<List<VaultItem>> = dao.getAllItems()
        .map { items ->
            items.map { it.copy(password = CryptoManager.decrypt(it.password)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 添加时：将明文加密后存入数据库
    fun addItem(title: String, user: String, pass: String, category: VaultCategory) {
        viewModelScope.launch {
            val encryptedPass = CryptoManager.encrypt(pass)
            dao.insert(VaultItem(title = title, username = user, password = encryptedPass, category = category))
        }
    }

    fun deleteItem(item: VaultItem) {
        viewModelScope.launch {
            dao.delete(item)
        }
    }
}
