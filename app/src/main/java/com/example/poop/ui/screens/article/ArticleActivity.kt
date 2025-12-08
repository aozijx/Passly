package com.example.poop.ui.screens.article

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poop.R
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.component.navigation.navItems
import com.example.poop.ui.theme.PoopTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
fun ArticleScreen() {
    Scaffold(modifier = Modifier.fillMaxSize(), {
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
        SimpleBottomBar(navItems, ArticleActivity::class.java)
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ImageTextCard(
                        "https://picsum.photos/1920/1080",
                        "标题",
                        "null",
                        "描述"
                    )
                }
                item {
                    ImageTextCard(
                        "https://aozijx.github.io/hiner/img/default.avif",
                        "Hello Android",
                        "null",
                        "欢迎使用 Jetpack Compose 创建美丽的应用界面"
                    )
                }
                item {
                    ImageTextCard(
                        "https://aozijx.github.io/hiner/img/default.avif",
                        "Hello Hexo, Solitude, APlayer",
                        "https://aozijx.github.io/hiner/music/",
                        "音乐是生活的调味品，愿你我都能在音乐中找到属于自己的那份宁静与快乐～"
                    )
                }
                item {
                    BottomSheetDemo()
//                    CollapsibleBottomSheetDemo()
                }
                items(10) { index ->
                    Card(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                onClick = { /* 点击事件处理 */ },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true),
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(), // 填满Card的大小
                            contentAlignment = Alignment.Center // 内容居中
                        ) {
                            Text(
                                text = "Item $index",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                item { // 初始化偏移量状态
                    var offsetX by remember {
                        mutableFloatStateOf(0f)
                    }
                    val boxSideLengthDp = 50.dp
                    val boxSideLengthPx = with(LocalDensity.current) {
                        boxSideLengthDp.toPx()
                    }
                    val draggableState = rememberDraggableState {
                        offsetX = (offsetX + it).coerceIn(0f, 3 * boxSideLengthPx)
                    }

                    // 可拖拽的盒子
                    Box(
                        Modifier
                            .width(boxSideLengthDp * 4)
                            .height(boxSideLengthDp)
                            .background(Color.LightGray)
                    ) {
                        Box(
                            Modifier
                                .size(boxSideLengthDp)
                                .offset {
                                    IntOffset(offsetX.roundToInt(), 0)
                                }
                                .draggable(
                                    orientation = Orientation.Horizontal, state = draggableState
                                )
                                .background(Color.DarkGray)

                        )
                    }
                }
                item { TransformerDemo() }
            }
        }
    }
}

@Composable
fun TransformerDemo() {
    val boxSize = 100.dp
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    val transformableState =
        rememberTransformableState { zoomChange: Float, panChange: Offset, rotationChange: Float ->
            scale *= zoomChange
            offset += panChange
            rotationAngle += rotationChange
        }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(boxSize)
                .rotate(rotationAngle) // 需要注意 offset 与 rotate 的调用先后顺序
                .offset {
                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                }
                .scale(scale)
                .background(Color.Green)
                .transformable(
                    state = transformableState, lockRotationOnZoomPan = false
                ))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTextCard(
    cover: String,
    title: String,
    link: String,
    description: String,
    onItemClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier,
        shape = MaterialTheme.shapes.medium,
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图片
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(cover).crossfade(true)
                    .build(),
                contentDescription = "文章图片",
                modifier = Modifier
                    .size(98.dp, 72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
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

            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24),
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
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
    // 使用 rememberCoroutineScope 处理状态变更的协程操作
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val context = LocalContext.current

    Button(onClick = { showBottomSheet = true }) {
        Text("打开 BottomSheet")
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // 异步关闭，避免状态冲突
                scope.launch {
                    if (sheetState.isVisible) {
                        sheetState.hide()
                    }
                }
                showBottomSheet = false
            },
            sheetState = sheetState,
            // 添加拖拽手柄，提升用户体验
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
                    .padding(bottom = 16.dp) // 为安全区域留出空间
                    .navigationBarsPadding() // 考虑导航栏高度
            ) {
                // 使用更语义化的组件
                Text(
                    text = "底部弹窗标题",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 使用可点击的选项
                listOf("选项 1", "选项 2", "选项 3").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // 处理选项点击
                                Toast.makeText(context, "选择了: $option", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
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
                            painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 使用 OutlinedButton 作为次要操作
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
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

