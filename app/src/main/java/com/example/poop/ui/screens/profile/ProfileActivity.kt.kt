package com.example.poop.ui.screens.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.component.navigation.navItems
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { CenterAlignedTopAppBar({ Text(text = "个人页") }) },
        bottomBar = {
            SimpleBottomBar(navItems, ProfileActivity::class.java)
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(3) { index ->
                var imageKey by remember { mutableIntStateOf(0) }
                AsyncImage(
                    model = "https://picsum.photos/1920/1080?random=$imageKey", // 网络图片URL
                    contentDescription = "网络图片${index}",
                    modifier = Modifier
                        .size(400.dp, 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = { imageKey++ }),
                    placeholder = painterResource(id = android.R.drawable.ic_menu_upload), // 加载中显示
                    error = painterResource(id = android.R.drawable.btn_star_big_on) // 加载失败显示
                )
            }
        }
    }
}