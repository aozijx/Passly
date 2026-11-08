package passly

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceBoundaryPresentationPolicyTest {
    @Test
    fun vaultUiCannotAcceptMultiplePagingStreams() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/VaultScreen.kt",
                content =
                    "entryPages: Map<VaultAddTypeUiModel, Flow<PagingData<VaultListItemUiModel>>>",
            )

        assertEquals(
            "VAULT_UI_PAGING_FACTORY",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun vaultEntryGridCannotDiscardItsExternalModifier() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/list/VaultEntryGrid.kt",
                content = "modifier = Modifier.fillMaxSize()",
            )

        assertEquals(
            "VAULT_LIST_EXTERNAL_MODIFIER",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun vaultPagingContainerCannotOwnEntryRowRendering() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/list/VaultEntryGrid.kt",
                content = "fun VaultEntryRow() = Unit",
            )

        assertEquals(
            "VAULT_LIST_ROW_OWNERSHIP",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun settingsNavigationCannotMirrorNavigatorDestinationInLocalState() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/SettingsRoute.kt",
                content = "mutableStateOf(navigator.currentDestination?.contentKey)",
            )

        assertEquals(
            "SETTINGS_DUPLICATE_NAVIGATION_STATE",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun passwordEditorRouteCannotRenderFieldsDirectly() {
        listOf(
            "NextFocusTextField(value = state.title)",
            "OutlinedTextField(value = state.title)",
        ).forEach { directField ->
            val source =
                EditorSource(
                    path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordEditorRoute.kt",
                    content = directField,
                )

            assertEquals(
                "PASSWORD_EDITOR_ROUTE_PASSIVE_UI",
                SourceBoundaryVerifier
                    .verify(
                        listOf(source),
                        SourceBoundaryPolicy.generalRules,
                    ).single()
                    .ruleId,
            )
        }
    }

    @Test
    fun otpEditorFeatureCannotRenderOtpFormDirectly() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt",
                content = "OtpConfigForm(state = state.form)",
            )

        assertEquals(
            "OTP_EDITOR_ROUTE_PASSIVE_UI",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun bankCardEditorFeatureCannotRenderCardTypeDropdownDirectly() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/bankcard/AddBankCardEditorRoute.kt",
                content = "CardTypeDropdown(selected = state.cardType)",
            )

        assertEquals(
            "BANK_CARD_EDITOR_ROUTE_PASSIVE_UI",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun settingsUiCannotImportVaultOwnedUiModelsOrComponents() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/interaction/QuickGestureSettingsSection.kt",
                content = "import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel",
            )

        assertEquals(
            "SETTINGS_UI_VAULT_OWNERSHIP",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun globalSettingsAccessRestrictionsRemainCoveredAcrossConsumers() {
        val cases =
            listOf(
                PolicyCase(
                    "application shell legacy settings bus",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt",
                            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
                        ),
                    ),
                    setOf("APP_GLOBAL_SETTINGS_BUS"),
                ),
                PolicyCase(
                    "appearance settings",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/appearance/AppearanceSettingsViewModel.kt",
                            content =
                                """
                                import com.aozijx.passly.domain.settings.model.SettingsCommand
                                import com.aozijx.passly.domain.settings.port.AppSettingsRepository
                                """.trimIndent(),
                        ),
                    ),
                    setOf("SETTINGS_APPEARANCE_NARROW_PORT"),
                ),
                PolicyCase(
                    "security settings",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/PrivacySettingsViewModel.kt",
                            content =
                                """
                                import com.aozijx.passly.domain.settings.model.SettingsCommand
                                import com.aozijx.passly.domain.settings.port.AppSettingsRepository
                                """.trimIndent(),
                        ),
                    ),
                    setOf("SECURITY_SETTINGS_NARROW_PORT"),
                ),
                PolicyCase(
                    "security runtime",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/app/clipboard/ClipboardCopyController.kt",
                            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
                        ),
                    ),
                    setOf("SECURITY_RUNTIME_NARROW_SOURCE"),
                    expectedViolationCount = 1,
                ),
                PolicyCase(
                    "message settings",
                    listOf(
                        EditorSource(
                            path =
                                "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/general/" +
                                    "NotificationSettingsViewModel.kt",
                            content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
                        ),
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/app/message/runtime/DefaultMessageSettingsSnapshotProvider.kt",
                            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
                        ),
                    ),
                    setOf("MESSAGE_SETTINGS_NARROW_PORT"),
                ),
                PolicyCase(
                    "backup settings",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/backup/DataManagementSettingsViewModel.kt",
                            content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
                        ),
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt",
                            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
                        ),
                    ),
                    setOf("BACKUP_SETTINGS_NARROW_PORT"),
                ),
                PolicyCase(
                    "vault list",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt",
                            content =
                                """
                                import com.aozijx.passly.domain.settings.model.SettingsCommand
                                import com.aozijx.passly.domain.settings.port.AppSettingsRepository
                                """.trimIndent(),
                        ),
                    ),
                    setOf("VAULT_LIBRARY_SETTINGS_NARROW_PORT"),
                ),
                PolicyCase(
                    "interaction consumers",
                    listOf(
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/autofill/AutofillSettingsViewModel.kt",
                            content = "import com.aozijx.passly.domain.settings.model.SettingsCommand",
                        ),
                        EditorSource(
                            path = "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt",
                            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
                        ),
                        EditorSource(
                            path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/display/VaultDisplayViewModel.kt",
                            content = "import com.aozijx.passly.domain.settings.port.AppSettingsRepository",
                        ),
                    ),
                    setOf("INTERACTION_SETTINGS_NARROW_PORT"),
                ),
            )

        cases.forEach { case ->
            val violations = SourceBoundaryVerifier.verify(case.sources, SourceBoundaryPolicy.generalRules)
            assertEquals(
                case.expectedRuleIds,
                violations.map { it.ruleId }.toSet(),
                case.name,
            )
            case.expectedViolationCount?.let { expectedCount ->
                assertEquals(expectedCount, violations.size, case.name)
            }
        }
    }

    private data class PolicyCase(
        val name: String,
        val sources: List<EditorSource>,
        val expectedRuleIds: Set<String>,
        val expectedViolationCount: Int? = null,
    )

    @Test
    fun dataManagementSettingsCannotOwnTrashStateOrCommands() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/backup/DataManagementSettingsViewModel.kt",
                content =
                    """
                    import com.aozijx.passly.domain.entry.port.EntryCommandRepository
                    val deletedEntries = emptyList<String>()
                    """.trimIndent(),
            )

        assertEquals(
            "SETTINGS_DATA_MANAGEMENT_TRASH_OWNERSHIP",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).map { it.ruleId }
                .distinct()
                .single(),
        )
    }

    @Test
    fun settingsPresentationCannotOwnDatabaseLifecycleOrResetCapability() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/SettingsRoute.kt",
                content =
                    """
                    import com.aozijx.passly.app.database.DatabaseLifecycleGateway
                    """.trimIndent(),
            )

        assertEquals(
            "SETTINGS_DATABASE_CAPABILITY_OWNERSHIP",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).map { it.ruleId }
                .distinct()
                .single(),
        )
    }

    @Test
    fun retiredDatabaseRecoveryCannotReturn() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/database/recovery/Recovery.kt",
                content = "class DatabaseRecoveryStore",
            )

        assertEquals(
            "RETIRED_DATABASE_RECOVERY",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }
}
