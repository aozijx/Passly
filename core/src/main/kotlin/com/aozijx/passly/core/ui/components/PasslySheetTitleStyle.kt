package com.aozijx.passly.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun passlySheetTitleStyle(): TextStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
