package com.example.poop.ui.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.poop.model.AppConfigs
import com.example.poop.model.AppOpenConfig

@Composable
fun rememberAppOpenHandler(config: AppOpenConfig): () -> Unit {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(config.requiredPermission == null) }

    val permissionLauncher = if (config.requiredPermission != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                openTarget(config, context)
            } else {
                Toast.makeText(context, "需要权限才能使用${config.app}", Toast.LENGTH_SHORT).show()
            }
        }
    } else null

    return {
        when {
            config.requiredPermission == null -> openTarget(config, context)
            hasPermission -> openTarget(config, context)
            else -> permissionLauncher?.launch(config.requiredPermission)
        }
    }
}

@Composable
fun AppOpenButton(
    config: AppOpenConfig,
    modifier: Modifier = Modifier
) {
    val onAppOpen = rememberAppOpenHandler(config)

    androidx.compose.material3.Button(
        modifier = modifier,
        onClick = onAppOpen
    ) {
        Text(text = config.app)
    }
}

// 封装打开目标的核心逻辑（私有工具函数）
private fun openTarget(config: AppOpenConfig, context: Context) {
    // 构建 Intent（优化空安全与链式调用）
    val intent: Intent? = when {
        config.intentBuilder != null -> config.intentBuilder.invoke(context)
        config.packageName != null -> context.packageManager.getLaunchIntentForPackage(config.packageName)
        else -> null
    }

    if (intent != null) {
        try {
            // 1. 检查是否有应用能处理该 Intent（避免无匹配应用时崩溃）
            if (intent.resolveActivity(context.packageManager) == null) {
                Toast.makeText(context, config.errorTip, Toast.LENGTH_SHORT).show()
                return
            }

            // 2. 安全添加 Flag（分离调用，避免链式调用潜在问题）
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 3. 启动 Intent
            context.startActivity(intent)
        } catch (e: Exception) {
            // 捕获启动失败异常（如权限不足、系统限制、应用未安装等）
            val errorMsg = config.errorTip
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            e.printStackTrace() // 调试用，正式版可移除或上报日志
        }
    } else {
        // Intent 构建失败时的兜底提示
        Toast.makeText(context, config.errorTip, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun OpenAPP(modifier: Modifier = Modifier) {
    Column(modifier) {
        AppConfigs.appList.forEach { config ->
            AppOpenButton(config = config)
        }
    }
}