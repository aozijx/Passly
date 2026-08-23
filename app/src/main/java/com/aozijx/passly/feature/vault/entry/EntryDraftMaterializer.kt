package com.aozijx.passly.feature.vault.entry

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryAssociations
import com.aozijx.passly.domain.entry.model.EntryDraft
import com.aozijx.passly.domain.entry.model.EntryDraftValue
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import javax.inject.Inject

class EntryDraftMaterializer @Inject constructor() {
    fun materialize(draft: EntryDraft, identity: EntryIdentity): Entry {
        require(identity.type == draft.target.type) { "Draft and identity types must match" }
        return when (identity.type) {
            EntryType.LOGIN -> materializeLogin(draft, identity)
            EntryType.OTP -> materializeOtp(draft, identity)
            EntryType.BANK_CARD -> materializeBankCard(draft, identity)
            else -> throw IllegalArgumentException("Entry creation is not supported for ${identity.type}")
        }
    }

    private fun materializeLogin(draft: EntryDraft, identity: EntryIdentity) = Entry(
        identity = identity,
        profile = draft.profile(
            username = draft.text(FieldKey.USERNAME).orEmpty(),
            associations = EntryAssociations(
                primaryUrl = draft.text(FieldKey.PRIMARY_URL),
                domains = draft.textList(FieldKey.DOMAINS).toCollection(linkedSetOf()),
                applicationIds = draft.textList(FieldKey.APPLICATION_IDS).toCollection(linkedSetOf()),
            ),
        ),
        secret = EntrySecret(
            credential = LoginCredential(
                email = draft.text(FieldKey.EMAIL),
                password = draft.text(FieldKey.PASSWORD),
            ),
            notes = draft.text(FieldKey.NOTES),
        ),
    )

    private fun materializeOtp(draft: EntryDraft, identity: EntryIdentity): Entry {
        val type = draft.enumValue(FieldKey.OTP_TYPE, OtpType.TOTP)
        val config = OtpConfig(
            type = type,
            secret = draft.text(FieldKey.OTP_SECRET),
            algorithm = draft.enumValue(FieldKey.OTP_ALGORITHM, OtpHashAlgorithm.SHA1),
            digits = draft.number(FieldKey.OTP_DIGITS) ?: if (type == OtpType.STEAM) 5 else 6,
            periodSeconds = if (type == OtpType.HOTP) null else draft.number(FieldKey.OTP_PERIOD) ?: 30,
            counter = if (type == OtpType.HOTP) draft.longNumber(FieldKey.OTP_COUNTER) ?: 0L else null,
            encoding = draft.enumValue(FieldKey.OTP_ENCODING, OtpSecretEncoding.BASE32),
            issuer = draft.text(FieldKey.OTP_ISSUER),
            accountName = draft.text(FieldKey.OTP_ACCOUNT_NAME),
        )
        return Entry(
            identity = identity,
            profile = draft.profile(),
            secret = EntrySecret(
                credential = OtpCredential(config),
                notes = draft.text(FieldKey.NOTES),
            ),
        )
    }

    private fun materializeBankCard(draft: EntryDraft, identity: EntryIdentity) = Entry(
        identity = identity,
        profile = draft.profile(username = draft.text(FieldKey.CARD_HOLDER).orEmpty()),
        secret = EntrySecret(
            credential = CardCredential(
                cardType = draft.text(FieldKey.CARD_TYPE),
                cardNumber = draft.text(FieldKey.CARD_NUMBER),
                cardExpiry = draft.text(FieldKey.CARD_EXPIRATION),
                cardCvv = draft.text(FieldKey.CARD_CVV),
                cardHolder = draft.text(FieldKey.CARD_HOLDER),
                paymentPin = draft.text(FieldKey.PAYMENT_PIN),
                paymentPlatform = draft.text(FieldKey.PAYMENT_PLATFORM),
                billingAddress = draft.text(FieldKey.BILLING_ADDRESS),
            ),
            notes = draft.text(FieldKey.NOTES),
        ),
    )

    private fun EntryDraft.profile(
        username: String = "",
        associations: EntryAssociations = EntryAssociations(),
    ) = EntryProfile(
        title = requireNotNull(text(FieldKey.TITLE)),
        username = username,
        associations = associations,
        tags = textList(FieldKey.TAGS).toCollection(linkedSetOf()),
    )

    private fun EntryDraft.text(key: FieldKey): String? =
        (this[key] as? EntryDraftValue.Text)?.value

    private fun EntryDraft.textList(key: FieldKey): List<String> =
        (this[key] as? EntryDraftValue.TextList)?.values.orEmpty()

    private fun EntryDraft.number(key: FieldKey): Int? =
        (this[key] as? EntryDraftValue.Number)?.value

    private fun EntryDraft.longNumber(key: FieldKey): Long? =
        (this[key] as? EntryDraftValue.LongNumber)?.value

    private inline fun <reified T : Enum<T>> EntryDraft.enumValue(key: FieldKey, default: T): T {
        val raw = text(key) ?: return default
        return enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default
    }
}
