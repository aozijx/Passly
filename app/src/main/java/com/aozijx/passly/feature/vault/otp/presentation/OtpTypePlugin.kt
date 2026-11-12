package com.aozijx.passly.feature.vault.otp.presentation

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions

/** Presentation contributions for each Domain OTP type. */
internal interface OtpTypePlugin {
    val type: OtpType

    @get:StringRes
    val labelResource: Int

    val formatCode: (String) -> String
}

private object TotpPlugin : OtpTypePlugin {
    override val type = OtpType.TOTP

    @StringRes
    override val labelResource = R.string.otp_type_totp
    override val formatCode: (String) -> String = ::groupNumericCode
}

private object HotpPlugin : OtpTypePlugin {
    override val type = OtpType.HOTP

    @StringRes
    override val labelResource = R.string.otp_type_hotp
    override val formatCode: (String) -> String = ::groupNumericCode
}

private object SteamPlugin : OtpTypePlugin {
    override val type = OtpType.STEAM

    @StringRes
    override val labelResource = R.string.otp_type_steam
    override val formatCode: (String) -> String = { it }
}

/** Keeps presentation-specific behavior local while checking complete Domain type coverage. */
internal object OtpTypePlugins {
    private val plugins = listOf(TotpPlugin, HotpPlugin, SteamPlugin)
    private val byType = plugins.associateBy(OtpTypePlugin::type).also { registered ->
        check(registered.size == plugins.size) { "OTP presentation plugins must be unique" }
        check(registered.keys == OtpTypeDefinitions.all.map { it.type }.toSet()) {
            "Every Domain OTP type must have a presentation plugin"
        }
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
