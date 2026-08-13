package com.aozijx.passly.core.ui.components

import com.aozijx.passly.domain.entry.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryVisualCategoryClassifierTest {
    @Test
    fun entryTypeIsStrongerThanTextualHints() {
        val result = EntryVisualCategoryClassifier.classify(
            EntryClassificationInput(
                entryType = EntryType.BANK_CARD,
                title = "Work account",
            ),
        )

        assertEquals(EntryVisualCategory.BANK, result)
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
    ): EntryVisualCategory = EntryVisualCategoryClassifier.classify(
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
