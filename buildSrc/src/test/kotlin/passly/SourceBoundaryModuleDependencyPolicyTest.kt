package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryModuleDependencyPolicyTest {
    @Test
    fun missingRequiredEdgesReturnsOnlyUnresolvedPairs() {
        val requiredEdges =
            setOf(
                edge(":app", ":domain"),
                edge(":data", ":domain"),
            )
        val actualEdges = setOf(edge(":app", ":core"), edge(":data", ":core"))

        assertEquals(
            requiredEdges,
            missingRequiredEdges(requiredEdges, actualEdges),
        )
        assertEquals(
            emptySet(),
            missingRequiredEdges(requiredEdges, actualEdges + requiredEdges),
        )
    }

    @Test
    fun presentationUiRejectsViewModelLookupWithExactEvidence() {
        val violations =
            SourceBoundaryVerifier.verify(
                sources = listOf(uiSource("val owner = hiltViewModel<VaultViewModel>()")),
                rules = SourceBoundaryPolicy.generalRules,
            )

        assertEquals(1, violations.size)
        assertEquals("PRESENTATION_UI_VIEW_MODEL", violations.single().ruleId)
        assertEquals("val owner = hiltViewModel<VaultViewModel>()", violations.single().evidence)
    }

    @Test
    fun commentsAndSimilarlyNamedPackagesDoNotMatchImportRules() {
        val violations =
            SourceBoundaryVerifier.verify(
                sources =
                    listOf(
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
        val violation =
            SourceBoundaryVerifier
                .verify(
                    sources = listOf(uiSource("import com.aozijx.passly.domain.entry.model.Entry")),
                    rules = SourceBoundaryPolicy.generalRules,
                ).single()

        val formatted = violation.format()
        assertTrue(formatted.contains("[LAYER_PRESENTATION_UI]"))
        assertTrue(formatted.contains("presentation/ui/vault/list/VaultScreen.kt"))
        assertTrue(formatted.contains("import com.aozijx.passly.domain.entry.model.Entry"))
    }

    @Test
    fun presentationFeatureMayUseDomainPagingPortButNotDataPaging() {
        val allowed =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt",
                content = "import com.aozijx.passly.domain.entry.port.EntryPagingRepository",
            )
        val forbidden =
            allowed.copy(
                content = "import com.aozijx.passly.data.repository.entry.paging.RoomEntryPagingStore",
            )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules),
        )
        val violations =
            SourceBoundaryVerifier.verify(
                listOf(forbidden),
                SourceBoundaryPolicy.generalRules,
            )
        assertTrue(violations.any { it.ruleId == "LAYER_FEATURE_PRESENTATION" })
    }

    @Test
    fun domainPagingPortAllowsOnlyPagingCommonTypes() {
        val pagingPort =
            EditorSource(
                path = "domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryPagingRepository.kt",
                content = "import androidx.paging.PagingData",
            )
        val composePort =
            pagingPort.copy(
                content = "import androidx.compose.runtime.Immutable",
            )

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(listOf(pagingPort), SourceBoundaryPolicy.generalRules),
        )
        assertTrue(
            SourceBoundaryVerifier
                .verify(listOf(composePort), SourceBoundaryPolicy.generalRules)
                .any { it.ruleId == "LAYER_DOMAIN" },
        )
    }

    @Test
    fun featureImplementationCannotImportDataOrPresentation() {
        val forbiddenSources =
            listOf(
                EditorSource(
                    "app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt",
                    "import com.aozijx.passly.feature.backup.presentation.BackupUiState",
                ),
                EditorSource(
                    "app/src/main/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGateway.kt",
                    "import com.aozijx.passly.data.local.database.session.AppDatabaseSession",
                ),
            )

        val violations = SourceBoundaryVerifier.verify(forbiddenSources, SourceBoundaryPolicy.generalRules)
        assertEquals(List(forbiddenSources.size) { "LAYER_FEATURE" }, violations.map { it.ruleId })
        assertEquals(forbiddenSources.map { it.path }, violations.map { it.path })
        assertEquals(forbiddenSources.map { it.content }, violations.map { it.evidence })
    }

    @Test
    fun presentationOutsideUnlockCannotImportAuthenticationManager() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt",
                content = "import com.aozijx.passly.domain.access.port.AuthenticationManager",
            )

        assertEquals(
            "PRESENTATION_AUTH_MANAGER_IMPORT",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun applicationLifecycleCannotImportAuthenticationManager() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/AppLifecycleObserver.kt",
                content = "import com.aozijx.passly.domain.access.port.AuthenticationManager",
            )

        assertEquals(
            "APPLICATION_AUTH_MANAGER_IMPORT",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun appSecurityImportsAreLimitedToTheAuthenticationHostContract() {
        val hostImport = "import com.aozijx.passly.security.authentication.host.AuthenticationHost"
        val allowedSources =
            listOf(
                "app/src/main/java/com/aozijx/passly/presentation/PasslyApp.kt",
                "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt",
            ).map { path -> EditorSource(path, hostImport) }
        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(allowedSources, SourceBoundaryPolicy.generalRules),
        )

        val forbiddenSources =
            listOf(
                EditorSource(
                    "app/src/main/java/com/aozijx/passly/app/autofill/AndroidAutofillLaunchTarget.kt",
                    "import com.aozijx.passly.security.dek.DekManager",
                ),
                EditorSource(
                    "app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotReader.kt",
                    "import com.aozijx.passly.security.dek.AttachmentContentCrypto",
                ),
                EditorSource(
                    "app/src/main/java/com/aozijx/passly/feature/unlock/presentation/UnlockViewModel.kt",
                    "import com.aozijx.passly.security.authentication.BiometricRotationReconciler",
                ),
                EditorSource(
                    "app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt",
                    "import com.aozijx.passly.security.session.VaultSessionController",
                ),
            )
        val violations = SourceBoundaryVerifier.verify(forbiddenSources, SourceBoundaryPolicy.generalRules)
        assertEquals(
            List(forbiddenSources.size) { "APP_ADAPTER_SECURITY_IMPLEMENTATION_IMPORT" },
            violations.map { it.ruleId },
        )
        assertEquals(forbiddenSources.map { it.path }, violations.map { it.path })
        assertEquals(forbiddenSources.map { it.content }, violations.map { it.evidence })
    }

    @Test
    fun autofillCannotImportFullAuthenticationManager() {
        val source =
            EditorSource(
                path = "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/AuthorizeAutofillInteraction.kt",
                content = "import com.aozijx.passly.domain.access.port.AuthenticationManager",
            )

        assertEquals(
            "AUTOFILL_AUTH_MANAGER_IMPORT",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun appCanOnlyImportTheReviewedAutofillModuleApi() {
        val reviewedImports =
            listOf(
                "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt" to
                    "com.aozijx.passly.feature.autofill.presentation.AutofillFeatureEntry",
                "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt" to
                    "com.aozijx.passly.feature.autofill.presentation.AutofillHostCompletion",
                "app/src/main/java/com/aozijx/passly/app/autofill/AutofillPlatformModule.kt" to
                    "com.aozijx.passly.feature.autofill.platform.AutofillLaunchTarget",
                "app/src/main/java/com/aozijx/passly/app/autofill/AndroidAutofillLaunchTarget.kt" to
                    "com.aozijx.passly.feature.autofill.platform.AutofillHostLaunchContract",
            ).map { (path, importedType) ->
                EditorSource(path, "import $importedType")
            }

        assertTrue(
            SourceBoundaryVerifier
                .verify(reviewedImports, SourceBoundaryPolicy.generalRules)
                .isEmpty(),
        )

        val obsoleteHostImports =
            listOf(
                "com.aozijx.passly.feature.autofill.presentation.AutofillHostPresentation",
                "com.aozijx.passly.feature.autofill.presentation.autofillHostPresentation",
            ).map { importedType ->
                EditorSource(
                    path = "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt",
                    content = "import $importedType",
                )
            }
        assertEquals(
            listOf("AUTOFILL_MODULE_API", "AUTOFILL_MODULE_API"),
            SourceBoundaryVerifier
                .verify(obsoleteHostImports, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )

        val implementationImport =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/autofill/AndroidAutofillLaunchTarget.kt",
                content = "import com.aozijx.passly.feature.autofill.credential.service.ModernCredentialService",
            )
        val violation =
            SourceBoundaryVerifier
                .verify(listOf(implementationImport), SourceBoundaryPolicy.generalRules)
                .single()

        assertEquals("AUTOFILL_MODULE_API", violation.ruleId)

        val featureUiImport =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt",
                content = "import com.aozijx.passly.feature.autofill.presentation.accessibility.AccessibilityFillUiState",
            )
        assertEquals(
            "AUTOFILL_MODULE_API",
            SourceBoundaryVerifier.verify(listOf(featureUiImport), SourceBoundaryPolicy.generalRules).single().ruleId,
        )

        val featureSheetImport =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt",
                content = "import com.aozijx.passly.feature.autofill.presentation.legacy.ui.AutofillCandidateBottomSheet",
            )
        assertEquals(
            "AUTOFILL_MODULE_API",
            SourceBoundaryVerifier.verify(listOf(featureSheetImport), SourceBoundaryPolicy.generalRules).single().ruleId,
        )

        val formerAppAdapterImport =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/autofill/AndroidAutofillLaunchTarget.kt",
                content = "import com.aozijx.passly.feature.autofill.core.SaveAutofillCredential",
            )
        assertEquals(
            "AUTOFILL_MODULE_API",
            SourceBoundaryVerifier
                .verify(listOf(formerAppAdapterImport), SourceBoundaryPolicy.generalRules)
                .single()
                .ruleId,
        )
    }

    @Test
    fun autofillApiBoundaryIgnoresAppPackageDeclaration() {
        val appPackage =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt",
                content = "package com.aozijx.passly.app.autofill.presentation",
            )

        assertTrue(
            SourceBoundaryVerifier
                .verify(listOf(appPackage), SourceBoundaryPolicy.generalRules)
                .isEmpty(),
        )
    }

    @Test
    fun autofillAuthenticationHostSecurityImportsAreLimitedToSystemActivities() {
        val hostImport = "import com.aozijx.passly.security.authentication.host.AuthenticationHost"
        val approvedActivities =
            listOf(
                "app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt",
            ).map { path -> EditorSource(path, hostImport) }

        assertEquals(
            emptyList(),
            SourceBoundaryVerifier.verify(approvedActivities, SourceBoundaryPolicy.generalRules),
        )

        val businessViewModel =
            EditorSource(
                "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillViewModel.kt",
                hostImport,
            )
        val violation =
            SourceBoundaryVerifier
                .verify(
                    listOf(businessViewModel),
                    SourceBoundaryPolicy.generalRules,
                ).single()
        assertEquals("AUTOFILL_AUTH_HOST_ACTIVITY_OWNER", violation.ruleId)
    }

    @Test
    fun autofillFeatureUiCannotDependOnAppVaultIcons() {
        val appOwnedIconImport =
            EditorSource(
                path =
                    "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/" +
                        "presentation/legacy/ui/AutofillCandidateBottomSheet.kt",
                content = "import com.aozijx.passly.presentation.shared.components.VaultItemIcon",
            )

        val violation =
            SourceBoundaryVerifier
                .verify(listOf(appOwnedIconImport), SourceBoundaryPolicy.generalRules)
                .single()

        assertEquals("AUTOFILL_UI_NO_APP_ICON", violation.ruleId)
    }

    @Test
    fun autofillLegacyRouteMustStayInItsFeatureModule() {
        val appOwnedRoute =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillRoute.kt",
                content = "fun AutofillFillRoute(",
            )

        val violation =
            SourceBoundaryVerifier
                .verify(listOf(appOwnedRoute), SourceBoundaryPolicy.generalRules)
                .single()

        assertEquals("AUTOFILL_LEGACY_ROUTE_OWNER", violation.ruleId)
    }

    @Test
    fun autofillPlatformAdaptersCannotImportRepositoriesOrAuthenticationPorts() {
        val sources =
            listOf(
                EditorSource(
                    path =
                        "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/" +
                            "credential/service/CredentialPlatformAdapter.kt",
                    content = "import com.aozijx.passly.domain.entry.port.EntryQueryRepository",
                ),
                EditorSource(
                    path =
                        "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/" +
                            "legacy/service/adapter/LegacyPlatformAdapter.kt",
                    content = "import com.aozijx.passly.domain.access.port.AuthenticationRequester",
                ),
            )

        assertEquals(
            listOf(
                "AUTOFILL_CREDENTIAL_ADAPTER_DOMAIN_PORT",
                "AUTOFILL_LEGACY_ADAPTER_DOMAIN_PORT",
            ),
            SourceBoundaryVerifier
                .verify(sources, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )

        val readOnlySessionProjection =
            EditorSource(
                path = "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlow.kt",
                content = "import com.aozijx.passly.domain.access.port.SecureSessionAccessState",
            )
        assertTrue(
            SourceBoundaryVerifier
                .verify(
                    listOf(readOnlySessionProjection),
                    SourceBoundaryPolicy.generalRules,
                ).isEmpty(),
        )
    }

    @Test
    fun autofillCandidateSummaryCannotContainAggregateOrSecretTypes() {
        val allowed =
            EditorSource(
                path = "domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCandidateSummary.kt",
                content = "import com.aozijx.passly.domain.entry.model.query.EntryListItem",
            )
        val forbidden =
            listOf(
                "com.aozijx.passly.domain.entry.model.Entry",
                "com.aozijx.passly.domain.entry.model.EntrySecret",
                "com.aozijx.passly.domain.entry.model.credential.EntryCredential",
            ).map { importedType ->
                EditorSource(
                    path = "domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCandidateSummary.kt",
                    content = "import $importedType",
                )
            }

        assertTrue(
            SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules).isEmpty(),
        )
        assertEquals(
            listOf(
                "AUTOFILL_CANDIDATE_SECRET_BOUNDARY",
                "AUTOFILL_CANDIDATE_SECRET_BOUNDARY",
                "AUTOFILL_CANDIDATE_SECRET_BOUNDARY",
            ),
            SourceBoundaryVerifier
                .verify(forbidden, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )
    }
}
