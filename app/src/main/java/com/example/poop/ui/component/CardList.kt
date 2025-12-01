package com.example.poop.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poop.R
import com.example.poop.data.AppConfigs
import com.example.poop.data.AppOpenConfig
import com.example.poop.util.rememberAppIconPainter

@Composable
fun CardList() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 固定2列
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        items(items = AppConfigs.appList, key = { it.app }) { config ->
            AppCard(config, rememberAppOpenHandler(config))
        }
    }
}

@Composable
fun AppCard(config: AppOpenConfig, onAppOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = true,
                onClick = onAppOpen,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row {
                Image(
                    painter = rememberAppIconPainter(
                        packageName = "${config.packageName}",
                        defaultIconResId = R.drawable.ic_launcher_foreground
                    ),
                    contentDescription = "${config.packageName} 图标",
                    modifier = Modifier.size(40.dp)
                )
                Text(text = config.app, fontSize = 24.sp)
            }
        }
    }
}
