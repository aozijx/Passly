package com.example.poop.ui.screens.detail.component

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.poop.ui.navigation.TopBarConfig
import com.example.poop.util.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppWithSdk(
    val packageName: String,
    val appName: String,
    val targetSdk: Int,
    val versionName: String,
    val architecture: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSdkClassifier() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sdkAppMap by remember { mutableStateOf<Map<Int, List<AppWithSdk>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadStatus by remember { mutableStateOf("扫描已安装应用的架构与 SDK") }

    // 1. 配置顶栏
    TopBarConfig(
        title = "应用分析",
        centerTitle = true
    )

    // 2. 页面内容
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "扫描控制台",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(loadStatus, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!isLoading) {
                        Button(
                            onClick = {
                                isLoading = true
                                loadStatus = "正在深度扫描架构信息..."
                                scope.launch {
                                    val result = getAllAppsWithSdk(context)
                                    withContext(Dispatchers.Main) {
                                        sdkAppMap = result.groupBy { it.targetSdk }
                                            .toSortedMap(compareByDescending { it })
                                        isLoading = false
                                        loadStatus = "扫描完成: 共 ${result.size} 个应用"
                                    }
                                }
                            }
                        ) { Text("开始") }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        if (sdkAppMap.isNotEmpty()) {
            sdkAppMap.forEach { (sdkVersion, appList) ->
                item(key = "header_$sdkVersion") {
                    SdkGroupHeader(sdkVersion, appList.size)
                }
                items(appList, key = { "${it.packageName}_$sdkVersion" }) { app ->
                    AppInfoItem(app)
                }
            }
        } else if (!isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "请点击上方开始扫描",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SdkGroupHeader(sdkVersion: Int, count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Android ${getAndroidVersionName(sdkVersion)} (API $sdkVersion)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Badge { Text("$count") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoItem(app: AppWithSdk) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "版本: ${app.versionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                val archLabel = app.architecture
                val is64 = archLabel.contains("64") || archLabel.contains("v8")
                
                val badgeColor = when {
                    is64 -> MaterialTheme.colorScheme.primaryContainer
                    archLabel == "32-bit" || archLabel.contains("v7") -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Badge(containerColor = badgeColor) {
                    Text(
                        text = archLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private suspend fun getAllAppsWithSdk(context: Context): List<AppWithSdk> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)

        packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {

                // ABI 识别逻辑
                val arch = try {
                    val primaryCpuAbi = ApplicationInfo::class.java.getField("primaryCpuAbi")
                        .get(appInfo) as? String

                    when {
                        primaryCpuAbi != null -> {
                            when {
                                primaryCpuAbi.contains("arm64-v8a") -> "arm64-v8a"
                                primaryCpuAbi.contains("armeabi-v7a") -> "armeabi-v7a"
                                primaryCpuAbi.contains("x86_64") -> "x86_64"
                                primaryCpuAbi.contains("x86") -> "x86"
                                primaryCpuAbi.contains("64") -> "64-bit"
                                else -> primaryCpuAbi
                            }
                        }
                        appInfo.nativeLibraryDir.contains("arm64") -> "arm64-v8a"
                        appInfo.nativeLibraryDir.contains("arm") -> "armeabi-v7a"
                        else -> "32-bit"
                    }
                } catch (e: Exception) {
                    Logcat.e("AppSdkClassifier", "识别 ABI 失败: ${pkg.packageName}", e)
                    "Unknown"
                }

                AppWithSdk(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    targetSdk = appInfo.targetSdkVersion,
                    versionName = pkg.versionName ?: "N/A",
                    architecture = arch
                )
            } else null
        }
    }

private fun getAndroidVersionName(sdkVersion: Int): String {
    return when (sdkVersion) {
        in 1..18 -> "Older"
        19 -> "4.4"; 21 -> "5.0"; 22 -> "5.1"; 23 -> "6.0"; 24 -> "7.0"; 25 -> "7.1"; 26 -> "8.0"; 27 -> "8.1"
        28 -> "9"; 29 -> "10"; 30 -> "11"; 31 -> "12"; 32 -> "12L"; 33 -> "13"; 34 -> "14"; 35 -> "15"
        else -> "API $sdkVersion"
    }
}
