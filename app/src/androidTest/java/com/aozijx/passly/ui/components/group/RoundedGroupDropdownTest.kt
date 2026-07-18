package com.aozijx.passly.ui.components.group

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class RoundedGroupDropdownTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dropdownReadsAndUpdatesExternalState() {
        composeRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf<Boolean?>(null) }

            MaterialTheme {
                RoundedGroup(
                    items = listOf(
                        dropdownSettingsGroupItem(
                            key = "appearance.theme_mode",
                            title = "外观模式",
                            selected = selected,
                            selectedLabel = when (selected) {
                                null -> "跟随系统"
                                false -> "浅色"
                                true -> "深色"
                            },
                            options = listOf(
                                null to "跟随系统",
                                false to "浅色",
                                true to "深色"
                            ),
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            onSelect = { selected = it }
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithText("外观模式").performClick()
        composeRule.onNodeWithText("深色").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("深色").assertIsDisplayed()
    }
}
