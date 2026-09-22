package com.aozijx.passly.ui.components.group

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.sliderSettingsGroupItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SliderSettingsItemAlignmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sliderIsHorizontallyCenteredWithinSegmentedItem() {
        composeRule.setContent {
            MaterialTheme {
                SegmentedSettingsGroup(
                    items = listOf(
                        sliderSettingsGroupItem(
                            key = "interface.app_corner_radius",
                            icon = Icons.Default.RoundedCorner,
                            title = "应用圆角",
                            subtitle = "调整应用组件的圆角",
                            value = 16f,
                            valueLabel = "16 dp",
                            valueRange = 0f..48f,
                            steps = 47,
                            onValueChange = {},
                            onValueChangeFinished = {},
                        )
                    ),
                    modifier = Modifier.width(360.dp).testTag("settings_group"),
                )
            }
        }

        val groupBounds = composeRule.onNodeWithTag("settings_group")
            .getUnclippedBoundsInRoot()
        val sliderBounds = composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        ).getUnclippedBoundsInRoot()

        val groupCenter = (groupBounds.left + groupBounds.right) / 2
        val sliderCenter = (sliderBounds.left + sliderBounds.right) / 2
        assertEquals(groupCenter.value, sliderCenter.value, 1f)
    }
}
