package com.aozijx.passly.feature.settings.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.feature.settings.components.RoundedGroup
import com.aozijx.passly.feature.settings.components.RoundedGroupScope

@Composable
fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRoundedGroup(
    modifier: Modifier = Modifier,
    content: RoundedGroupScope.() -> Unit
) {
    RoundedGroup(modifier = modifier, content = content)
}

fun Modifier.sectionSpacing(): Modifier = padding(vertical = 8.dp)
