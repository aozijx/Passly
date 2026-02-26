package com.example.poop.ui.screens.article

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poop.ui.component.IconTitleCard
import com.example.poop.util.NotificationHelper
import kotlinx.coroutines.launch

@Composable
fun ArticleScreen(
    navController: NavHostController? = null,
    viewModel: ArticleViewModel = viewModel()
) {
    if (navController == null) {
        ArticleStandaloneScreen(viewModel)
    } else {
        ArticleContent(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleStandaloneScreen(viewModel: ArticleViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("列表页") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Search, null)
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.MoreVert, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ArticleContent(viewModel)
        }
    }
}

@Composable
private fun ArticleContent(viewModel: ArticleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // 动态渲染 ViewModel 中的文章列表
            items(
                uiState.articles,
                key = { it.id },
                span = { GridItemSpan(maxLineSpan) }) { article ->
                ArticleCard(
                    cover = article.cover,
                    title = article.title,
                    link = article.link,
                    description = article.description,
                    onItemClick = { openUrlInBrowser(context, it) }
                )
            }

            // 静态演示项
            item {
                IconTitleCard(
                    icon = {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    title = "Fancy Border",
                    description = "圆角生成器",
                    link = "https://9elements.github.io/fancy-border-radius/"
                )
            }

            item {
                IconTitleCard(
                    icon = {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://cssgradient.io/icon.svg").crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    title = "CSS Gradient",
                    description = "CSS 渐变色",
                    link = "https://cssgradient.io/"
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) { BottomSheetDemo() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleCard(
    cover: String,
    title: String,
    link: String,
    description: String,
    onItemClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick = { onItemClick(link) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(cover).crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    if (url == "null") return
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDemo() {
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = { showBottomSheet = true }) {
            Text("更多演示选项")
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "操作演示",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                StatusBarPopupDemo()
                Spacer(modifier = Modifier.height(16.dp))
                // 可以在这里增加更多操作
                OutlinedButton(onClick = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showBottomSheet = false }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
fun StatusBarPopupDemo() {
    val context = LocalContext.current
    val notificationHelper = NotificationHelper(context)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 触发弹窗按钮
        Button(
            onClick = {
                // 这里仅做检查，如果用户之前拒绝了，则提示用户
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notificationHelper.sendCustomNotification(
                        "状态栏弹窗标题", "这是Compose触发的状态栏弹窗内容"
                    )
                } else {
                    Toast.makeText(context, "请在系统设置中开启通知权限", Toast.LENGTH_SHORT).show()
                }
            }) {
            Text(text = "触发状态栏弹窗")
        }

        Spacer(modifier = Modifier.width(16.dp)) // 间距

        // 取消弹窗按钮
        Button(
            onClick = { notificationHelper.cancelNotification() }) {
            Text(text = "取消弹窗")
        }
    }
}