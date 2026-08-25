package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryPolicyTest {
    @Test
    fun presentationUiRejectsExactForbiddenImportPrefixes() {
        val cases = listOf(
            "com.aozijx.passly.presentation.feature.vault.list.VaultUiState",
            "com.aozijx.passly.feature.vault.model.AddType",
            "com.aozijx.passly.domain.entry.model.Entry",
            "com.aozijx.passly.data.repository.EntryRepositoryImpl",
        )

        cases.forEach { importedType ->
            val violations = SourceBoundaryVerifier.verify(
                sources = listOf(uiSource("import $importedType")),
                rules = SourceBoundaryPolicy.generalRules,
            )

            assertEquals(1, violations.size, importedType)
            assertEquals("PRESENTATION_UI_IMPORT", violations.single().ruleId)
            assertEquals("import $importedType", violations.single().evidence)
        }
    }

    @Test
    fun presentationUiRejectsViewModelLookupWithExactEvidence() {
        val violations = SourceBoundaryVerifier.verify(
            sources = listOf(uiSource("val owner = hiltViewModel<VaultViewModel>()")),
            rules = SourceBoundaryPolicy.generalRules,
        )

        assertEquals(1, violations.size)
        assertEquals("PRESENTATION_UI_VIEW_MODEL", violations.single().ruleId)
        assertEquals("val owner = hiltViewModel<VaultViewModel>()", violations.single().evidence)
    }

    @Test
    fun commentsAndSimilarlyNamedPackagesDoNotMatchImportRules() {
        val violations = SourceBoundaryVerifier.verify(
            sources = listOf(
                uiSource(
                    """
                    // import com.aozijx.passly.data.repository.Hidden
                    import com.aozijx.passly.database.preview.EntryPreview
                    import com.aozijx.passly.core.ui.theme.PasslyTheme
                    """.trimIndent(),
                ),
            ),
            rules = SourceBoundaryPolicy.generalRules,
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun formattedViolationIncludesRulePathAndEvidence() {
        val violation = SourceBoundaryVerifier.verify(
            sources = listOf(uiSource("import com.aozijx.passly.domain.entry.model.Entry")),
            rules = SourceBoundaryPolicy.generalRules,
        ).single()

        val formatted = violation.format()
        assertTrue(formatted.contains("[PRESENTATION_UI_IMPORT]"))
        assertTrue(formatted.contains("presentation/ui/vault/list/VaultScreen.kt"))
        assertTrue(formatted.contains("import com.aozijx.passly.domain.entry.model.Entry"))
    }

    private fun uiSource(content: String) = EditorSource(
        path = "app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/VaultScreen.kt",
        content = content,
    )
}
