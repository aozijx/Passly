package com.example.poop.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun PhotoPickerScreen(
    navigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("图片选择器", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = navigateBack) {
            Text("返回")
        }
        // 状态：存储选中的图片 URI
        val selectedImages = remember { mutableStateOf<List<Uri>>(emptyList()) }

        // 创建图片选择器
        val pickMedia = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            // 当用户选择图片后，这个回调会被调用
            uri?.let { selectedUri ->
                // 将新选择的图片添加到列表中（显式创建新列表，避免对不可变列表做就地修改）
                selectedImages.value += selectedUri
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 选择图片按钮
            Button(
                onClick = {
                    // 启动图片选择器
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Text("选择图片")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 显示选中的图片
            if (selectedImages.value.isNotEmpty()) {
                Text(
                    text = "已选择 ${'$'}{selectedImages.value.size} 张图片",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 图片列表
                LazyColumn {
                    items(selectedImages.value) { imageUri ->
                        ImageCard(imageUri = imageUri)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // 没有选择图片时的提示
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有选择图片",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ImageCard(imageUri: Uri) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 显示图片
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = "选择的图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 显示图片路径（可选）
            Text(
                text = "图片路径: ${'$'}{imageUri.toString().take(50)}...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}