package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.OtpCodeState
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.otp.presentation.OtpTypePlugins
import com.aozijx.passly.feature.vault.otp.presentation.toOtpUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpUiStateMapperTest {
    @Test
    fun everyDomainOtpTypeHasExactlyOnePresentationPlugin() {
        val plugins = OtpType.entries.map(OtpTypePlugins::get)

        assertEquals(OtpType.entries.toSet(), plugins.map { it.type }.toSet())
        assertEquals(plugins.size, plugins.map { it.type }.toSet().size)
    }

    @Test
    fun `maps otp runtime states for vault display`() {
        val timedState =
            OtpCodeState.TimedReady(
                code = "123456",
                nextCode = "654321",
                progress = 0.5f,
                windowEndMillis = 30_000L,
                periodSeconds = 30,
            )

        assertEquals(
            OtpUiState.TimedReady(
                currentCode = "123 456",
                nextCode = "654 321",
                progress = 0.5f,
                windowEndMillis = 30_000L,
                periodSeconds = 30,
            ),
            timedState.toOtpUiState(OtpType.TOTP, showCode = true),
        )
        assertEquals(
            OtpUiState.Pending,
            OtpCodeState.Pending.toOtpUiState(OtpType.TOTP, showCode = true),
        )
        assertEquals(
            OtpUiState.Failed(canRollbackPrevious = false),
            OtpCodeState.Failed(OtpGenerationError.InvalidSecret)
                .toOtpUiState(OtpType.TOTP, showCode = true),
        )
    }

    @Test
    fun `projects hidden inapplicable and missing otp states explicitly`() {
        val state =
            OtpCodeState.TimedReady(
                code = "123456",
                nextCode = "654321",
                progress = 0.5f,
                windowEndMillis = 30_000L,
                periodSeconds = 30,
            )

        assertEquals(
            OtpUiState.Hidden,
            state.toOtpUiState(OtpType.TOTP, showCode = false),
        )
        assertEquals(
            OtpUiState.NotApplicable,
            state.toOtpUiState(OtpPresentation.NotApplicable, showCode = true),
        )
        assertEquals(
            OtpUiState.NotRequested,
            null.toOtpUiState(OtpType.TOTP, showCode = true),
        )
        assertEquals(
            OtpUiState.HotpReady(currentCode = "123 456", counter = 9L, canRollbackPrevious = false),
            OtpCodeState.HotpReady(code = "123456", counter = 9)
                .toOtpUiState(OtpType.HOTP, showCode = true),
        )
    }
}
