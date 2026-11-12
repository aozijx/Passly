package com.aozijx.passly.presentation.shared.components.group.model

import org.junit.Test

class SettingsItemValueTest {
    @Test(expected = IllegalArgumentException::class)
    fun `text value cannot be blank`() {
        SettingsItemValue.Text(" ")
    }
}
