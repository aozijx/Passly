package com.aozijx.passly.feature.vault.presentation.list.preview

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemCategoryLabel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSecondaryTextUiModel
import com.aozijx.passly.feature.vault.presentation.list.model.VaultPasswordCardSupportingTextUiModel
import com.aozijx.passly.presentation.shared.entry.labelRes
import com.aozijx.passly.presentation.shared.entryvisual.EntryClassificationInput
import com.aozijx.passly.presentation.shared.entryvisual.EntryVisualCategoryClassifier
import com.aozijx.passly.presentation.shared.entryvisual.vaultItemIconUiModel

internal object CardStylePreviewFixtures {
    val otpState =
        OtpUiState.TimedReady(
            currentCode = "123 456",
            nextCode = "654 321",
            progress = 0.4f,
            windowEndMillis = 30_000L,
            periodSeconds = 30,
        )

    fun defaultEntry(entryType: EntryType) =
        previewEntry(
            entryType = entryType,
            id = "-100",
            title = "示例账号",
            username = "demo_user",
            associatedDomain = "example.com",
            otpType =
                if (entryType == EntryType.OTP) {
                    OtpType.TOTP
                } else {
                    null
                },
        )

    fun passwordEntry(entryType: EntryType) =
        previewEntry(
            entryType = entryType,
            id = "-101",
            title = "我的邮箱",
            username = "me@example.com",
            associatedDomain = "example.com",
            hasPassword = true,
        )

    fun totpEntry(entryType: EntryType) =
        previewEntry(
            entryType = entryType,
            id = "-102",
            title = "示例二步验证",
            username = "totp_user",
            otpType = OtpType.TOTP,
        )

    private fun previewEntry(
        entryType: EntryType,
        id: String,
        title: String,
        username: String,
        associatedDomain: String? = null,
        hasPassword: Boolean = false,
        otpType: OtpType? = null,
    ) = VaultListItemUiModel(
        id = id,
        entryType = entryType,
        title = title,
        username = username,
        categoryLabel = VaultListItemCategoryLabel.EntryTypeName(entryType.labelRes),
        favorite = false,
        icon =
            vaultItemIconUiModel(
                iconName = null,
                customImagePath = null,
                associatedAppPackage = null,
                visualCategory =
                    EntryVisualCategoryClassifier.classify(
                        EntryClassificationInput(
                            entryType = entryType,
                            title = title,
                            username = username,
                            urls = setOfNotNull(associatedDomain),
                            domains = setOfNotNull(associatedDomain),
                        ),
                    ),
            ),
        hasPassword = hasPassword,
        otpPresentation = otpType?.let(OtpPresentation::Applicable) ?: OtpPresentation.NotApplicable,
        passwordCardSecondaryText = VaultPasswordCardSecondaryTextUiModel.EntryTypeLabel(entryType.labelRes),
        passwordCardSupportingText = VaultPasswordCardSupportingTextUiModel.PROTECTED,
    )
}
