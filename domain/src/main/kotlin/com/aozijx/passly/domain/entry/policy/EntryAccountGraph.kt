package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkKind

/** Read-only projection of generic links and endpoint types into ACCOUNT group membership. */
class EntryAccountGraph(
    links: List<EntryLink>,
    entryTypes: Map<EntryId, EntryType>,
) {
    private val memberToAccount: Map<EntryId, EntryId>
    private val otpToLogin: Map<EntryId, EntryId>
    private val recoveryToLogin: Map<EntryId, EntryId>

    init {
        val memberships = mutableMapOf<EntryId, EntryId>()
        val otps = mutableMapOf<EntryId, EntryId>()
        val recoveries = mutableMapOf<EntryId, EntryId>()
        links.forEach { link ->
            val firstType = entryTypes[link.firstEntryId] ?: return@forEach
            val secondType = entryTypes[link.secondEntryId] ?: return@forEach
            when (EntryLinkPolicy.classify(firstType, secondType)) {
                EntryLinkKind.ACCOUNT_MEMBERSHIP -> {
                    val (memberId, accountId) =
                        if (firstType == EntryType.ACCOUNT) {
                            link.secondEntryId to link.firstEntryId
                        } else {
                            link.firstEntryId to link.secondEntryId
                        }
                    memberships[memberId] = accountId
                }

                EntryLinkKind.OTP_ASSOCIATION -> {
                    val (otpId, loginId) =
                        if (firstType == EntryType.OTP) {
                            link.firstEntryId to link.secondEntryId
                        } else {
                            link.secondEntryId to link.firstEntryId
                        }
                    otps[otpId] = loginId
                }

                EntryLinkKind.RECOVERY_ASSOCIATION -> {
                    val (recoveryId, targetId) =
                        if (firstType == EntryType.RECOVERY_CODE) {
                            link.firstEntryId to link.secondEntryId
                        } else {
                            link.secondEntryId to link.firstEntryId
                        }
                    recoveries[recoveryId] = targetId
                }

                EntryLinkKind.RELATED_ENTRIES -> {
                    Unit
                }
            }
        }
        memberToAccount = memberships
        otpToLogin = otps
        recoveryToLogin = recoveries
    }

    fun accountFor(entryId: EntryId): EntryId? =
        memberToAccount[entryId]
            ?: otpToLogin[entryId]?.let(memberToAccount::get)
            ?: recoveryToLogin[entryId]?.let(memberToAccount::get)

    fun membersOf(accountEntryId: EntryId): Set<EntryId> =
        buildSet {
            memberToAccount.forEach { (member, account) ->
                if (account == accountEntryId) add(member)
            }
            otpToLogin.forEach { (otp, login) ->
                if (memberToAccount[login] == accountEntryId) add(otp)
            }
            recoveryToLogin.forEach { (recovery, login) ->
                if (memberToAccount[login] == accountEntryId) add(recovery)
            }
        }
}
