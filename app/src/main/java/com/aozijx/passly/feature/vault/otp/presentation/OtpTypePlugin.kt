package com.aozijx.passly.feature.vault.otp.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType

/** Presentation contribution for one Domain OTP subtype. Generation stays in Domain. */
internal sealed interface OtpTypePlugin {
    val type: OtpType
    @get:StringRes val labelResource: Int
    val formatter: OtpCodeFormatter
    val editorPolicy: OtpEditorPolicy
}

internal fun interface OtpCodeFormatter {
    fun format(code: String): String
}

internal sealed interface OtpEditorPolicy {
    data class Timed(
        val defaultDigits: String,
        val defaultPeriod: String,
        val digitsToReset: Set<String>,
        val parameterEditingAllowed: Boolean,
    ) : OtpEditorPolicy

    data class Counter(
        val defaultDigits: String,
        val defaultCounter: String,
        val digitsToReset: Set<String>,
    ) : OtpEditorPolicy

    data class FixedTimed(
        val digits: String,
        val period: String,
        val algorithm: OtpHashAlgorithm,
    ) : OtpEditorPolicy
}

private data object TotpPlugin : OtpTypePlugin {
    override val type = OtpType.TOTP
    @StringRes override val labelResource = R.string.otp_type_totp
    override val formatter = OtpCodeFormatter(::groupNumericCode)
    override val editorPolicy =
        OtpEditorPolicy.Timed(
            defaultDigits = "6",
            defaultPeriod = "30",
            digitsToReset = setOf("5"),
            parameterEditingAllowed = true,
        )
}

private data object HotpPlugin : OtpTypePlugin {
    override val type = OtpType.HOTP
    @StringRes override val labelResource = R.string.otp_type_hotp
    override val formatter = OtpCodeFormatter(::groupNumericCode)
    override val editorPolicy =
        OtpEditorPolicy.Counter(defaultDigits = "6", defaultCounter = "0", digitsToReset = setOf("5"))
}

private data object SteamPlugin : OtpTypePlugin {
    override val type = OtpType.STEAM
    @StringRes override val labelResource = R.string.otp_type_steam
    override val formatter = OtpCodeFormatter { it }
    override val editorPolicy =
        OtpEditorPolicy.FixedTimed(
            digits = "5",
            period = "30",
            algorithm = OtpHashAlgorithm.SHA1,
        )
}

/** Add one plugin when a Domain subtype is added; coverage is checked against the complete Domain type set. */
internal object OtpTypePlugins {
    private val definitions = listOf(TotpPlugin, HotpPlugin, SteamPlugin)
    private val byType = definitions.associateBy(OtpTypePlugin::type).also { plugins ->
        check(plugins.size == definitions.size) { "OTP subtype plugins must be unique" }
        check(plugins.keys == OtpType.entries.toSet()) { "Every Domain OTP subtype must have a feature plugin" }
    }

    fun get(type: OtpType): OtpTypePlugin = byType.getValue(type)
}

internal fun OtpType.plugin(): OtpTypePlugin = OtpTypePlugins.get(this)

private fun groupNumericCode(code: String): String =
    when (code.length) {
        6 -> "${code.take(3)} ${code.drop(3)}"
        8 -> "${code.take(4)} ${code.drop(4)}"
        else -> code.chunked(3).joinToString(" ")
    }
