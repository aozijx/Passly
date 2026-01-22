package com.example.poop.ui.screens.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.poop.ui.component.CardList


@Composable
fun MyTest() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部的 Greeting
        Greeting(
            name = "Android",
            modifier = Modifier.fillMaxWidth()
        )

        // 中间的测试组件
        TestComposable()

        // 使用 weight(1f) 让 CardList 占据剩余的所有垂直空间
        // 这样 LazyVerticalGrid 就能得到一个确定的高度，不再崩溃
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CardList()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
        textAlign = TextAlign.Center
    )

}

@Composable
fun TestComposable() {
    // 定义状态：控制弹窗是否显示（默认隐藏）
    val isDialogShow = remember { mutableStateOf(false) }
    Button(
        onClick = { isDialogShow.value = true } // 点击事件： 状态设为 true → 弹窗显示
    ) {
        Text(text = "我和CSS里的flex居中效果一样")
    }
    // 根据状态显示弹窗
    if (isDialogShow.value) {
        MyAlert(
            onDismiss = { isDialogShow.value = false }
        )
    }
}

@Composable
private fun MyAlert(onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = "提示") },
        text = { Text(text = "这是 Compose 的 Alert 弹窗，和 CSS alert() 功能一样！") },
        // 确认按钮（点击后关闭弹窗）
        confirmButton = {
            TextButton(
                onClick = onDismiss  // 状态设为 false → 弹窗关闭
            ) {
                Text(text = "确定")
            }
        },
        // 取消按钮（可选，点击后也关闭弹窗）
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = "取消")
            }
        },
        // 点击弹窗外部 → 关闭弹窗（可选，类似浏览器 alert() 点击外部不关闭，这里可自定义）
        onDismissRequest = onDismiss
    )
}