package com.example.poop.features.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.MainViewModel
import com.example.poop.core.designsystem.state.VaultEditState
import com.example.poop.core.util.ClipboardUtils
import com.example.poop.data.model.VaultEntry
import com.example.poop.features.vault.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    entry: VaultEntry,
    onBack: () -> Unit,
    activity: FragmentActivity,
    mainViewModel: MainViewModel,
    vaultViewModel: VaultViewModel,
    detailViewModel: DetailViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    // 进入页面时更新交互时间
    LaunchedEffect(Unit) {
        mainViewModel.updateInteraction()
    }

    // 解密状态 - 使用 DisposableEffect 确保离开页面时清除
    var revealedUsername by remember { mutableStateOf<String?>(null) }
    var revealedPassword by remember { mutableStateOf<String?>(null) }

    // 编辑模式状态
    var isEditing by remember { mutableStateOf(false) }
    val editState = remember(entry) { VaultEditState(entry) }

    // 离开页面时清除敏感数据
    DisposableEffect(Unit) {
        onDispose {
            revealedUsername = null
            revealedPassword = null
            // 立即清除剪贴板
            ClipboardUtils.clear(context)
        }
    }

    // 初始化数据
    LaunchedEffect(entry) {
        // detailViewModel.loadEntry(entry.id)
    }

    // 安全地复制到剪贴板，并更新交互时间
    fun safeCopyToClipboard(text: String) {
        mainViewModel.updateInteraction()
        ClipboardUtils.copy(context, text, isSensitive = true)
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { mainViewModel.updateInteraction() }
            ),
        topBar = {
            LargeTopAppBar(
                title = {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editState.editedTitle,
                            onValueChange = { editState.editedTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            singleLine = true
                        )
                    } else {
                        Text(entry.title, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        mainViewModel.updateInteraction()
                        if (isEditing) {
                            isEditing = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, if (isEditing) "取消" else "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = {
                            mainViewModel.updateInteraction()
                            val updatedEntry = editState.applyTo(entry)
                            vaultViewModel.updateVaultEntry(updatedEntry)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, "保存")
                            Spacer(Modifier.width(4.dp))
                            Text("保存")
                        }
                    } else {
                        IconButton(onClick = {
                            mainViewModel.updateInteraction()
                            detailViewModel.toggleFavorite()
                        }) {
                            Icon(
                                if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (entry.favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            mainViewModel.updateInteraction()
                            // 重新初始化编辑状态
                            editState.editedTitle = entry.title
                            editState.editedNotes = entry.notes ?: ""
                            editState.editedDomain = entry.associatedDomain ?: ""
                            editState.editedPackage = entry.associatedAppPackage ?: ""
                            isEditing = true
                        }) {
                            Icon(Icons.Default.Edit, "编辑")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { mainViewModel.updateInteraction() }
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 核心凭据卡片（编辑模式下不显示，因为用户名密码不可直接编辑）
            if (!isEditing) {
                item {
                    InfoGroupCard(title = "登录凭据") {
                        DetailRow(
                            label = "用户名",
                            value = revealedUsername ?: "••••••••",
                            icon = Icons.Default.Person,
                            isSensitive = true,
                            onReveal = {
                                mainViewModel.updateInteraction()
                                if (revealedUsername == null && entry.username.isNotEmpty()) {
                                    vaultViewModel.decryptSingle(activity, entry.username, mainViewModel::authenticate) {
                                        revealedUsername = it
                                    }
                                }
                            },
                            onCopy = {
                                val username = revealedUsername
                                if (username != null) {
                                    safeCopyToClipboard(username)
                                } else if (entry.username.isNotEmpty()) {
                                    vaultViewModel.decryptSingle(activity, entry.username, mainViewModel::authenticate) {
                                        it?.let { 
                                            safeCopyToClipboard(it)
                                            revealedUsername = it
                                        }
                                    }
                                }
                            }
                        )
                        if (entry.password.isNotEmpty()) {
                            DetailRow(
                                label = "密码",
                                value = revealedPassword ?: "••••••••",
                                icon = Icons.Default.Lock,
                                isSensitive = true,
                                onReveal = {
                                    mainViewModel.updateInteraction()
                                    if (revealedPassword == null && entry.password.isNotEmpty()) {
                                        vaultViewModel.decryptSingle(activity, entry.password, mainViewModel::authenticate) {
                                            revealedPassword = it
                                        }
                                    }
                                },
                                onCopy = {
                                    val password = revealedPassword
                                    if (password != null) {
                                        safeCopyToClipboard(password)
                                    } else if (entry.password.isNotEmpty()) {
                                        vaultViewModel.decryptSingle(activity, entry.password, mainViewModel::authenticate) {
                                            it?.let { 
                                                safeCopyToClipboard(it)
                                                revealedPassword = it
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. 关联信息卡片（编辑模式下可编辑）
            item {
                InfoGroupCard(title = "关联应用与网站") {
                    if (isEditing) {
                        EditableDetailRow(
                            label = "网站域名",
                            value = editState.editedDomain,
                            onValueChange = { editState.editedDomain = it },
                            icon = Icons.Default.Language
                        )
                        EditableDetailRow(
                            label = "应用包名",
                            value = editState.editedPackage,
                            onValueChange = { editState.editedPackage = it },
                            icon = Icons.Default.AppShortcut
                        )
                    } else {
                        entry.associatedDomain?.let {
                            DetailRow(
                                label = "网站域名",
                                value = it,
                                icon = Icons.Default.Language,
                                onCopy = { safeCopyToClipboard(it) }
                            )
                        }
                        entry.associatedAppPackage?.let {
                            DetailRow(
                                label = "应用包名",
                                value = it,
                                icon = Icons.Default.AppShortcut,
                                onCopy = { safeCopyToClipboard(it) }
                            )
                        }
                    }
                }
            }

            // 3. 备注卡片（编辑模式下可编辑）
            item {
                InfoGroupCard(title = "备注") {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editState.editedNotes,
                            onValueChange = { editState.editedNotes = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            minLines = 3,
                            maxLines = 10,
                            placeholder = { Text("添加备注...") }
                        )
                    } else {
                        Text(
                            text = entry.notes ?: "暂无备注",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // 4. 元数据信息（仅在非编辑模式显示）
            if (!isEditing) {
                item {
                    MetadataSection(entry)
                }
            }
        }
    }
}

@Composable
fun InfoGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = MaterialTheme.shapes.large,
            content = content
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    isSensitive: Boolean = false,
    onReveal: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Row {
            if (isSensitive && onReveal != null) {
                IconButton(onClick = onReveal) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                }
            }
            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EditableDetailRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun MetadataSection(entry: VaultEntry) {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MetadataText("创建于: ${df.format(Date(entry.createdAt ?: 0))}")
        entry.updatedAt?.let { MetadataText("最后修改: ${df.format(Date(it))}") }
        MetadataText("使用次数: ${entry.usageCount} 次")
    }
}

@Composable
fun MetadataText(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
}
