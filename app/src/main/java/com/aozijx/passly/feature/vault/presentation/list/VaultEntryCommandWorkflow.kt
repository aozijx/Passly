package com.aozijx.passly.feature.vault.presentation.list

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.feature.vault.copy.SensitiveCopyResult
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.otp.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.trash.TrashCommandResult
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
import com.aozijx.passly.presentation.shared.event.PageEffects
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class VaultEntryCommandWorkflowFactory
    @Inject
    constructor(
        private val secureSessionAccessState: SecureSessionAccessState,
        private val trashCommand: TrashCommandUseCase,
        private val copyEntryField: CopyEntryFieldUseCase,
        private val copyOtpCode: CopyOtpCodeUseCase,
    ) {
        fun create(
            scope: CoroutineScope,
            pageEffects: PageEffects<VaultEffect>,
            onOtpEntryRemoved: (String) -> Unit,
        ) = VaultEntryCommandWorkflow(
            scope = scope,
            secureSessionAccessState = secureSessionAccessState,
            trashCommand = trashCommand,
            copyEntryField = copyEntryField,
            copyOtpCode = copyOtpCode,
            pageEffects = pageEffects,
            onOtpEntryRemoved = onOtpEntryRemoved,
        )
    }

/** Owns Vault list commands whose delivery and side effects belong to one page visit. */
internal class VaultEntryCommandWorkflow(
    private val scope: CoroutineScope,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val trashCommand: TrashCommandUseCase,
    private val copyEntryField: CopyEntryFieldUseCase,
    private val copyOtpCode: CopyOtpCodeUseCase,
    private val pageEffects: PageEffects<VaultEffect>,
    private val onOtpEntryRemoved: (String) -> Unit,
) {
    private val deletingEntryIds = mutableSetOf<EntryId>()

    fun quickDelete(entryId: String) {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_delete_access)) return
        moveToTrash(EntryId(entryId))
    }

    fun copyField(
        entryId: String,
        entryType: EntryType,
        fieldKey: FieldKey,
    ) {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_copy_access)) return
        val owner = pageEffects.currentOwner ?: return
        scope.launch {
            when (
                copyEntryField(
                    EntryId(entryId),
                    entryType,
                    fieldKey,
                    canDeliver = {
                        pageEffects.isActive(owner) && secureSessionAccessState.hasFullSecureSessionAccess()
                    },
                )
            ) {
                SensitiveCopyResult.Copied -> pageEffects.tryEmit(owner, VaultEffect.FieldCopied(fieldKey))

                SensitiveCopyResult.NotAuthorized,
                SensitiveCopyResult.Unavailable,
                -> Unit
            }
        }
    }

    fun copyOtp(entryId: String) {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_copy_otp_access)) return
        val owner = pageEffects.currentOwner ?: return
        scope.launch {
            when (
                copyOtpCode(
                    EntryId(entryId),
                    canDeliver = {
                        pageEffects.isActive(owner) && secureSessionAccessState.hasFullSecureSessionAccess()
                    },
                )
            ) {
                SensitiveCopyResult.Copied -> pageEffects.tryEmit(owner, VaultEffect.OtpCopied)

                SensitiveCopyResult.NotAuthorized,
                SensitiveCopyResult.Unavailable,
                -> Unit
            }
        }
    }

    private fun moveToTrash(entryId: EntryId) {
        val owner = pageEffects.currentOwner ?: return
        if (!deletingEntryIds.add(entryId)) return
        scope.launch {
            try {
                when (val result = trashCommand.moveToTrash(entryId)) {
                    TrashCommandResult.Completed -> {
                        onOtpEntryRemoved(entryId.value)
                    }

                    TrashCommandResult.NotAuthorized -> {
                        Unit
                    }

                    is TrashCommandResult.Failed -> {
                        pageEffects.tryEmit(
                            owner,
                            VaultEffect.ShowError(result.error.toUiErrorMessage(R.string.vault_error_trash_failed)),
                        )
                    }
                }
            } finally {
                deletingEntryIds.remove(entryId)
            }
        }
    }

    private fun ensureFullSecureSessionAccess(
        @StringRes messageRes: Int,
    ): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        pageEffects.tryEmit(VaultEffect.ShowError(UiErrorMessage(messageRes)))
        return false
    }
}
