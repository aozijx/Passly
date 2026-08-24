package com.aozijx.passly.presentation.ui.vault.list.model

data class VaultListItemUiModel(
    val id: String,
    val entryType: VaultEntryTypeUiModel,
    val title: String,
    val username: String,
    val category: String?,
    val favorite: Boolean,
    val associatedDomain: String?,
    val associatedAppPackage: String?,
    val iconCustomPath: String?,
    val hasPassword: Boolean,
    val hasOtp: Boolean,
    val otpKind: VaultOtpKindUiModel?,
    val otpPreview: String?,
)

enum class VaultEntryTypeUiModel {
    ACCOUNT, LOGIN, NOTE, BANK_CARD, ID_CARD, PASSPORT, DRIVER_LICENSE,
    SSH_KEY, WIFI, PASSKEY, OTP, DATABASE_CREDENTIAL, SERVER_CREDENTIAL,
    API_KEY, CRYPTO_WALLET, SEED_PHRASE, RECOVERY_CODE,
}

enum class VaultOtpKindUiModel { STANDARD, STEAM }

data class VaultOtpUiState(
    val code: String? = null,
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

enum class VaultQuickFilterUiModel { ALL, PASSWORDS, TOTP }

enum class VaultSortUiModel { DEFAULT, TITLE, CREATED_AT, UPDATED_AT, USAGE_FREQUENCY }

enum class VaultSwipeActionUiModel { DELETE, DETAIL, COPY_PASSWORD, COPY_USERNAME }

enum class VaultAddTypeUiModel {
    PASSWORD, TOTP, BANK_CARD, WIFI, SSH_KEY, ID_CARD, SEED_PHRASE, PASSKEY, RECOVERY_CODE;

    companion object {
        val fabMenuOptions = listOf(TOTP, PASSWORD)
        val allOptions = entries.toList()
    }
}

data class VaultCardPresentationUiModel(
    val entryTypeKey: String,
    val variantKey: String,
    val density: VaultCardDensityUiModel,
    val showIcon: Boolean,
    val showFavorite: Boolean,
    val showSecondaryText: Boolean,
    val showQuickAction: Boolean,
)

enum class VaultCardDensityUiModel { COMPACT, STANDARD, COMFORTABLE }
