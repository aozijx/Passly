package com.example.poop.ui.screens.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.poop.model.navItems
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.theme.PoopTheme

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopTheme {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SimpleBottomBar(navItems, currentActivityClass = ProfileActivity::class.java)
        }
    ) { innerPadding ->
        var imageKey by remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "个人页",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
            )
            AsyncImage(
                model = "https://picsum.photos/1920/1080?random=$imageKey", // 网络图片URL
                contentDescription = "网络图片",
                modifier = Modifier
                    .size(400.dp)
                    .clip(RoundedCornerShape(8.dp)),
                // 可选：占位图/错误图
//                key = imageKey,
                placeholder = painterResource(id = android.R.drawable.ic_menu_upload), // 加载中显示
                error = painterResource(id = android.R.drawable.btn_star_big_on) // 加载失败显示
            )
            Button(onClick = {
                imageKey++ // 改变key触发重新请求
            }) {
                Text("换一张图片")
            }
//            Image(
//                bitmap = ImageBitmap.imageResource(id = R.drawable.btn_star_big_on), // 图片资源ID
//                contentDescription = "应用logo", // 无障碍描述
//                modifier = Modifier.size(80.dp) // 设置大小
//            )
        }
    }
}