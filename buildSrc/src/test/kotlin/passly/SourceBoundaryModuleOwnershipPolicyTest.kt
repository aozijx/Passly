package passly

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceBoundaryModuleOwnershipPolicyTest {
    @Test
    fun dataCanOnlyImportReviewedCoreFamilies() {
        val allowedImports =
            listOf(
                "com.aozijx.passly.core.crypto.FieldEncryptor",
                "com.aozijx.passly.core.error.result.AppResult",
                "com.aozijx.passly.core.observability.Logger",
                "com.aozijx.passly.core.platform.VaultResourcePaths",
                "com.aozijx.passly.core.telemetry.TelemetryReporter",
                "com.aozijx.passly.core.crypto.SensitiveFieldCryptor",
            )
        val forbiddenImports =
            listOf(
                "com.aozijx.passly.core.ui.theme.PasslyTheme",
                "com.aozijx.passly.core.permission.RuntimePermission",
                "com.aozijx.passly.core.platform.packageinfo.InstalledAppCatalog",
            )

        allowedImports.forEach { allowedImport ->
            val source =
                EditorSource(
                    path = "data/src/main/java/com/aozijx/passly/data/Allowed.kt",
                    content = "import $allowedImport",
                )
            assertTrue(
                SourceBoundaryVerifier
                    .verify(
                        listOf(source),
                        SourceBoundaryPolicy.generalRules,
                    ).isEmpty(),
            )
        }
        forbiddenImports.forEach { forbiddenImport ->
            val source =
                EditorSource(
                    path = "data/src/main/java/com/aozijx/passly/data/Forbidden.kt",
                    content = "import $forbiddenImport",
                )
            assertEquals(
                "DATA_CORE_PACKAGE_ACCESS",
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
    fun coreCannotOwnEntryDomainPolicy() {
        val source =
            EditorSource(
                path = "core/src/main/kotlin/com/aozijx/passly/core/ui/components/settings/PasswordStrengthUiState.kt",
                content = "import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator",
            )

        assertEquals(
            "CORE_NO_DOMAIN_ENTRY_POLICY",
            SourceBoundaryVerifier
                .verify(listOf(source), SourceBoundaryPolicy.generalRules)
                .single()
                .ruleId,
        )
    }

    @Test
    fun coreCannotDeclareSecurityImplementationPackages() {
        val source =
            EditorSource(
                path = "core/src/main/kotlin/com/aozijx/passly/security/dek/DekManager.kt",
                content = "package com.aozijx.passly.security.dek",
            )

        assertEquals(
            "CORE_NO_SECURITY_IMPLEMENTATION",
            SourceBoundaryVerifier
                .verify(listOf(source), SourceBoundaryPolicy.generalRules)
                .single()
                .ruleId,
        )
    }

    @Test
    fun dataCannotImportSecurityImplementations() {
        val source =
            EditorSource(
                path = "data/src/main/java/com/aozijx/passly/data/codec/entry/SecretFieldCodec.kt",
                content = "import com.aozijx.passly.security.dek.SensitiveFieldEncryptor",
            )

        assertEquals(
            "DATA_NO_SECURITY_IMPLEMENTATION",
            SourceBoundaryVerifier
                .verify(listOf(source), SourceBoundaryPolicy.generalRules)
                .single()
                .ruleId,
        )
    }

    @Test
    fun autofillPendingIntentFactoriesCannotImportPresentationActivities() {
        val sources =
            listOf(
                EditorSource(
                    path = "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/AutofillPendingIntentFactory.kt",
                    content = "import com.aozijx.passly.app.autofill.presentation.AutofillAuthActivity",
                ),
                EditorSource(
                    path =
                        "feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/" +
                            "credential/service/CredentialPendingIntentFactory.kt",
                    content = "import com.aozijx.passly.app.autofill.presentation.AutofillAuthActivity",
                ),
            )

        assertEquals(
            listOf("AUTOFILL_FEATURE_NO_APP_IMPORT", "AUTOFILL_FEATURE_NO_APP_IMPORT"),
            SourceBoundaryVerifier
                .verify(sources, SourceBoundaryPolicy.generalRules)
                .map { it.ruleId },
        )
    }

    @Test
    fun passlyNavHostCannotRegisterFeatureDestinationsDirectly() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/feature/shell/presentation/navigation/PasslyNavHost.kt",
                content = "composable(AppRoute.Vault.route) { VaultHost() }",
            )

        assertEquals(
            "SHELL_NAV_HOST_FEATURE_REGISTRATION",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun authenticationHostCannotRemainInAppLocalCoreUi() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/core/ui/components/auth/AuthenticationHost.kt",
                content = "fun AuthenticationHost() = Unit",
            )

        assertEquals(
            "APP_LOCAL_CORE_AUTH_HOST",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun retiredEntryFieldKeysCannotReturn() {
        val source =
            EditorSource(
                path = "domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/FieldKey.kt",
                content = "enum class FieldKey { TITLE, URIS }",
            )

        assertEquals(
            "RETIRED_ENTRY_FIELD_KEY",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun vaultBootstrapCapabilitiesCannotHaveSilentDefaults() {
        val source =
            EditorSource(
                path = "domain/src/main/kotlin/com/aozijx/passly/domain/access/port/VaultBootstrapStore.kt",
                content = "suspend fun loadAttachmentKeyEnvelope(): ByteArray? = null",
            )

        assertEquals(
            "VAULT_BOOTSTRAP_SILENT_DEFAULT",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }

    @Test
    fun appBackupGatewayAllowsDomainPortAndRejectsDataImplementation() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/database/backup/AppBackupSnapshotGateway.kt",
                content = "import com.aozijx.passly.data.repository.entry.SecretFieldStore",
            )

        assertEquals(
            "APP_BACKUP_DATA_IMPLEMENTATION_IMPORT",
            SourceBoundaryVerifier
                .verify(listOf(source), SourceBoundaryPolicy.generalRules)
                .single()
                .ruleId,
        )

        val allowed = source.copy(content = "import com.aozijx.passly.domain.backup.port.VaultBackupStore")

        assertTrue(SourceBoundaryVerifier.verify(listOf(allowed), SourceBoundaryPolicy.generalRules).isEmpty())
    }

    @Test
    fun applicationCannotToggleManifestServicesAtRuntime() {
        val source =
            EditorSource(
                path = "app/src/main/java/com/aozijx/passly/app/PasslyApplication.kt",
                content = "packageManager.setComponentEnabledSetting(component, state, flags)",
            )

        assertEquals(
            "APPLICATION_RUNTIME_SERVICE_TOGGLE",
            SourceBoundaryVerifier
                .verify(
                    listOf(source),
                    SourceBoundaryPolicy.generalRules,
                ).single()
                .ruleId,
        )
    }
}
