package com.example.poop.ui.screens.article

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poop.data.BottomNavItem
import com.example.poop.ui.component.IconTitleCard
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.theme.PoopTheme
import com.example.poop.util.NotificationHelper
import kotlinx.coroutines.launch

class ArticleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopTheme {
                ArticleScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(navController: NavHostController? = null) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        CenterAlignedTopAppBar(title = {
            Text("列表页")
        }, navigationIcon = {
            IconButton(onClick = { } //do something
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        }, actions = {
            IconButton(onClick = { } //do something
            ) {
                Icon(Icons.Filled.Search, null)
            }
            IconButton(onClick = { } //do something
            ) {
                Icon(Icons.Filled.MoreVert, null)
            }
        })
    }, bottomBar = {
        SimpleBottomBar(activityClass = ArticleActivity::class.java)
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ArticleCard(
                        "https://picsum.photos/1920/1080",
                        "Hello Android",
                        "null",
                        "欢迎使用 Jetpack Compose 创建美丽的应用界面"

                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ArticleCard(
                        "https://aozijx.github.io/hiner/img/default.avif",
                        "Hello Hexo, Solitude, APlayer",
                        "https://aozijx.github.io/hiner/music/",
                        "音乐是生活的调味品，愿你我都能在音乐中找到属于自己的那份宁静与快乐～"
                    )
                }
                item {
                    IconTitleCard(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
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
                                    .data("https://cssgradient.io/icon.svg").crossfade(true)
                                    .build(),
                                contentDescription = "文章图片",
                                modifier = Modifier
                                    .size(40.dp) // 匹配 IconTitleCard 内部尺寸
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        },
                        title = "CSS Gradient",
                        description = "CSS 渐变色",
                        link = "https://cssgradient.io/"
                    )
                }
                item {
                    IconTitleCard(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = "用户信息",
                        description = "查看和编辑您的个人资料",
                        // link = null // 不传 link 则不可点击跳转
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) { BottomSheetDemo() }
                item(span = { GridItemSpan(maxLineSpan) }) { StatusBarPopupDemo() }
            }
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
    onItemClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier,
        shape = MaterialTheme.shapes.large, // 圆润的圆角
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // 统一边框风格
        elevation = CardDefaults.cardElevation(0.dp),
        onClick = { // 点击事件
            if (onItemClick != null) {
                onItemClick(link)
            } else {
                openUrlInBrowser(context, link)
            }
        }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // 增加内边距
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图片
            AsyncImage(
                model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                contentDescription = "文章图片",
                modifier = Modifier
                    .size(80.dp, 60.dp) // 稍微调整比例
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, // 统一使用箭头
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "未找到浏览器应用", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        Log.e("ImageTextCard", "打开链接失败: $url", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDemo() {
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { showBottomSheet = true }) {
            Text("打开 BottomSheet")
        }
    }

    // 监听 sheetState 变化，同步到 showBottomSheet
    LaunchedEffect(sheetState.isVisible) {
        // 当 sheetState 变为不可见且动画结束时，更新 showBottomSheet
        if (!sheetState.isVisible) {
            showBottomSheet = false
        }
    }

    // 始终声明 ModalBottomSheet，但用 showBottomSheet 控制其显示
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // 触发关闭动画
                scope.launch {
                    sheetState.hide()
                }
            },
            sheetState = sheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "底部弹窗标题",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                listOf("选项 1", "选项 2", "选项 3").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "选择了: $option", Toast.LENGTH_SHORT)
                                    .show()
                                scope.launch {
                                    sheetState.hide()
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
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

    // 直接使用 Column 作为主容器
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

        Spacer(modifier = Modifier.height(16.dp)) // 间距

        // 取消弹窗按钮
        Button(
            onClick = { notificationHelper.cancelNotification() }) {
            Text(text = "取消弹窗")
        }
    }
}
