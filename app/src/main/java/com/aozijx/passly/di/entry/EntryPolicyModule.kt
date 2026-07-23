package com.aozijx.passly.di.entry

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.service.entry.BankCardEntryValidator
import com.aozijx.passly.domain.service.entry.DefaultEntryDefaultsProvider
import com.aozijx.passly.domain.service.entry.DefaultEntryFieldReader
import com.aozijx.passly.domain.service.entry.DefaultEntryTypePolicy
import com.aozijx.passly.domain.service.entry.EntryDefaultsProvider
import com.aozijx.passly.domain.service.entry.EntryFieldReader
import com.aozijx.passly.domain.service.entry.EntryTypePolicy
import com.aozijx.passly.domain.service.entry.EntryValidator
import com.aozijx.passly.domain.service.entry.IdCardEntryValidator
import com.aozijx.passly.domain.service.entry.LoginEntryValidator
import com.aozijx.passly.domain.service.entry.PasskeyEntryValidator
import com.aozijx.passly.domain.service.entry.RecoveryCodeEntryValidator
import com.aozijx.passly.domain.service.entry.SeedPhraseEntryValidator
import com.aozijx.passly.domain.service.entry.SshKeyEntryValidator
import com.aozijx.passly.domain.service.entry.TotpEntryValidator
import com.aozijx.passly.domain.service.entry.WiFiEntryValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EntryPolicyModule {

    @Binds
    @Singleton
    abstract fun bindEntryTypePolicy(impl: DefaultEntryTypePolicy): EntryTypePolicy

    @Binds
    @Singleton
    abstract fun bindEntryDefaultsProvider(impl: DefaultEntryDefaultsProvider): EntryDefaultsProvider

    @Binds
    @Singleton
    abstract fun bindEntryFieldReader(impl: DefaultEntryFieldReader): EntryFieldReader

    // -- Validator 多绑定 --

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.LOGIN)
    abstract fun bindLoginValidator(impl: LoginEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.TOTP)
    abstract fun bindTotpValidator(impl: TotpEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.SEED_PHRASE)
    abstract fun bindSeedPhraseValidator(impl: SeedPhraseEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.RECOVERY_CODE)
    abstract fun bindRecoveryCodeValidator(impl: RecoveryCodeEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.PASSKEY)
    abstract fun bindPasskeyValidator(impl: PasskeyEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.SSH_KEY)
    abstract fun bindSshKeyValidator(impl: SshKeyEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.WIFI)
    abstract fun bindWiFiValidator(impl: WiFiEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.BANK_CARD)
    abstract fun bindBankCardValidator(impl: BankCardEntryValidator): EntryValidator

    @Binds
    @IntoMap
    @EntryTypeKey(EntryType.ID_CARD)
    abstract fun bindIdCardValidator(impl: IdCardEntryValidator): EntryValidator

    // DefaultEntryValidator 本身由 @Singleton @Inject 直接注入，不需要额外绑定
}
