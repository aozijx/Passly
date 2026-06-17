package com.aozijx.passly.ui.features.vault.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.ui.features.vault.VaultViewModel
import com.aozijx.passly.ui.features.vault.model.AddType
import kotlinx.coroutines.delay

@Composable
fun VaultFab(
    viewModel: VaultViewModel,
    isVisible: Boolean = true
) {
    var showFabMenu by remember { mutableStateOf(false) }

    // 为 FAB 旋转添加阻尼动画 (Spring)
    val rotation by animateFloatAsState(
        targetValue = if (showFabMenu) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabRotation"
    )

    // 用于实现依次弹出的状态控制
    var item1Visible by remember { mutableStateOf(false) }
    var item2Visible by remember { mutableStateOf(false) }
    var item3Visible by remember { mutableStateOf(false) }

    // 监听 showFabMenu 变化，手动控制延迟实现交错效果
    LaunchedEffect(showFabMenu) {
        if (showFabMenu) {
            item3Visible = true
            delay(60)
            item2Visible = true
            delay(60)
            item1Visible = true
        } else {
            item1Visible = false
            item2Visible = false
            item3Visible = false
        }
    }

    if (!isVisible) {
        showFabMenu = false
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
        exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(1f, 1f))
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp, end = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 扫描 (最上方)
                FabMenuItemWithSpring(
                    visible = item1Visible,
                    label = stringResource(R.string.vault_fab_scan),
                    icon = Icons.Default.QrCodeScanner,
                    onClick = {
                        showFabMenu = false
                        viewModel.setAddType(AddType.SCAN)
                    }
                )
                // 2FA (中间)
                FabMenuItemWithSpring(
                    visible = item2Visible,
                    label = stringResource(R.string.vault_fab_2fa),
                    icon = Icons.Default.Pin,
                    onClick = {
                        showFabMenu = false
                        viewModel.setAddType(AddType.TOTP)
                    }
                )
                // 密码 (最下方)
                FabMenuItemWithSpring(
                    visible = item3Visible,
                    label = stringResource(R.string.vault_fab_password),
                    icon = Icons.Default.Key,
                    onClick = {
                        showFabMenu = false
                        viewModel.setAddType(AddType.PASSWORD)
                    }
                )
            }

            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                containerColor = if (showFabMenu) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add),
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
fun FabMenuItemWithSpring(
    visible: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { it / 2 } +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        FabMenuItem(label = label, icon = icon, onClick = onClick)
    }
}

@Composable
fun FabMenuItem(
    label: String, icon: ImageVector, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}