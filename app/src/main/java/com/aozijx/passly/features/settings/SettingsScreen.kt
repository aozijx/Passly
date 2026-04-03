package com.aozijx.passly.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.VaultCardStyle
import com.aozijx.passly.features.settings.components.CardStyleSettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val isSwipeEnabled by viewModel.isSwipeEnabled.collectAsStateWithLifecycle()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsStateWithLifecycle()
    val swipeRightAction by viewModel.swipeRightAction.collectAsStateWithLifecycle()
    
    val isStatusBarAutoHide by viewModel.isStatusBarAutoHide.collectAsStateWithLifecycle()
    val isTopBarCollapsible by viewModel.isTopBarCollapsible.collectAsStateWithLifecycle()
    val isTabBarCollapsible by viewModel.isTabBarCollapsible.collectAsStateWithLifecycle()
    val isSecureContentEnabled by viewModel.isSecureContentEnabled.collectAsStateWithLifecycle()
    val isFlipToLockEnabled by viewModel.isFlipToLockEnabled.collectAsStateWithLifecycle()
    val cardStyle by viewModel.cardStyle.collectAsStateWithLifecycle()

    // 预留扩展：后续新增样式时只需追加到这里
    val availableCardStyles = remember { listOf(VaultCardStyle.BASE) }
    val effectiveCardStyle = if (cardStyle in availableCardStyles) cardStyle else VaultCardStyle.BASE

    LaunchedEffect(cardStyle) {
        if (cardStyle !in availableCardStyles) {
            viewModel.setCardStyle(VaultCardStyle.BASE)
        }
    }
    
    var showLeftActionDialog by remember { mutableStateOf(false) }
    var showRightActionDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SettingsGroupTitle(text = "沉浸式体验")
                SettingsCard {
                    SwitchSettingItem(
                        icon = Icons.Default.Fullscreen,
                        title = "自动隐藏系统状态栏",
                        subtitle = "浏览列表时释放屏幕顶部空间",
                        checked = isStatusBarAutoHide,
                        onCheckedChange = { viewModel.setStatusBarAutoHide(it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SwitchSettingItem(
                        icon = Icons.Default.ViewDay,
                        title = "标题栏跟随滚动",
                        subtitle = "上滑时自动收缩标题以获得更多视野",
                        checked = isTopBarCollapsible,
                        onCheckedChange = { viewModel.setTopBarCollapsible(it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SwitchSettingItem(
                        icon = Icons.Default.SpaceDashboard,
                        title = "分类标签栏跟随滚动",
                        subtitle = "功能分类标签随列表滑动智能隐藏",
                        checked = isTabBarCollapsible,
                        onCheckedChange = { viewModel.setTabBarCollapsible(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                SettingsGroupTitle(text = "安全与隐私")
                SettingsCard {
                    SwitchSettingItem(
                        icon = Icons.Default.Security,
                        title = "高级安全防护",
                        subtitle = "禁止截屏录屏，并隐藏多任务预览内容",
                        checked = isSecureContentEnabled,
                        onCheckedChange = { viewModel.setSecureContentEnabled(it) }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SwitchSettingItem(
                        icon = Icons.Default.Flip,
                        title = "翻转即锁定",
                        subtitle = "手机屏幕朝下放置时立即关闭保险箱",
                        checked = isFlipToLockEnabled,
                        onCheckedChange = { viewModel.setFlipToLockEnabled(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                SettingsGroupTitle(text = "交互习惯")
                SettingsCard {
                    SwitchSettingItem(
                        icon = Icons.Default.Swipe,
                        title = "列表快捷手势",
                        subtitle = "支持条目左右滑动触发快捷操作",
                        checked = isSwipeEnabled,
                        onCheckedChange = { viewModel.setSwipeEnabled(it) }
                    )

                    AnimatedVisibility(
                        visible = isSwipeEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp)
                            ClickableSettingItem(title = "左滑快捷动作", value = swipeLeftAction.displayName, onClick = { showLeftActionDialog = true })
                            HorizontalDivider(Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp)
                            ClickableSettingItem(title = "右滑快捷动作", value = swipeRightAction.displayName, onClick = { showRightActionDialog = true })
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            item {
                SettingsGroupTitle(text = "外观定制")
                SettingsCard {
                    ClickableSettingItem(icon = Icons.Default.Palette, title = "个性化配色", value = "动态取色", onClick = { })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    CardStyleSettingsSection(
                        availableStyles = availableCardStyles,
                        selectedStyle = effectiveCardStyle,
                        onStyleSelected = { viewModel.setCardStyle(it) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showLeftActionDialog) {
        SwipeActionSelectDialog("选择左滑动作", swipeLeftAction, { viewModel.setSwipeLeftAction(it); showLeftActionDialog = false }, { showLeftActionDialog = false })
    }
    if (showRightActionDialog) {
        SwipeActionSelectDialog("选择右滑动作", swipeRightAction, { viewModel.setSwipeRightAction(it); showRightActionDialog = false }, { showRightActionDialog = false })
    }
}

@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        content = content
    )
}

@Composable
fun SwitchSettingItem(icon: ImageVector? = null, title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var localChecked by remember(checked) { mutableStateOf(checked) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { localChecked = !localChecked; onCheckedChange(localChecked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(16.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) { Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = localChecked, onCheckedChange = { localChecked = it; onCheckedChange(it) })
    }
}

@Composable
fun ClickableSettingItem(icon: ImageVector? = null, title: String, value: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(16.dp)) }
        else { Spacer(modifier = Modifier.width(40.dp)) }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        if (value != null) { Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp).padding(start = 4.dp))
    }
}

@Composable
fun SwipeActionSelectDialog(title: String, currentAction: SwipeActionType, onActionSelected: (SwipeActionType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                SwipeActionType.entries.forEach { action ->
                    val isSelected = action == currentAction
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onActionSelected(action) }.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = { onActionSelected(action) })
                        Spacer(modifier = Modifier.width(8.dp))
                        action.icon?.let { icon -> Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(12.dp)) }
                        Text(text = action.displayName, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = RoundedCornerShape(28.dp)
    )
}



