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
                    "app/src/main/java/com/example/feature/vault/presentation/editor/password/PasswordFormMapper.kt",
                    """
                    package com.example.feature.vault.presentation.editor.password
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
            listOf(source("app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/X.kt", "import com.aozijx.passly.data.repository.EntryRepositoryImpl")),
        )

        assertTrue(violations.single().contains("owner=feature.presentation"))
    }

    @Test
    fun lowerModulesCannotImportPresentationEditorState() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(source("domain/src/main/kotlin/com/example/X.kt", "import com.aozijx.passly.feature.vault.presentation.editor.otp.OtpFormState")),
        )

        assertTrue(violations.single().contains("owner=domain"))
    }

    @Test
    fun formMapperCannotOwnIdentityPersistenceCodecCryptoOrLinks() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source(
                    "app/src/main/java/com/example/feature/vault/presentation/editor/otp/OtpFormMapper.kt",
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
                source("app/src/main/java/com/aozijx/passly/presentation/ui/vault/list/Feature.kt", "import com.aozijx.passly.feature.vault.presentation.list.VaultUiState"),
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
                source("app/src/main/java/com/example/feature/vault/presentation/list/VaultScreen.kt", "@Composable fun VaultScreen() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/list/component/Card.kt", "@Composable fun Card() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/list/VaultHost.kt", "@Composable fun VaultHost() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/list/ui/VaultScreen.kt", "@Composable fun VaultScreen() = Unit"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("passive vault-list UI") })
    }

    @Test
    fun vaultDetailPassiveUiCannotReturnToFeaturePackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/vault/presentation/detail/DetailScreen.kt", "@Composable fun DetailScreen() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/detail/component/Header.kt", "@Composable fun Header() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/detail/DetailHost.kt", "@Composable fun DetailHost() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/detail/ui/DetailScreen.kt", "@Composable fun DetailScreen() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/detail/ui/component/Header.kt", "@Composable fun Header() = Unit"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("passive vault-detail UI") })
    }

    @Test
    fun vaultEditorPassiveUiCannotReturnOutsideUiPackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/vault/presentation/editor/password/AddPasswordEditorScreen.kt", "@Composable fun AddPasswordEditorScreen() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/editor/common/AddEntryScaffold.kt", "class AddEntryScaffold"),
                source("app/src/main/java/com/example/feature/vault/presentation/editor/ui/password/AddPasswordEditorScreen.kt", "@Composable fun AddPasswordEditorScreen() = Unit"),
                source("app/src/main/java/com/example/feature/vault/presentation/editor/password/AddPasswordEditorRoute.kt", "@Composable fun AddPasswordEditorRoute() = Unit"),
            ),
        )

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.contains("passive vault-editor UI") })
    }

    @Test
    fun scannerPassiveUiCannotReturnOutsideUiPackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/scanner/presentation/ScannerContent.kt", "@Composable fun ScannerContent() = Unit"),
                source("app/src/main/java/com/example/feature/scanner/presentation/ui/ScannerContent.kt", "@Composable fun ScannerContent() = Unit"),
                source("app/src/main/java/com/example/feature/scanner/presentation/ScannerCameraHost.kt", "@Composable fun ScannerCameraHost() = Unit"),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("passive scanner UI"))
    }

    @Test
    fun backupPassiveUiCannotReturnOutsideUiPackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/backup/presentation/BackupRestoreSheet.kt", "@Composable fun BackupRestoreSheet() = Unit"),
                source("app/src/main/java/com/example/feature/backup/presentation/ui/BackupRestoreSheet.kt", "@Composable fun BackupRestoreSheet() = Unit"),
                source("app/src/main/java/com/example/feature/backup/presentation/BackupOperationRoute.kt", "@Composable fun BackupOperationRoute() = Unit"),
            ),
        )

        assertEquals(1, violations.size)
        assertTrue(violations.single().contains("passive backup UI"))
    }

    @Test
    fun backupMapperNamesAreNotClassifiedAsPassiveUi() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/backup/presentation/BackupSheetUiMapper.kt", "class BackupSheetUiMapper"),
                source("app/src/test/java/com/example/feature/backup/presentation/BackupSheetUiMapperTest.kt", "class BackupSheetUiMapperTest"),
            ),
        )

        assertEquals(emptyList(), violations)
    }

    @Test
    fun settingsPassiveUiCannotRemainInFeaturePackage() {
        val violations = EditorSourceBoundaryVerifier.verify(
            listOf(
                source("app/src/main/java/com/example/feature/settings/presentation/main/SettingsScreen.kt", "@Composable fun SettingsScreen() = Unit"),
                source("app/src/main/java/com/example/feature/settings/presentation/main/SettingsContent.kt", "@Composable fun SettingsContent() = Unit"),
                source("app/src/main/java/com/example/feature/settings/presentation/security/SecuritySection.kt", "@Composable fun SecuritySection() = Unit"),
                source("app/src/main/java/com/example/feature/settings/presentation/security/PasswordDialog.kt", "@Composable fun PasswordDialog() = Unit"),
                source("app/src/main/java/com/example/feature/settings/presentation/backup/RecoverySheet.kt", "@Composable fun RecoverySheet() = Unit"),
                source("app/src/main/java/com/example/feature/settings/presentation/main/SettingsHost.kt", "@Composable fun SettingsHost() = Unit"),
                source("app/src/main/java/com/example/feature/settings/presentation/main/SettingsRoute.kt", "data object SettingsRoute"),
                source("app/src/main/java/com/example/feature/settings/presentation/main/SettingsViewModel.kt", "class SettingsViewModel"),
                source("app/src/main/java/com/example/feature/settings/presentation/main/SettingsReducer.kt", "object SettingsReducer"),
                source("app/src/main/java/com/example/feature/settings/presentation/ui/data/DataManagementContent.kt", "@Composable fun DataManagementContent() = Unit"),
            ),
        )

        assertEquals(5, violations.size)
        assertTrue(violations.all { it.contains("passive settings UI") })
    }

    private fun source(path: String, content: String) = EditorSource(path, content)
}
