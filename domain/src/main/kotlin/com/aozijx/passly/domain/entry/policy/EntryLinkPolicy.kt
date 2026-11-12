package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLinkKind

/**
 * Infers known link meanings from unordered endpoint types; unclassified pairs remain generic links.
 */
object EntryLinkPolicy {
    fun classify(
        firstType: EntryType,
        secondType: EntryType,
    ): EntryLinkKind =
        when {
            isAccountMembership(firstType, secondType) -> EntryLinkKind.ACCOUNT_MEMBERSHIP
            isOtpAssociation(firstType, secondType) -> EntryLinkKind.OTP_ASSOCIATION
            isRecoveryAssociation(firstType, secondType) -> EntryLinkKind.RECOVERY_ASSOCIATION
            else -> EntryLinkKind.RELATED_ENTRIES
        }

    private fun isAccountMembership(
        firstType: EntryType,
        secondType: EntryType,
    ): Boolean = (firstType == EntryType.ACCOUNT) xor (secondType == EntryType.ACCOUNT)

    private fun isOtpAssociation(
        firstType: EntryType,
        secondType: EntryType,
    ): Boolean =
        (firstType == EntryType.OTP && secondType.canReceiveAuthenticationFactor()) ||
            (secondType == EntryType.OTP && firstType.canReceiveAuthenticationFactor())

    private fun isRecoveryAssociation(
        firstType: EntryType,
        secondType: EntryType,
    ): Boolean =
        (firstType == EntryType.RECOVERY_CODE && secondType.canReceiveAuthenticationFactor()) ||
            (secondType == EntryType.RECOVERY_CODE && firstType.canReceiveAuthenticationFactor())

    private fun EntryType.canReceiveAuthenticationFactor(): Boolean =
        when (this) {
            EntryType.ACCOUNT,
            EntryType.OTP,
            EntryType.RECOVERY_CODE,
            -> false

            else -> true
        }
}
