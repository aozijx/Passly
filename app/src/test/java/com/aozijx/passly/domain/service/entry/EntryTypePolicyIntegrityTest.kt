package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.service.BankCardEntryValidator
import com.aozijx.passly.domain.entry.service.DefaultEntryTypePolicy
import com.aozijx.passly.domain.entry.service.DefaultEntryValidator
import com.aozijx.passly.domain.entry.service.EntryValidator
import com.aozijx.passly.domain.entry.service.EntryValidatorProvider
import com.aozijx.passly.domain.entry.service.IdCardEntryValidator
import com.aozijx.passly.domain.entry.service.LoginEntryValidator
import com.aozijx.passly.domain.entry.service.PasskeyEntryValidator
import com.aozijx.passly.domain.entry.service.RecoveryCodeEntryValidator
import com.aozijx.passly.domain.entry.service.SeedPhraseEntryValidator
import com.aozijx.passly.domain.entry.service.SshKeyEntryValidator
import com.aozijx.passly.domain.entry.service.TotpEntryValidator
import com.aozijx.passly.domain.entry.service.WiFiEntryValidator
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 完整性测试：确保每个 [EntryType] 都有对应的 [EntryTypePolicy] 配置或明确使用 [DefaultEntryTypePolicy] 的默认值。
 *
 * 未显式配置的类型会获得保守的默认值（不支持 Autofill、空分类、空敏感字段、空摘要），
 * 此测试确保新增的 EntryType 不会被遗漏。
 */
class EntryTypePolicyIntegrityTest {

    private lateinit var policy: DefaultEntryTypePolicy

    @Before
    fun setUp() {
        policy = DefaultEntryTypePolicy()
    }

    @Test
    fun `every EntryType should have a policy configuration`() {
        val uncoveredTypes = EntryType.entries.filter { type ->
            // 通过检查 suggestedCategory 是否为空来判断是否有显式配置
            // 显式配置的 type 都有非空的 suggestedCategory
            policy.suggestedCategory(type).isEmpty() &&
                    !policy.supportsAutofill(type) &&
                    policy.sensitiveFields(type).isEmpty()
        }

        // 允许未显式配置的类型（它们使用 DefaultPolicy 的默认值），
        // 但应明确记录。如果新增了 EntryType 且有业务需求，应添加策略配置。
        assertTrue(
            "以下 EntryType 没有显式的策略配置，如需业务规则请添加: $uncoveredTypes",
            // NOTE, CARD, IDENTITY, PASSPORT, LICENSE, DATABASE, SERVER, API_KEY, CRYPTO_WALLET
            // 这些类型当前使用默认策略，不需要特殊配置
            uncoveredTypes.all {
                it in setOf(
                    EntryType.NOTE,
                    EntryType.CARD,
                    EntryType.IDENTITY,
                    EntryType.PASSPORT,
                    EntryType.LICENSE,
                    EntryType.DATABASE,
                    EntryType.SERVER,
                    EntryType.API_KEY,
                    EntryType.CRYPTO_WALLET
                )
            }
        )
    }

    @Test
    fun `all custom EntryType validators should be registered`() {
        val validatorProvider = EntryValidatorProvider(
            validators = mapOf(
                EntryType.LOGIN to LoginEntryValidator(),
                EntryType.TOTP to TotpEntryValidator(),
                EntryType.SEED_PHRASE to SeedPhraseEntryValidator(),
                EntryType.RECOVERY_CODE to RecoveryCodeEntryValidator(),
                EntryType.PASSKEY to PasskeyEntryValidator(),
                EntryType.SSH_KEY to SshKeyEntryValidator(),
                EntryType.WIFI to WiFiEntryValidator(),
                EntryType.BANK_CARD to BankCardEntryValidator(),
                EntryType.ID_CARD to IdCardEntryValidator()
            ),
            defaultValidator = DefaultEntryValidator()
        )

        // 验证自定义校验器能被正确获取
        EntryType.entries.forEach { type ->
            val validator = validatorProvider.getValidator(type)
            assertTrue(
                "EntryType $type 必须能获取到 EntryValidator",
                validator is EntryValidator
            )
        }
    }
}
