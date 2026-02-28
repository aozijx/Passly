package com.example.poop.ui.screens.home.component

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.poop.util.NotificationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDemo() {
    val showBottomSheet = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // 监听实际状态，并使用稍微快一点的动画避开冲突期
    val isExpanded = sheetState.targetValue == SheetValue.Expanded
    val dynamicSpacing by animateDpAsState(
        targetValue = if (isExpanded) 32.dp else 24.dp,
        animationSpec = spring(
            // 使用弹簧动画来匹配系统的抽屉感，这比 tween 更自然
            dampingRatio = Spring.DampingRatioNoBouncy, // 无回弹
            stiffness = Spring.StiffnessLow             // 较低的刚度，产生顺滑的慢速感
        ),
        label = "spacingAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(onClick = { showBottomSheet.value = true }) {
            Text("更多演示选项")
        }
    }

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet.value = false },
            sheetState = sheetState
        ) {
            BottomSheetContent(
                sheetState = sheetState,
                dynamicSpacing = dynamicSpacing,
                onToggleState = {
                    scope.launch {
                        if (sheetState.targetValue == SheetValue.PartiallyExpanded) {
                            sheetState.expand()
                        } else {
                            sheetState.partialExpand()
                        }
                    }
                },
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet.value = false
                        }
                    }
                }
            )
        }
    }
}

/**
 * 抽离出的 BottomSheet 内部布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(
    sheetState: SheetState,
    dynamicSpacing: Dp,
    onToggleState: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        // 关键点 2：使用 dynamicSpacing，但配合稳定的容器高度
        verticalArrangement = Arrangement.spacedBy(dynamicSpacing)
    ) {
        Text(
            "分段式操作演示",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        StatusBarPopupDemo()
        ReadClipboardButton()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onToggleState,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (sheetState.currentValue == SheetValue.PartiallyExpanded) "切换全开" else "切换半开")
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("关闭")
            }
        }
    }
}

@Composable
fun StatusBarPopupDemo() {
    val context = LocalContext.current
    val notificationHelper = NotificationHelper(context)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notificationHelper.sendCustomNotification(
                        "状态栏弹窗标题", "这是Compose触发的状态栏弹窗内容"
                    )
                } else {
                    Toast.makeText(context, "请在系统设置中开启通知权限", Toast.LENGTH_SHORT).show()
                }
            }) {
            Text(text = "触发状态栏弹窗")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = { notificationHelper.cancelNotification() }) {
            Text(text = "取消弹窗")
        }
    }
}

@Composable
fun ReadClipboardButton() {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var clipboardContent by remember { mutableStateOf("") }
    var buttonText by remember { mutableStateOf("读取剪贴板") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = {
                try {
                    val clipData: ClipData? = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString() ?: ""
                        if (text.isNotEmpty()) {
                            clipboardContent = text
                            buttonText = "读取成功"
                        } else {
                            clipboardContent = ""
                            buttonText = "剪贴板无内容"
                        }
                    } else {
                        clipboardContent = ""
                        buttonText = "未发现剪贴内容"
                    }
                } catch (e: Exception) {
                    clipboardContent = ""
                    buttonText = "读取失败"
                    e.printStackTrace()
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(text = buttonText)
        }

        if (clipboardContent.isNotEmpty()) {
            Text(
                text = "剪贴板内容：$clipboardContent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}
