package com.aozijx.passly.feature.vault.presentation.editor.wifi

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddWifiPresentationProjectionTest {
    @Test
    fun `password field visibility follows selected security type`() {
        assertTrue(AddWifiFormState(securityType = WifiSecurityType.Wep).showsPasswordField)
        assertTrue(AddWifiFormState(securityType = WifiSecurityType.WpaPersonal).showsPasswordField)
        assertFalse(AddWifiFormState(securityType = WifiSecurityType.Open).showsPasswordField)
    }

    @Test
    fun `password feedback hides untouched required input and shows invalid input`() {
        assertEquals(
            WifiPasswordFeedbackUiState.Hidden,
            AddWifiUiState(form = AddWifiFormState()).passwordFeedback,
        )

        assertEquals(
            WifiPasswordFeedbackUiState.Invalid,
            AddWifiUiState(form = AddWifiFormState(ssid = "Office", password = "short")).passwordFeedback,
        )
    }

    @Test
    fun `open network hides password feedback while domain validation still rejects stale password`() {
        val state =
            AddWifiUiState(
                form = AddWifiFormState(ssid = "Guest", securityType = WifiSecurityType.Open, password = "stale"),
            )

        assertFalse(state.canSave)
        assertEquals(WifiPasswordFeedbackUiState.Hidden, state.passwordFeedback)
    }

    @Test
    fun `ssid required violation remains save validation without unreachable inline feedback`() {
        val state = AddWifiUiState(form = AddWifiFormState())

        assertFalse(state.canSave)
        assertEquals(WifiPasswordFeedbackUiState.Hidden, state.passwordFeedback)
    }

    @Test
    fun `security type resource projection covers every domain option`() {
        assertEquals(R.string.wifi_security_open, WifiSecurityType.Open.labelResource())
        assertEquals(R.string.wifi_security_wep, WifiSecurityType.Wep.labelResource())
        assertEquals(R.string.wifi_security_wpa_personal, WifiSecurityType.WpaPersonal.labelResource())
    }
}
