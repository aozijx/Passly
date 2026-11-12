package com.aozijx.passly.feature.vault.otp.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions

/** App-owned presentation contribution. Domain rules remain in OtpTypeDefinitions. */
internal interface OtpTypePlugin {
    val type: OtpType
    @get:StringRes val labelResource: Int
    val formatter: OtpCodeFormatter
    val parameterEditingAllowed: Boolean
    val displayableInVault: Boolean
}

internal fun interface OtpCodeFormatter {
    fun format(code: String): String
}

private data object TotpPlugin : OtpTypePlugin {
    override val type = OtpType.TOTP
    @StringRes override val labelResource = R.string.otp_type_totp
    override val formatter = OtpCodeFormatter(::groupNumericCode)
    override val parameterEditingAllowed = true
    override val displayableInVault = true
}

private data object HotpPlugin : OtpTypePlugin {
    override val type = OtpType.HOTP
    @StringRes override val labelResource = R.string.otp_type_hotp
    override val formatter = OtpCodeFormatter(::groupNumericCode)
    override val parameterEditingAllowed = true
    override val displayableInVault = false
}

private data object SteamPlugin : OtpTypePlugin {
    override val type = OtpType.STEAM
    @StringRes override val labelResource = R.string.otp_type_steam
    override val formatter = OtpCodeFormatter { it }
    override val parameterEditingAllowed = false
    override val displayableInVault = true
}

/** Add one plugin when a Domain subtype is added; coverage is checked against the complete Domain type set. */
internal object OtpTypePlugins {
    private val definitions = listOf(TotpPlugin, HotpPlugin, SteamPlugin)
    private val byType = definitions.associateBy(OtpTypePlugin::type).also { plugins ->
        check(plugins.size == definitions.size) { "OTP subtype plugins must be unique" }
        check(plugins.keys == OtpTypeDefinitions.all.map { it.type }.toSet()) {
            "Every Domain OTP subtype must have a feature plugin"
        }
    }

    fun get(type: OtpType): OtpTypePlugin = byType.getValue(type)
}

internal fun OtpType.plugin(): OtpTypePlugin = OtpTypePlugins.get(this)

internal val OtpType.isDisplayableInVault: Boolean
    get() = plugin().displayableInVault

private fun groupNumericCode(code: String): String =
    when (code.length) {
        6 -> "${code.take(3)} ${code.drop(3)}"
        8 -> "${code.take(4)} ${code.drop(4)}"
        else -> code.chunked(3).joinToString(" ")
    }
