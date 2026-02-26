package com.example.poop.ui.screens.article

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Article(
    val id: Int,
    val cover: String,
    val title: String,
    val link: String,
    val description: String
)

data class ArticleUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false
)

class ArticleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ArticleUiState())
    val uiState: StateFlow<ArticleUiState> = _uiState.asStateFlow()

    init {
        fetchArticles()
    }

    private fun fetchArticles() {
        // 这里模拟从 Repository 获取数据
        _uiState.value = ArticleUiState(
            articles = listOf(
                Article(
                    1,
                    "https://picsum.photos/1920/1080",
                    "Hello Android",
                    "https://developer.android.com",
                    "欢迎使用 Jetpack Compose 创建美丽的应用界面"
                ),
                Article(
                    2,
                    "https://aozijx.github.io/hiner/img/default.avif",
                    "Hello Hexo",
                    "https://aozijx.github.io/hiner/music/",
                    "音乐是生活的调味品，愿你我都能在音乐中找到属于自己的那份宁静与快乐～"
                )
            ),
            isLoading = false
        )
    }
}
