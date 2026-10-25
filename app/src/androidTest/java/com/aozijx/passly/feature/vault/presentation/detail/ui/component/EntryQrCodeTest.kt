package com.aozijx.passly.feature.vault.presentation.detail.ui.component

import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.qr.QrCodeEncoder
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.WifiQrFormatResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadFormatter
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiAction
import com.aozijx.passly.feature.vault.presentation.detail.DetailUiState
import com.aozijx.passly.feature.vault.presentation.detail.section.DetailSectionKey
import com.aozijx.passly.feature.vault.presentation.detail.toDetailPresentationModel
import com.aozijx.passly.feature.vault.presentation.detail.ui.DetailBody
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.WifiParsedResult
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EntryQrCodeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unicodeNetworkCodeDecodesFromRealBitmapWithIndependentWifiParser() {
        val payload = WifiQrPayload("办公室;West\\Wing", "pa:ss;word,\\\"", WifiSecurityType.WpaPersonal, true)
        val formatted = WifiQrPayloadFormatter.format(payload) as WifiQrFormatResult.Formatted
        val bitmap = requireNotNull(QrCodeEncoder.encode(formatted.content))
        val pixels = IntArray(bitmap.width * bitmap.height)
        try {
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val decoded = QRCodeReader().decode(
                BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))),
            )
            val wifi = ResultParser.parseResult(decoded) as WifiParsedResult

            assertEquals(formatted.content, decoded.text)
            assertEquals(payload.ssid, wifi.ssid)
            assertEquals(payload.password, wifi.password)
            assertTrue(wifi.isHidden)
        } finally {
            pixels.fill(0)
            bitmap.eraseColor(Color.WHITE)
            bitmap.recycle()
        }
    }

    @Test
    fun savedWifiDetailExposesConnectionCodeAction() {
        val entry = Entry(
            identity = EntryIdentity(id = EntryId("wifi"), type = EntryType.WIFI, timestamps = EntryTimestamps(1L)),
            profile = EntryProfile(title = "Office"),
            secret = EntrySecret(credential = WifiCredential("Office", "password")),
        )
        val model = requireNotNull(toDetailPresentationModel(
            DetailUiState(entry = entry, sections = listOf(DetailSectionKey.CREDENTIAL)), null,
        ))
        val actions = mutableListOf<DetailUiAction>()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            MaterialTheme {
                DetailBody(model.content, onAction = actions::add, onOpenRelatedEntry = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.vault_show_wifi_qr))
            .assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(listOf(DetailUiAction.ShowEntryQr), actions) }
    }
}
