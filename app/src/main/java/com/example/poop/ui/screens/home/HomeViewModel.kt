package com.example.poop.ui.screens.home

import androidx.lifecycle.ViewModel
import com.example.poop.R
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

data class Article(
    val id: Int,
    val cover: Any,
    val title: String,
    val link: String,
    val description: String,
    val isFavorite: Boolean = false
)

data class HomeUiState(
    val userName: String = "开发者",
    val featuredItems: List<FeaturedItem> = emptyList(),
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        fetchArticles()
    }

    private fun loadHomeData() {
        _uiState.update { it.copy(isLoading = true) }
        
        val featured = listOf(
            FeaturedItem(1, "精选内容 #1", "点击查看更多详情...", "https://picsum.photos/400/300?random=1"),
            FeaturedItem(2, "精选内容 #2", "点击查看更多详情...", "https://picsum.photos/400/300?random=2"),
            FeaturedItem(3, "精选内容 #3", "点击查看更多详情...", "https://picsum.photos/400/300?random=3")
        )

        _uiState.update { 
            it.copy(
                featuredItems = featured,
                isLoading = false
            )
        }
    }

    fun toggleArticleFavorite(articleId: Int) {
        _uiState.update { state ->
            val updatedArticles = state.articles.map {
                if (it.id == articleId) it.copy(isFavorite = !it.isFavorite) else it
            }
            state.copy(articles = updatedArticles)
        }
    }

    private fun fetchArticles() {
        val articles = listOf(
            Article(
                1,
                "https://picsum.photos/1920/1080",
                "Hello Android",
                "https://developer.android.com",
                "欢迎使用 Jetpack Compose 创建美丽的应用界面"
            ),
            Article(
                2,
                R.drawable.img, // 正确使用资源 ID
                "Hello Hexo",
                "https://aozijx.github.io/hiner/music/",
                "音乐是生活的调味品，愿你我都能在音乐中找到属于自己的那份宁静与快乐～"
            )
        )
        _uiState.update {
            it.copy(articles = articles)
        }
    }
}
