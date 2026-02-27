package com.example.poop.ui.screens.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationScreen(navController: NavController? = null) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Compose 动画精选", fontWeight = FontWeight.Bold) }
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxWidth()

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "基础动画",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            item { FadeSlideAnimationCard(modifier) }
            item { ScaleAnimationCard(modifier) }

            item {
                Text(
                    "高级交互",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }
            item { AnimatedHeartCard(modifier) }
            item { FlipCardAnimation(modifier) }
            item { ShimmerEffectCard(modifier) }

            item {
                Text(
                    "布局变化",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }
            item { HeightExpandAnimationCard(modifier) }
        }
    }
}

// 1. 淡入淡出+垂直滑动
@Composable
fun FadeSlideAnimationCard(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape) // 应用 README 方案：在 clickable 前 clip
            .clickable { },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("可见性动画", style = MaterialTheme.typography.titleSmall)
                Button(onClick = { isVisible = !isVisible }) {
                    Text(if (isVisible) "隐藏" else "显示")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.height(100.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ),
                    exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(200.dp, 80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Hello Compose!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2. 缩放与颜色动画
@Composable
fun ScaleAnimationCard(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .clickable { },
        shape = shape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        var isSelected by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.2f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale"
        )
        val infiniteTransition = rememberInfiniteTransition(label = "color")
        val color by infiniteTransition.animateColor(
            initialValue = MaterialTheme.colorScheme.primary,
            targetValue = MaterialTheme.colorScheme.tertiary,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            ), label = "color"
        )

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = CenterHorizontally
        ) {
            Text(
                "弹性缩放与颜色渐变",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.secondary else color,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { isSelected = !isSelected },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "点击方块体验弹性动画",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// 3. 点赞红心动画
@Composable
fun AnimatedHeartCard(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .clickable { },
        shape = shape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        var liked by remember { mutableStateOf(false) }
        val size by animateDpAsState(
            targetValue = if (liked) 64.dp else 48.dp,
            animationSpec = keyframes {
                durationMillis = 500
                48.dp at 0
                56.dp at 150
                72.dp at 300
                64.dp at 500
            }, label = "heart"
        )

        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = CenterHorizontally) {
            Text(
                "关键帧动画 (Keyframes)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = if (liked) Color(0xFFE91E63) else Color.Gray,
                modifier = Modifier
                    .size(size)
                    .clickable { liked = !liked }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(if (liked) "已点赞!" else "点击点赞", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// 4. 3D翻转卡片
@Composable
fun FlipCardAnimation(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .clickable { },
        shape = shape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        var rotated by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(
            targetValue = if (rotated) 180f else 0f,
            animationSpec = tween(600, easing = FastOutSlowInEasing), label = "flip"
        )

        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = CenterHorizontally) {
            Text(
                "3D 翻转效果",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp, 120.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { rotated = !rotated }
            ) {
                if (rotation <= 90f) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "正面",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "背面",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("点击卡片翻转", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// 5. 骨架屏闪光效果
// 1. 首先创建一个可重用的ShimmerBrush
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 500, translateAnim.value - 500),
        end = Offset(translateAnim.value, translateAnim.value),
        tileMode = TileMode.Mirror
    )
}
@Composable
fun ShimmerEffectCard(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .clickable { },
        shape = shape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("骨架屏加载动画", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush())
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .width(120.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush())
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush())
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "注：完整Shimmer需配合Brush.linearGradient与偏移量使用",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

// 6. 高度展开/折叠
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeightExpandAnimationCard(modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .clickable { },
        shape = shape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        var expanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("内容折叠", style = MaterialTheme.typography.titleSmall)
                Button(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "收起" else "展开")
                }
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(tween(300)) + slideInVertically { -it / 3 } togetherWith
                            fadeOut(tween(300)) + slideOutVertically { -it / 3 } using SizeTransform(
                        clip = true
                    )
                },
                label = "expand"
            ) { targetExpanded ->
                if (targetExpanded) {
                    Text(
                        text = "Jetpack Compose 的动画系统非常强大！\n它可以轻松处理布局变化、颜色渐变和复杂的过渡效果。\n\nAnimatedContent 专门用于在不同内容之间进行动画切换。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "点击展开查看更多详情...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
