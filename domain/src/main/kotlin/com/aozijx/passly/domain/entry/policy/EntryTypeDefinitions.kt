package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryTypeDefinition
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.entry.policy.type.AccountEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.ApiKeyEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.BankCardEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.CryptoWalletEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.DatabaseCredentialEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.DriverLicenseEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.IdCardEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.LoginEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.NoteEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.OtpEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.PasskeyEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.PassportEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.RecoveryCodeEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.SeedPhraseEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.ServerCredentialEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.SshKeyEntryDefinition
import com.aozijx.passly.domain.entry.policy.type.WifiEntryDefinition

/** Complete domain-owned field catalog used by create and edit flows. */
object EntryTypeDefinitions {
    private val definitions = buildEntryTypeDefinitionIndex(
        definitions = listOf(
            AccountEntryDefinition.definition,
            LoginEntryDefinition.definition,
            NoteEntryDefinition.definition,
            BankCardEntryDefinition.definition,
            IdCardEntryDefinition.definition,
            PassportEntryDefinition.definition,
            DriverLicenseEntryDefinition.definition,
            SshKeyEntryDefinition.definition,
            WifiEntryDefinition.definition,
            PasskeyEntryDefinition.definition,
            OtpEntryDefinition.definition,
            DatabaseCredentialEntryDefinition.definition,
            ServerCredentialEntryDefinition.definition,
            ApiKeyEntryDefinition.definition,
            CryptoWalletEntryDefinition.definition,
            SeedPhraseEntryDefinition.definition,
            RecoveryCodeEntryDefinition.definition,
        ),
        expectedTypes = EntryType.entries.toSet(),
    )

    val all: Collection<EntryTypeDefinition> get() = definitions.values

    operator fun get(type: EntryType): EntryTypeDefinition = definitions.getValue(type)

    fun sensitiveStorageKey(type: EntryType, fieldKey: FieldKey): SensitiveFieldKey? =
        definitions.getValue(type)[fieldKey]?.sensitiveFieldKey
}
