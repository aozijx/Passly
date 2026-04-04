package com.aozijx.passly.ui.screens.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aozijx.passly.R
import com.aozijx.passly.core.common.ImageType
import com.aozijx.passly.core.media.rememberImagePicker
import com.aozijx.passly.ui.navigation.Screen
import com.aozijx.passly.ui.navigation.TopBarConfig
import com.aozijx.passly.ui.screens.login.LoginActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController? = null, viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 图片选择器
    val pickPhoto = rememberImagePicker { uri, type ->
        viewModel.updateSelectedImage(uri, type)
    }

    // 权限请求启动器：专门用于 Android 10+ 获取图片原始位置信息 (ACCESS_MEDIA_LOCATION)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 权限请求完毕后，无论结果如何都继续打开相册
        // 即使拒绝权限，也不影响选图，只是后续 Exif 解析拿不到 GPS
        pickPhoto(ImageType.COVER)
    }

    // 1. 配置当前页面的顶栏信息
    TopBarConfig(
        title = "个人中心", centerTitle = true,
        actions = {
            IconButton(onClick = {
                navController?.navigate(Screen.Scanner.route)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_qr_code_scanner_24),
                    contentDescription = "扫码",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { navController?.navigate(Screen.Setting.route) }) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )

    // 2. 直接展示内容
    Surface(modifier = Modifier.fillMaxSize()) {
        ProfileContent(
            uiState = uiState,
            onPickAvatar = { pickPhoto(ImageType.AVATAR) },
            onPickCover = {
                // 直接启动权限请求（由系统处理版本兼容性）
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        )
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

        // 图片展示与 Exif 信息
        item {
            if (uiState.coverUri == null) {
                Text(
                    text = "我的相册",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp)
                )
            }
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

        // 展示 Exif 详细信息
        uiState.exifInfo?.let { info ->
            item {
                ExifInfoList(info)
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
fun ExifInfoList(info: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "图片详情 (Exif)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        info.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

