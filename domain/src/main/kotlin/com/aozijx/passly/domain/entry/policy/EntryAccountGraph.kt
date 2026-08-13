package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType

/** Read-only projection of typed links into ACCOUNT group membership. */
class EntryAccountGraph(links: List<EntryLink>) {
    private val memberToAccount = links
        .asSequence()
        .filter { it.relationType == EntryRelationType.MEMBER_OF_ACCOUNT }
        .associate { it.sourceEntryId to it.targetEntryId }

    private val otpToLogin = links
        .asSequence()
        .filter { it.relationType == EntryRelationType.OTP_FOR }
        .associate { it.sourceEntryId to it.targetEntryId }

    private val recoveryToAccount = links
        .asSequence()
        .filter { it.relationType == EntryRelationType.RECOVERY_FOR }
        .associate { it.sourceEntryId to it.targetEntryId }

    fun accountFor(entryId: EntryId): EntryId? =
        memberToAccount[entryId]
            ?: recoveryToAccount[entryId]
            ?: otpToLogin[entryId]?.let(memberToAccount::get)

    fun membersOf(accountEntryId: EntryId): Set<EntryId> = buildSet {
        memberToAccount.forEach { (member, account) ->
            if (account == accountEntryId) add(member)
        }
        recoveryToAccount.forEach { (recovery, account) ->
            if (account == accountEntryId) add(recovery)
        }
        otpToLogin.forEach { (otp, login) ->
            if (memberToAccount[login] == accountEntryId) add(otp)
        }
    }
}
