package com.example.poop.ui.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FeaturedItem(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String
)

data class NewsItem(
    val id: Int,
    val title: String,
    val summary: String,
    val imageUrl: String,
    val isFavorite: Boolean = false
)

data class HomeUiState(
    val userName: String = "开发者",
    val featuredItems: List<FeaturedItem> = emptyList(),
    val newsFeed: List<NewsItem> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        _uiState.update { it.copy(isLoading = true) }
        
        // 模拟从数据源获取
        val featured = listOf(
            FeaturedItem(1, "精选内容 #1", "点击查看更多详情...", "https://picsum.photos/400/300?random=1"),
            FeaturedItem(2, "精选内容 #2", "点击查看更多详情...", "https://picsum.photos/400/300?random=2"),
            FeaturedItem(3, "精选内容 #3", "点击查看更多详情...", "https://picsum.photos/400/300?random=3")
        )

        val news = List(10) { index ->
            NewsItem(
                id = index,
                title = "Jetpack Compose 更新日志 v1.$index",
                summary = "探索最新的 UI 构建工具包特性，提升开发效率...",
                imageUrl = "https://picsum.photos/100/100?random=${index + 100}"
            )
        }

        _uiState.update { 
            it.copy(
                featuredItems = featured,
                newsFeed = news,
                isLoading = false
            )
        }
    }

    fun toggleFavorite(newsId: Int) {
        _uiState.update { state ->
            val updatedFeed = state.newsFeed.map {
                if (it.id == newsId) it.copy(isFavorite = !it.isFavorite) else it
            }
            state.copy(newsFeed = updatedFeed)
        }
    }
}
