package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.link.EntryRelationType

/**
 * Defines the legal direction and endpoint types for an entry relation.
 *
 * ACCOUNT is only a grouping target. OTP and recovery-code entries are typed
 * factors that point at the entry they protect; they are never relation targets.
 */
object EntryLinkPolicy {
    fun isAllowed(
        relationType: EntryRelationType,
        sourceType: EntryType,
        targetType: EntryType,
    ): Boolean = when (relationType) {
        EntryRelationType.MEMBER_OF_ACCOUNT ->
            sourceType != EntryType.ACCOUNT && targetType == EntryType.ACCOUNT

        EntryRelationType.OTP_FOR ->
            sourceType == EntryType.OTP && targetType.canReceiveAuthenticationFactor()

        EntryRelationType.RECOVERY_FOR ->
            sourceType == EntryType.RECOVERY_CODE && targetType.canReceiveAuthenticationFactor()

        EntryRelationType.RELATED_TO -> true
    }

    private fun EntryType.canReceiveAuthenticationFactor(): Boolean = when (this) {
        EntryType.ACCOUNT,
        EntryType.OTP,
        EntryType.RECOVERY_CODE,
        -> false

        else -> true
    }
}
