package com.aozijx.passly.features.vault.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.features.vault.VaultViewModel

@Composable
fun VaultFab(
    viewModel: VaultViewModel, isVisible: Boolean = true
) {
    var showFabMenu by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showFabMenu) 45f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fabRotation"
    )

    // 如果主按钮被隐藏，自动收起菜单
    if (!isVisible) {
        showFabMenu = false
    }

    // 动画改为缩放 (Scale)，变换中心为右下角 (1f, 1f)
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
            AnimatedVisibility(
                visible = showFabMenu,
                enter = fadeIn() + slideInHorizontally { fullWidth -> fullWidth },
                exit = fadeOut() + slideOutHorizontally { fullWidth -> fullWidth }) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FabMenuItem(
                        label = stringResource(R.string.vault_fab_scan),
                        icon = Icons.Default.QrCodeScanner,
                        onClick = {
                            showFabMenu = false
                            viewModel.addType = AddType.SCAN
                        })
                    FabMenuItem(
                        label = stringResource(R.string.vault_fab_2fa),
                        icon = Icons.Default.Pin,
                        onClick = {
                            showFabMenu = false
                            viewModel.addType = AddType.TOTP
                        })
                    FabMenuItem(
                        label = stringResource(R.string.vault_fab_password),
                        icon = Icons.Default.Key,
                        onClick = {
                            showFabMenu = false
                            viewModel.addType = AddType.PASSWORD
                        })
                }
            }

            // 主 FAB
            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                containerColor = if (showFabMenu) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
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




