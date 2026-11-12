package com.aozijx.passly.presentation.shared.entryvisual

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryVisualCategoryClassifierTest {
    @Test
    fun entryTypeIsStrongerThanTextualHints() {
        val result =
            EntryVisualCategoryClassifier.classify(
                EntryClassificationInput(
                    entryType = EntryType.BANK_CARD,
                    title = "Work account",
                ),
            )

        assertEquals(EntryVisualCategory.BANK, result)
    }

    @Test
    fun everyFixedEntryTypeKeepsItsCategoryWhenTextSuggestsAnotherOne() {
        val expectedCategories =
            mapOf(
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

        expectedCategories.forEach { (entryType, expected) ->
            assertEquals(
                entryType.name,
                expected,
                EntryVisualCategoryClassifier.classify(
                    EntryClassificationInput(
                        entryType = entryType,
                        title = "payment bank social",
                    ),
                ),
            )
        }
    }

    @Test
    fun domainLabelsMatchWithoutSubstringFalsePositives() {
        assertEquals(
            EntryVisualCategory.BANK,
            classifyLogin(domains = setOf("login.bank.example")),
        )
        assertEquals(
            EntryVisualCategory.ACCOUNT,
            classifyLogin(domains = setOf("notbank.example")),
        )
    }

    @Test
    fun packageSegmentsCanClassifyNativeApps() {
        val result = classifyLogin(packageNames = setOf("com.paypal.android.p2pmobile"))

        assertEquals(EntryVisualCategory.PAYMENT, result)
    }

    @Test
    fun appNameAndUrlAreIndependentSignals() {
        assertEquals(
            EntryVisualCategory.SOCIAL,
            classifyLogin(appNames = setOf("Acme Social")),
        )
        assertEquals(
            EntryVisualCategory.VIDEO,
            classifyLogin(urls = setOf("https://example.com/video/account")),
        )
    }

    @Test
    fun compactScriptAppNameCanMatchEmbeddedPhrase() {
        val result = classifyLogin(appNames = setOf("招商银行"))

        assertEquals(EntryVisualCategory.BANK, result)
    }

    @Test
    fun usernameAloneDoesNotChooseAVisualCategory() {
        val result = classifyLogin(username = "bank@example.com")

        assertEquals(EntryVisualCategory.ACCOUNT, result)
    }

    @Test
    fun unknownNativeAppUsesGenericAppCategory() {
        val result = classifyLogin(packageNames = setOf("com.example.product"))

        assertEquals(EntryVisualCategory.APP, result)
    }

    private fun classifyLogin(
        title: String = "Account",
        username: String = "",
        urls: Set<String> = emptySet(),
        domains: Set<String> = emptySet(),
        packageNames: Set<String> = emptySet(),
        appNames: Set<String> = emptySet(),
    ): EntryVisualCategory =
        EntryVisualCategoryClassifier.classify(
            EntryClassificationInput(
                entryType = EntryType.LOGIN,
                title = title,
                username = username,
                urls = urls,
                domains = domains,
                packageNames = packageNames,
                appNames = appNames,
            ),
        )
}
