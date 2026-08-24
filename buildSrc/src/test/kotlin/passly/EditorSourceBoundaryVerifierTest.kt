package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorSourceBoundaryVerifierTest {
    @Test
    fun validPresentationMapperHasNoViolations() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source(
                    "app/src/main/java/com/example/presentation/feature/vault/editor/password/PasswordFormMapper.kt",
                    """
                    package com.example.presentation.feature.vault.editor.password
                    import com.aozijx.passly.domain.entry.model.EntryDraft
                    import com.aozijx.passly.domain.entry.policy.EntryTypeDefinitions
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun presentationEditorCannotImportDataImplementation() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(source("app/src/main/java/com/example/presentation/feature/vault/editor/X.kt", "import com.aozijx.passly.data.repository.EntryRepositoryImpl")),
        )

        assertTrue(violations.single().contains("data implementation"))
    }

    @Test
    fun lowerModulesCannotImportPresentationEditorState() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(source("domain/src/main/kotlin/com/example/X.kt", "import com.aozijx.passly.presentation.feature.vault.editor.otp.OtpFormState")),
        )

        assertTrue(violations.single().contains("lower module"))
    }

    @Test
    fun editorFactoriesAndLegacyOtpStateAreRejected() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/vault/editor/password/PasswordEntryFactory.kt", "package com.example"),
                source("app/src/main/java/com/example/feature/vault/model/OtpFormState.kt", "package com.aozijx.passly.feature.vault.model"),
            ),
        )

        assertEquals(2, violations.size)
    }

    @Test
    fun formMapperCannotOwnIdentityPersistenceCodecCryptoOrLinks() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source(
                    "app/src/main/java/com/example/presentation/feature/vault/editor/otp/OtpFormMapper.kt",
                    """
                    import com.aozijx.passly.domain.entry.port.EntryCommandRepository
                    import com.github.f4b6a3.uuid.UuidCreator
                    import com.aozijx.passly.core.otp.OtpAuthUriCodec
                    import com.aozijx.passly.security.crypto.SecretCipher
                    import com.aozijx.passly.domain.entry.model.relation.EntryLink
                    val now = System.currentTimeMillis()
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(6, violations.size)
    }

    @Test
    fun legacyPresentationEditorPackageIsRejected() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(source("app/src/main/java/com/example/presentation/vault/editor/X.kt", "package com.aozijx.passly.presentation.vault.editor")),
        )

        assertEquals(1, violations.size)
    }

    @Test
    fun migratedUiPackagesAreRejected() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/settings/SettingsScreen.kt", "package com.example.feature.settings"),
                source("app/src/main/java/com/example/feature/detail/DetailScreen.kt", "package com.example.feature.detail"),
                source("app/src/main/java/com/example/presentation/settings/SettingsScreen.kt", "package com.example.presentation.settings"),
                source("app/src/main/java/com/example/app/navigation/PasslyNavHost.kt", "package com.example.app.navigation"),
                source("app/src/main/java/com/example/feature/backup/presentation/contract/BackupUiState.kt", "package com.example"),
                source("app/src/main/java/com/example/feature/autofill/legacy/AutofillFillActivity.kt", "package com.example"),
            ),
        )

        assertEquals(6, violations.size)
        assertTrue(violations.all { it.contains("migrated UI") })
    }

    private fun source(path: String, content: String) = EditorSource(path, content)
}
