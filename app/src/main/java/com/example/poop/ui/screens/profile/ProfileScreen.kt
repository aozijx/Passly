package com.example.poop.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.poop.R
import com.example.poop.ui.navigation.Screen
import com.example.poop.ui.screens.login.LoginActivity
import com.example.poop.util.rememberImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController? = null, viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pickPhoto = rememberImagePicker { uri, type ->
        viewModel.updateSelectedImage(uri, type)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "个人中心", fontWeight = FontWeight.Bold
                    )
                }, actions = {
                    IconButton(onClick = {
                        navController?.navigate(Screen.Scanner.route)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_qr_code_scanner_24),
                            contentDescription = "扫码",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }, windowInsets = WindowInsets(top = 0.dp)
            )
        }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            ProfileContent(
                uiState = uiState,
                onPickAvatar = { pickPhoto(ImageType.AVATAR) },
                onPickCover = { pickPhoto(ImageType.COVER) })
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState, onPickAvatar: () -> Unit, onPickCover: () -> Unit
) {
    val context = LocalContext.current
    val user = uiState.user

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头部个人信息卡片
        item {
            ProfileHeader(
                name = user.name, bio = user.bio, avatarUri = uiState.avatarUri, onLoginClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }, onPickAvatar = onPickAvatar
            )
        }

        // 统计数据栏
        item {
            ProfileStats(user.stats)
        }

        // 功能列表项
        items(user.menuItems) { menuItem ->
            ProfileMenuItem(
                icon = menuItem.icon, title = menuItem.title, onClick = { /* 根据需要处理点击 */ })
        }

        // 图片展示 (相册)
        item {
            Text(
                text = "我的相册",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = uiState.coverUri ?: "https://picsum.photos/1920/1080",
                    contentDescription = "相册图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { onPickCover() },
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = android.R.drawable.ic_menu_upload),
                    error = painterResource(id = android.R.drawable.btn_star_big_on)
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(
    name: String, bio: String, avatarUri: Uri?, onLoginClick: () -> Unit, onPickAvatar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoginClick() }
            .padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "头像",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(
                    onClick = { onPickAvatar() }, modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = 4.dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑头像",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileStats(stats: List<ProfileStat>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEach { stat ->
            StatItem(count = stat.count, label = stat.label)
        }
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = android.R.drawable.arrow_down_float),
            contentDescription = "进入",
            modifier = Modifier
                .size(16.dp)
                .offset(y = 1.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
