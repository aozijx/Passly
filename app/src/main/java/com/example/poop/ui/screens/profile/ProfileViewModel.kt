package com.example.poop.ui.screens.profile

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ImageType {
    AVATAR, COVER, SCREEN
}

data class ProfileStat(
    val count: String,
    val label: String
)

data class ProfileMenuItemData(
    val icon: ImageVector,
    val title: String,
    val id: String
)

data class UserProfile(
    val name: String,
    val bio: String,
    val avatarUrl: String? = null,
    val stats: List<ProfileStat> = emptyList(),
    val menuItems: List<ProfileMenuItemData> = emptyList()
)

data class ProfileUiState(
    val user: UserProfile = UserProfile(name = "", bio = ""),
    val avatarUri: Uri? = null, // 头像 Uri
    val coverUri: Uri? = null,  // 封面 Uri
    val screenUri: Uri? = null,
    val isLoading: Boolean = false
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _uiState.update {
            it.copy(
                user = UserProfile(
                    name = "Android 开发者",
                    bio = "热爱编程，喜欢探索新技术。这是我的个人简介。",
                    stats = listOf(
                        ProfileStat("12", "文章"),
                        ProfileStat("5.2k", "粉丝"),
                        ProfileStat("128", "关注")
                    ),
                    menuItems = listOf(
                        ProfileMenuItemData(Icons.Default.Person, "个人资料", "profile_info"),
                        ProfileMenuItemData(Icons.Default.Edit, "我的文章", "my_articles")
                    )
                )
            )
        }
    }

    fun updateSelectedImage(uri: Uri?, type: ImageType) {
        _uiState.update {
            when (type) {
                ImageType.AVATAR -> it.copy(avatarUri = uri)
                ImageType.COVER -> it.copy(coverUri = uri)
                ImageType.SCREEN -> it.copy(screenUri = uri)
            }
        }
    }
}
