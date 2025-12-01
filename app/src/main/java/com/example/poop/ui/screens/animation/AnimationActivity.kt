package com.example.poop.ui.screens.animation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poop.ui.component.navigation.navItems
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.theme.PoopTheme

class AnimationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopTheme {
                AnimationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationScreen() {
    Scaffold(
        modifier = Modifier,
        { CenterAlignedTopAppBar({ Text("动画页") }) },
        bottomBar = { SimpleBottomBar(navItems, AnimationActivity::class.java) }) { innerPadding ->
        val modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { FadeSlideAnimationCard(modifier) }
            item { ScaleAnimationCard(modifier) }
            item { RotateAnimationCard(modifier) }
            item { SlideDirectionAnimationCard(modifier) }
            item { StaggeredAnimationCard(modifier) }
            item { InfiniteAnimationCard(modifier) }
            item { HeightExpandAnimationCard(modifier) }
        }
    }

}

// 淡入淡出+垂直滑动（基础款）
@Composable
fun FadeSlideAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { isVisible = !isVisible }) {
                Text("淡入淡出+垂直滑动")
            }
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { it * 2 },
                    animationSpec = tween(500)
                ),
                exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(
                    targetOffsetY = { it * 2 },
                    animationSpec = tween(500)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hello Animation!",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 缩放动画
@Composable
fun ScaleAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { isVisible = !isVisible }) {
                Text("缩放动画")
            }
            AnimatedVisibility(
                visible = isVisible, enter = scaleIn(
                    initialScale = 0.2f, animationSpec = spring(stiffness = Spring.StiffnessLow)
                ), exit = scaleOut(
                    targetScale = 0.2f, animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("缩放效果")
                    }
                }
            }
        }
    }
}

// 旋转动画
@Composable
fun RotateAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        // 用animateFloatAsState实现旋转角度动画
        val rotation by animateFloatAsState(
            targetValue = if (isVisible) 0f else 180f,
            animationSpec = tween(800),
            label = "rotation"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { isVisible = !isVisible }) {
                Text("旋转+透明动画")
            }
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800)),
                exit = fadeOut(animationSpec = tween(800)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "旋转吧！",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .padding(24.dp)
                        .rotate(rotation), // 应用旋转角度
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 不同方向滑动
@Composable
fun SlideDirectionAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var showLeft by remember { mutableStateOf(false) }
        var showRight by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showLeft = !showLeft }) {
                    Text("左侧滑入")
                }
                Button(onClick = { showRight = !showRight }) {
                    Text("右侧滑入")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnimatedVisibility(
                    visible = showLeft, enter = slideInHorizontally(
                        initialOffsetX = { -it }, animationSpec = tween(500)
                    ) + fadeIn(), exit = slideOutHorizontally(
                        targetOffsetX = { -it }, animationSpec = tween(500)
                    ) + fadeOut()
                ) {
                    Text("从左边来", modifier = Modifier.padding(16.dp))
                }
                AnimatedVisibility(
                    visible = showRight, enter = slideInHorizontally(
                        initialOffsetX = { it }, animationSpec = tween(500)
                    ) + fadeIn(), exit = slideOutHorizontally(
                        targetOffsetX = { it }, animationSpec = tween(500)
                    ) + fadeOut()
                ) {
                    Text("从右边来", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

// 交错动画（列表项依次显示）
@Composable
fun StaggeredAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var isVisible by remember { mutableStateOf(false) }
        val items = listOf("第一项", "第二项", "第三项", "第四项")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { isVisible = !isVisible }) {
                Text("交错显示列表")
            }
            Column {
                items.forEachIndexed { index, text ->
                    AnimatedVisibility(
                        visible = isVisible, enter = slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = tween(300, delayMillis = index * 100)
                        ) + fadeIn(animationSpec = tween(300, delayMillis = index * 100)) + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(300, delayMillis = index * 100)
                        ), exit = slideOutVertically(
                            targetOffsetY = { 20 },
                            animationSpec = tween(300, delayMillis = index * 100)
                        ) + fadeOut(
                            animationSpec = tween(
                                300,
                                delayMillis = index * 100
                            )
                        ) + scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(300, delayMillis = index * 100)
                        )
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// 无限循环动画
@Composable
fun InfiniteAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var isRunning by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(
            targetValue = if (isRunning) 360f else 0f, animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart
            ), label = ""
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { isRunning = !isRunning }) {
                Text(if (isRunning) "停止旋转" else "开始旋转")
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(100.dp)
                    .rotate(rotation),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("无限旋转")
                }
            }
        }
    }
}

// 高度展开/折叠动画
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeightExpandAnimationCard(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        var expanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { expanded = !expanded }) {
                Text(if (expanded) "折叠" else "展开")
            }
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(200)
                    ) togetherWith
                            fadeOut(
                                animationSpec = tween(200)
                            ) using SizeTransform(clip = false)
                },
                label = "",
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) { targetExpanded ->
                if (targetExpanded) {
                    Text(
                        text = "这是一段展开的长文本内容\n包含多行文字\n用于展示高度变化的动画效果",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(text = "点击展开更多...", textAlign = TextAlign.Center)
                }
            }
        }
    }
}