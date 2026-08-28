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
            listOf(source("app/src/main/java/com/aozijx/passly/presentation/feature/vault/editor/X.kt", "import com.aozijx.passly.data.repository.EntryRepositoryImpl")),
        )

        assertTrue(violations.single().contains("owner=presentation.feature"))
    }

    @Test
    fun lowerModulesCannotImportPresentationEditorState() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(source("domain/src/main/kotlin/com/example/X.kt", "import com.aozijx.passly.presentation.feature.vault.editor.otp.OtpFormState")),
        )

        assertTrue(violations.single().contains("owner=domain"))
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
    fun presentationUiCannotDependOnFeatureDomainOrData() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/Feature.kt", "import com.aozijx.passly.presentation.feature.vault.list.VaultUiState"),
                source("app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/AppFeature.kt", "import com.aozijx.passly.feature.vault.model.AddType"),
                source("app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/Domain.kt", "import com.aozijx.passly.domain.entry.model.Entry"),
                source("app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/Data.kt", "import com.aozijx.passly.data.repository.EntryRepositoryImpl"),
            ),
        )

        assertEquals(4, violations.size)
        assertTrue(violations.all { it.contains("owner=presentation.ui") })
        assertTrue(violations.all { it.contains("[LAYER_PRESENTATION_UI]") })
    }

    @Test
    fun presentationUiCannotOwnViewModelsOrUseHiltViewModelLookup() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/presentation/ui/vault/list/VaultViewModel.kt", "class VaultViewModel : ViewModel()"),
                source("app/src/main/java/com/example/presentation/ui/vault/list/VaultScreen.kt", "val model = hiltViewModel<VaultViewModel>()"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("ViewModel") })
        assertTrue(violations.all { it.contains("[PRESENTATION_UI_VIEW_MODEL]") })
    }

    @Test
    fun presentationUiMayUseCoreUiComposeAndPaging() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source(
                    "app/src/main/java/com/example/presentation/ui/vault/list/VaultScreen.kt",
                    """
                    import androidx.compose.runtime.Composable
                    import androidx.paging.compose.LazyPagingItems
                    import com.aozijx.passly.core.ui.theme.PasslyTheme
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun vaultListPassiveUiCannotReturnToFeaturePackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/presentation/feature/vault/list/VaultScreen.kt", "@Composable fun VaultScreen() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/vault/list/component/Card.kt", "@Composable fun Card() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/vault/list/VaultHost.kt", "@Composable fun VaultHost() = Unit"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("passive vault-list UI") })
    }

    @Test
    fun vaultDetailPassiveUiCannotReturnToFeaturePackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/presentation/feature/vault/detail/DetailScreen.kt", "@Composable fun DetailScreen() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/vault/detail/component/Header.kt", "@Composable fun Header() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/vault/detail/DetailHost.kt", "@Composable fun DetailHost() = Unit"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("passive vault-detail UI") })
    }

    @Test
    fun settingsPassiveUiCannotRemainInFeaturePackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/presentation/feature/settings/main/SettingsScreen.kt", "@Composable fun SettingsScreen() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/settings/main/SettingsContent.kt", "@Composable fun SettingsContent() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/settings/security/SecuritySection.kt", "@Composable fun SecuritySection() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/settings/security/PasswordDialog.kt", "@Composable fun PasswordDialog() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/settings/backup/RecoverySheet.kt", "@Composable fun RecoverySheet() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/settings/main/SettingsHost.kt", "@Composable fun SettingsHost() = Unit"),
                source("app/src/main/java/com/example/presentation/feature/settings/main/SettingsRoute.kt", "data object SettingsRoute"),
                source("app/src/main/java/com/example/presentation/feature/settings/main/SettingsViewModel.kt", "class SettingsViewModel"),
                source("app/src/main/java/com/example/presentation/feature/settings/main/SettingsReducer.kt", "object SettingsReducer"),
            ),
        )

        assertEquals(5, violations.size)
        assertTrue(violations.all { it.contains("passive settings UI") })
    }

    private fun source(path: String, content: String) = EditorSource(path, content)
}
