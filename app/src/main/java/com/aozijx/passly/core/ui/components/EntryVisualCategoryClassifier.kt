package com.aozijx.passly.core.ui.components

import com.aozijx.passly.domain.entry.model.EntryType
import java.net.URI
import java.text.Normalizer

private const val DEFAULT_MINIMUM_RULE_SCORE = 4

internal enum class EntryVisualCategory {
    PERSONAL,
    BANK,
    PAYMENT,
    FINANCE,
    ACCOUNT,
    SOCIAL,
    EMAIL,
    APP,
    GAME,
    VIDEO,
    SHOPPING,
    HEALTH,
    NOTE,
    WORK,
    SCHOOL,
    TRAVEL,
    WIFI,
    SECURITY,
    IDENTITY,
    TECHNICAL,
    WALLET,
}

internal data class EntryClassificationInput(
    val entryType: EntryType,
    val title: String = "",
    val username: String = "",
    val urls: Set<String> = emptySet(),
    val domains: Set<String> = emptySet(),
    val packageNames: Set<String> = emptySet(),
    val appNames: Set<String> = emptySet(),
)

private enum class MatchField {
    TITLE,
    USERNAME,
    URL,
    DOMAIN,
    PACKAGE_NAME,
    APP_NAME,
}

private enum class MatchMode { TOKEN, DOMAIN_SUFFIX, SEGMENT }

private data class CategoryPattern(
    val field: MatchField,
    val mode: MatchMode,
    val terms: Set<String>,
    val score: Int,
)

private data class CategoryRule(
    val category: EntryVisualCategory,
    val minimumScore: Int = DEFAULT_MINIMUM_RULE_SCORE,
    val patterns: List<CategoryPattern>,
)

/**
 * Classifies an entry from typed signals instead of flattening every field into one string.
 *
 * Structured identifiers use boundary-aware matching. Human-readable fields use token matching;
 * username contributes only weak evidence and cannot select a category by itself.
 */
internal object EntryVisualCategoryClassifier {
    fun classify(input: EntryClassificationInput): EntryVisualCategory {
        TYPE_CATEGORIES[input.entryType]?.let { return it }

        val normalized = NormalizedInput(input)
        val best = RULES.asSequence()
            .mapIndexed { index, rule -> ScoredCategory(rule.category, rule.score(normalized), index) }
            .filter { it.score >= DEFAULT_MINIMUM_RULE_SCORE }
            .maxWithOrNull(compareBy<ScoredCategory> { it.score }.thenByDescending { it.ruleIndex })

        return best?.category
            ?: if (input.packageNames.any(String::isNotBlank)) EntryVisualCategory.APP
            else EntryVisualCategory.ACCOUNT
    }

    private fun CategoryRule.score(input: NormalizedInput): Int = patterns.sumOf { pattern ->
        if (pattern.matches(input)) pattern.score else 0
    }.takeIf { it >= minimumScore } ?: 0

    private fun CategoryPattern.matches(input: NormalizedInput): Boolean {
        val values = input.values(field)
        return when (mode) {
            MatchMode.TOKEN -> values.any { value ->
                terms.any { term -> value.hasTokenPhrase(term.normalize()) }
            }

            MatchMode.DOMAIN_SUFFIX -> values.any { value ->
                terms.any { term -> value == term || value.endsWith(".$term") }
            }

            MatchMode.SEGMENT -> values.any { value ->
                val segments = value.split('.', '_', '-')
                terms.any(segments::contains)
            }
        }
    }

    private data class ScoredCategory(
        val category: EntryVisualCategory,
        val score: Int,
        val ruleIndex: Int,
    )

    private class NormalizedInput(input: EntryClassificationInput) {
        private val values = mapOf(
            MatchField.TITLE to setOf(input.title.normalize()),
            MatchField.USERNAME to setOf(input.username.normalize()),
            MatchField.URL to input.urls.mapTo(mutableSetOf()) { it.normalize() },
            MatchField.DOMAIN to buildSet {
                input.domains.mapNotNullTo(this) { it.normalizeDomain() }
                input.urls.mapNotNullTo(this) { it.normalizeDomain() }
            },
            MatchField.PACKAGE_NAME to input.packageNames.mapTo(mutableSetOf()) { it.normalize() },
            MatchField.APP_NAME to input.appNames.mapTo(mutableSetOf()) { it.normalize() },
        )

        fun values(field: MatchField): Set<String> = values.getValue(field).filterTo(mutableSetOf()) {
            it.isNotBlank()
        }
    }

    private fun String.normalize(): String =
        Normalizer.normalize(trim(), Normalizer.Form.NFKC).lowercase()

    private fun String.normalizeDomain(): String? {
        val value = normalize().removeSuffix(".")
        if (value.isBlank()) return null
        return runCatching {
            val uri = if ("://" in value) value else "https://$value"
            URI(uri).host?.lowercase()?.removeSuffix(".")
        }.getOrNull() ?: value.substringBefore('/').substringBefore(':')
    }

    private fun String.hasTokenPhrase(term: String): Boolean {
        if (term.isBlank()) return false
        if (term.any { it.usesCompactScript() }) return contains(term)
        val valueTokens = TOKEN.findAll(this).map(MatchResult::value).toList()
        val termTokens = TOKEN.findAll(term).map(MatchResult::value).toList()
        if (termTokens.isEmpty() || termTokens.size > valueTokens.size) return false
        return valueTokens.windowed(termTokens.size).any { it == termTokens }
    }

    private fun Char.usesCompactScript(): Boolean = when (Character.UnicodeScript.of(code)) {
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA -> true

        else -> false
    }

    private fun textRule(
        category: EntryVisualCategory,
        vararg terms: String,
    ): CategoryRule = CategoryRule(
        category = category,
        patterns = listOf(
            CategoryPattern(MatchField.TITLE, MatchMode.TOKEN, terms.toSet(), TEXT_SCORE),
            CategoryPattern(MatchField.USERNAME, MatchMode.TOKEN, terms.toSet(), USERNAME_SCORE),
            CategoryPattern(MatchField.APP_NAME, MatchMode.TOKEN, terms.toSet(), APP_NAME_SCORE),
            CategoryPattern(MatchField.URL, MatchMode.TOKEN, terms.toSet(), URL_SCORE),
            CategoryPattern(MatchField.DOMAIN, MatchMode.SEGMENT, terms.toSet(), DOMAIN_SCORE),
            CategoryPattern(MatchField.PACKAGE_NAME, MatchMode.SEGMENT, terms.toSet(), PACKAGE_SCORE),
        ),
    )

    private val TYPE_CATEGORIES = mapOf(
        EntryType.NOTE to EntryVisualCategory.NOTE,
        EntryType.BANK_CARD to EntryVisualCategory.BANK,
        EntryType.ID_CARD to EntryVisualCategory.IDENTITY,
        EntryType.PASSPORT to EntryVisualCategory.IDENTITY,
        EntryType.DRIVER_LICENSE to EntryVisualCategory.IDENTITY,
        EntryType.SSH_KEY to EntryVisualCategory.TECHNICAL,
        EntryType.WIFI to EntryVisualCategory.WIFI,
        EntryType.PASSKEY to EntryVisualCategory.SECURITY,
        EntryType.OTP to EntryVisualCategory.SECURITY,
        EntryType.DATABASE_CREDENTIAL to EntryVisualCategory.TECHNICAL,
        EntryType.SERVER_CREDENTIAL to EntryVisualCategory.TECHNICAL,
        EntryType.API_KEY to EntryVisualCategory.TECHNICAL,
        EntryType.CRYPTO_WALLET to EntryVisualCategory.WALLET,
        EntryType.SEED_PHRASE to EntryVisualCategory.WALLET,
        EntryType.RECOVERY_CODE to EntryVisualCategory.SECURITY,
    )

    private val RULES = listOf(
        textRule(EntryVisualCategory.BANK, "bank", "banking", "银行", "銀行", "网银", "ネット銀行"),
        textRule(EntryVisualCategory.PAYMENT, "payment", "payments", "paypal", "alipay", "支付", "支付宝", "決済"),
        textRule(EntryVisualCategory.FINANCE, "finance", "investment", "broker", "理财", "投资", "投資"),
        textRule(EntryVisualCategory.SOCIAL, "social", "chat", "messenger", "wechat", "discord", "社交", "聊天", "微信"),
        textRule(EntryVisualCategory.EMAIL, "email", "mail", "gmail", "outlook", "邮箱", "邮件", "メール"),
        textRule(EntryVisualCategory.GAME, "game", "gaming", "steam", "游戏", "遊戲", "ゲーム"),
        textRule(EntryVisualCategory.VIDEO, "video", "streaming", "youtube", "netflix", "视频", "影视", "動画"),
        textRule(EntryVisualCategory.SHOPPING, "shopping", "shop", "store", "taobao", "amazon", "购物", "淘宝", "買い物"),
        textRule(EntryVisualCategory.HEALTH, "health", "medical", "hospital", "健康", "医疗", "病院"),
        textRule(EntryVisualCategory.WORK, "work", "office", "enterprise", "工作", "办公", "仕事"),
        textRule(EntryVisualCategory.SCHOOL, "school", "education", "campus", "学习", "教育", "学校"),
        textRule(EntryVisualCategory.TRAVEL, "travel", "train", "flight", "airline", "出行", "旅行", "火车"),
        CategoryRule(
            category = EntryVisualCategory.PAYMENT,
            patterns = listOf(
                CategoryPattern(
                    MatchField.DOMAIN,
                    MatchMode.DOMAIN_SUFFIX,
                    setOf("paypal.com", "alipay.com"),
                    STRUCTURED_SCORE,
                ),
                CategoryPattern(
                    MatchField.PACKAGE_NAME,
                    MatchMode.SEGMENT,
                    setOf("paypal", "alipay"),
                    STRUCTURED_SCORE,
                ),
            ),
        ),
    )

    private val TOKEN = Regex("[\\p{L}\\p{N}]+")
    private const val TEXT_SCORE = 4
    private const val USERNAME_SCORE = 1
    private const val APP_NAME_SCORE = 5
    private const val URL_SCORE = 4
    private const val DOMAIN_SCORE = 5
    private const val PACKAGE_SCORE = 5
    private const val STRUCTURED_SCORE = 6
}
