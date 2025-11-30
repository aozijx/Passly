package com.example.poop.ui.screens.detail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.poop.model.navItems
import com.example.poop.ui.component.SimpleBottomBar
import com.example.poop.ui.theme.PoopTheme

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopTheme {
                DetailScreen()
            }
        }
    }
}

@Composable
fun DetailScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SimpleBottomBar(navItems, currentActivityClass = DetailActivity::class.java)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "详情页",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
            )
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()

                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(), // 填满Card的大小
                            contentAlignment = Alignment.Center // 内容居中
                        ) {
                            Text(
                                text = "Header",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
                items(20) { index ->
                    Card(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
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
            }
        }
    }
}


