package com.aozijx.passly.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MigrationBoundaryTest {

    private val productionKotlinFiles: Sequence<File>
        get() = File("src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }

    @Test
    fun productionSourcesDoNotReferenceLegacyFeaturePackage() {
        val sourceRoot = File("src/main")
        val offenders = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .filter { "com.aozijx.passly.ui.features" in it.readText() }
            .map { it.relativeTo(sourceRoot).path }
            .toList()

        assertTrue("Legacy feature package references: $offenders", offenders.isEmpty())
    }

    @Test
    fun candidateResolverDependsOnRepositoryContract() {
        val source = File(
            "src/main/java/com/aozijx/passly/core/autofill/pipeline/CandidateResolver.kt"
        ).readText()

        assertTrue("CandidateResolver must use the domain contract", "CredentialServiceRepository" in source)
        assertTrue(
            "CandidateResolver must not use the data implementation",
            "CredentialServiceRepositoryImpl" !in source
        )
    }

    @Test
    fun domainHasNoAndroidDataOrFeatureDependencies() {
        val forbidden = listOf(
            "import android.",
            "import androidx.",
            "import com.aozijx.passly.data.",
            "import com.aozijx.passly.feature."
        )
        val offenders = productionKotlinFiles
            .filter { "/domain/" in it.invariantSeparatorsPath }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Domain boundary violations: $offenders", offenders.isEmpty())
    }

    @Test
    fun upperLayersDoNotReachIntoDataImplementations() {
        val guardedPackages = listOf("/core/", "/feature/", "/security/")
        val exemptPaths = listOf("/core/session/")
        val offenders = productionKotlinFiles
            .filter { source ->
                guardedPackages.any { it in source.invariantSeparatorsPath }
            }
            .filter { source ->
                exemptPaths.none { it in source.invariantSeparatorsPath }
            }
            .filter { "import com.aozijx.passly.data." in it.readText() }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Data implementation leaks: $offenders", offenders.isEmpty())
    }

    @Test
    fun vaultFeatureDoesNotDependOnSiblingFeaturesOrInternalBucket() {
        val vaultRoot = File("src/main/java/com/aozijx/passly/feature/vault")
        val siblingFeatureImport = Regex(
            """import com\.aozijx\.passly\.feature\.(?!vault(?:\.|$))"""
        )
        val siblingFeatureOffenders = vaultRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { siblingFeatureImport.containsMatchIn(it.readText()) }
            .map { it.relativeTo(vaultRoot).path }
            .toList()
        val internalBucket = File(vaultRoot, "internal")
        val internalFiles = if (internalBucket.exists()) {
            internalBucket.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.relativeTo(vaultRoot).path }
                .toList()
        } else {
            emptyList()
        }

        assertTrue(
            "Vault imports sibling features: $siblingFeatureOffenders",
            siblingFeatureOffenders.isEmpty()
        )
        assertTrue(
            "Vault internal bucket must stay split by responsibility: $internalFiles",
            internalFiles.isEmpty()
        )
    }

    @Test
    fun vaultMenuStateIsTransientAndEntryCreationUsesDedicatedRoutes() {
        val vaultUiState = File(
            "src/main/java/com/aozijx/passly/feature/vault/contract/VaultUiState.kt"
        ).readText()
        val searchFilterState = File(
            "src/main/java/com/aozijx/passly/feature/vault/list/SearchFilterState.kt"
        ).readText()
        val topBar = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/topbar/VaultTopBar.kt"
        ).readText()
        val routeSource = File(
            "src/main/java/com/aozijx/passly/app/navigation/AppRoute.kt"
        ).readText()
        val passwordScreen = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/password/" +
                    "AddPasswordScreen.kt"
        )
        val otpScreen = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/otp/" +
                    "AddOtpScreen.kt"
        )
        val commonScaffold = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/common/" +
                    "AddEntryScaffold.kt"
        ).readText()
        val oldPasswordDialog = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/editor/" +
                    "AddPasswordDialog.kt"
        )
        val oldOtpDialog = File(
            "src/main/java/com/aozijx/passly/feature/vault/components/editor/" +
                    "AddOtpDialog.kt"
        )

        assertTrue(
            "Popup visibility must not survive in VaultViewModel state",
            "isMoreMenuExpanded" !in vaultUiState &&
                    "isMoreMenuExpanded" !in searchFilterState
        )
        assertTrue(
            "Top bar must own popup visibility locally",
            "var isMoreMenuExpanded by remember" in topBar
        )
        assertTrue(
            "Password and OTP creation must have dedicated navigation routes",
            "data object AddPassword" in routeSource &&
                    "data object AddOtp" in routeSource &&
                    passwordScreen.isFile &&
                    otpScreen.isFile
        )
        assertTrue(
            "Password and OTP creation dialogs must not return",
            !oldPasswordDialog.exists() && !oldOtpDialog.exists()
        )
        assertTrue(
            "Entry creation pages must share the page shell and FAB transition",
            "fun AddEntryScaffold" in commonScaffold &&
                    "sharedBounds" in commonScaffold &&
                    "ADD_ENTRY_FAB_SHARED_KEY" in commonScaffold
        )
    }

    @Test
    fun associatedPackagesStayEncryptedAndVisibleInEntryDetails() {
        val entryEntity = File(
            "src/main/java/com/aozijx/passly/data/model/entity/EntryEntity.kt"
        ).readText()
        val summaryPayload = File(
            "src/main/java/com/aozijx/passly/data/model/payload/summary/SummaryPayload.kt"
        ).readText()
        val associatedInfoSection = File(
            "src/main/java/com/aozijx/passly/feature/detail/ui/sections/" +
                    "AssociatedInfoSection.kt"
        ).readText()

        assertTrue(
            "Associated packages must remain inside encrypted summary payloads",
            "packageNames: Set<String>" in summaryPayload &&
                    "val packageName" !in entryEntity
        )
        assertTrue(
            "Login details must expose associated package names",
            "entry.website?.packageNames" in associatedInfoSection &&
                    "AssociatedAppRow" in associatedInfoSection
        )
    }

    @Test
    fun appPasswordFeedbackAndEntryAuthenticationStaySeparated() {
        val passwordFields = File(
            "src/main/java/com/aozijx/passly/core/ui/components/apppassword/" +
                    "PasswordFields.kt"
        ).readText()
        val authenticationHost = File(
            "src/main/java/com/aozijx/passly/core/ui/components/auth/AuthenticationHost.kt"
        ).readText()
        val authenticationScreen = File(
            "src/main/java/com/aozijx/passly/feature/auth/ui/AuthenticationScreen.kt"
        ).readText()
        val addPasswordScreen = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/password/" +
                    "AddPasswordScreen.kt"
        ).readText()
        val provisioner = File(
            "src/main/java/com/aozijx/passly/security/authentication/" +
                    "DefaultAuthenticationMethodProvisioner.kt"
        ).readText()
        val authenticationModels = File(
            "src/main/java/com/aozijx/passly/domain/authentication/AuthenticationModels.kt"
        ).readText()
        val mainScreen = File(
            "src/main/java/com/aozijx/passly/feature/main/ui/MainScreen.kt"
        ).readText()
        val inputActionButton = File(
            "src/main/java/com/aozijx/passly/core/ui/components/common/" +
                    "InputActionButton.kt"
        ).readText()
        val authenticationViewModel = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/" +
                    "AuthenticationViewModel.kt"
        ).readText()
        val authenticationUiState = File(
            "src/main/java/com/aozijx/passly/feature/auth/contract/" +
                    "AuthenticationUiState.kt"
        ).readText()

        assertTrue(
            "Password inputs must use the platform password input contract",
            "keyboardType = KeyboardType.Password" in passwordFields &&
                    "keyboardType = KeyboardType.Password" in authenticationHost
        )
        assertTrue(
            "Authentication failures must stay authoritative from AuthenticationManager",
            "result = state.result" in inputActionButton &&
                    "RESULT_DISPLAY_DURATION_MS" in inputActionButton &&
                    "val failure: AuthenticationFailure" in authenticationUiState &&
                    "AuthenticationUiError" !in authenticationUiState &&
                    "toUiError" !in authenticationViewModel
        )
        assertTrue(
            "Expanded authentication inputs must remain visible above the IME",
            "imePadding()" in authenticationScreen &&
                    "bringIntoViewRequester.bringIntoView()" in inputActionButton &&
                    "keyboardController?.hide()" in inputActionButton &&
                    "focusManager.clearFocus()" in inputActionButton
        )
        assertTrue(
            "Changing an app password must verify the current password",
            "currentPassword: CharArray" in provisioner &&
                    "AuthenticationPurpose.MANAGE_APP_PASSWORD" in provisioner
        )
        assertTrue(
            "Creating a vault entry must not introduce a second authentication gate",
            "AuthenticationPurpose" !in addPasswordScreen &&
                    "CREATE_ENTRY" !in authenticationModels
        )
        assertTrue(
            "FLAG_SECURE must remain controlled only by the global setting",
            "if (mainConfig.isSecureContentEnabled)" in mainScreen &&
                    "SensitiveContent" !in mainScreen
        )
    }

    @Test
    fun packageNamesMatchSourceDirectories() {
        val sourceRoot = File("src/main/java")
        val offenders = productionKotlinFiles.mapNotNull { source ->
            val declaredPackage = source.useLines { lines ->
                lines.firstOrNull { it.startsWith("package ") }
                    ?.removePrefix("package ")
                    ?.trim()
            } ?: return@mapNotNull source.relativeTo(sourceRoot).path
            val expectedDirectory = declaredPackage.replace('.', File.separatorChar)
            val actualDirectory = source.parentFile!!.relativeTo(sourceRoot).path
            source.relativeTo(sourceRoot).path.takeIf { actualDirectory != expectedDirectory }
        }.toList()

        assertTrue("Package/path mismatches: $offenders", offenders.isEmpty())
    }

    @Test
    fun viewModelsDoNotOwnTopLevelMviContracts() {
        val topLevelContractPattern = Regex("""(?m)^(data class|sealed interface|sealed class)\s+""")
        val offenders = productionKotlinFiles
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { topLevelContractPattern.containsMatchIn(it.readText()) }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue(
            "Move ViewModel-owned MVI contracts into dedicated contract files: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun migratedFeaturePresentationKeepsComposeInsideUiPackagesOrFeatureScreens() {
        val guardedFeaturePaths = listOf(
            "/feature/verification/",
            "/feature/settings/apppassword/",
            "/feature/settings/security/"
        )
        val offenders = productionKotlinFiles
            .filter { source -> guardedFeaturePaths.any { it in source.invariantSeparatorsPath } }
            .filter {
                "/ui/" !in it.invariantSeparatorsPath &&
                        !it.name.endsWith("Screen.kt")
            }
            .filter { "androidx.compose" in it.readText() }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue(
            "Compose outside UI packages or feature Screen entry points: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun vaultQuickFilterModelDoesNotOwnUiPresentation() {
        val vaultQuickFilter = File(
            "src/main/java/com/aozijx/passly/domain/settings/model/VaultQuickFilter.kt"
        ).readText()

        assertTrue(
            "VaultQuickFilter must stay a pure filtering/settings model",
            "androidx.compose" !in vaultQuickFilter &&
                    "com.aozijx.passly.R" !in vaultQuickFilter &&
                    "ImageVector" !in vaultQuickFilter &&
                    "titleRes" !in vaultQuickFilter
        )
    }

    @Test
    fun addTypeModelDoesNotOwnUiPresentation() {
        val addType = File(
            "src/main/java/com/aozijx/passly/feature/vault/model/AddType.kt"
        ).readText()

        assertTrue(
            "AddType must stay a pure creation-type model",
            "androidx.compose" !in addType &&
                    "com.aozijx.passly.R" !in addType &&
                    "ImageVector" !in addType &&
                    "labelRes" !in addType
        )
    }

    @Test
    fun recoveryCodeCreationUsesFreshIdentityVerification() {
        val recoveryDraftViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/RecoveryDraftViewModel.kt"
        ).readText()

        assertTrue(
            "Recovery-code module must request its own fresh authentication",
            "AuthenticationPurpose.MANAGE_RECOVERY_CODE" in recoveryDraftViewModel &&
                    "authenticationManager.authenticate(" in recoveryDraftViewModel
        )
    }

    @Test
    fun roundedGroupUsesCommonUiWithMandatoryStableKeys() {
        val commonGroupRoot = File(
            "src/main/java/com/aozijx/passly/core/ui/components/group"
        )
        val roundedGroupSource = File(commonGroupRoot, "RoundedGroup.kt").readText()
        val legacyGroupRoot = File(
            "src/main/java/com/aozijx/passly/feature/settings/components"
        )

        assertTrue("RoundedGroup must live in common UI", commonGroupRoot.isDirectory)
        assertTrue(
            "Feature-specific RoundedGroup components must be removed",
            !legacyGroupRoot.exists() || legacyGroupRoot.walkTopDown().none { it.extension == "kt" }
        )
        assertTrue(
            "RoundedGroupBuilder must not return",
            !File(commonGroupRoot, "RoundedGroupBuilder.kt").exists()
        )
        assertTrue(
            "RoundedGroup keys must be mandatory strings",
            "val key: String" in roundedGroupSource
        )
        assertTrue(
            "RoundedGroup must not fall back to list indexes",
            "originalIndex" !in roundedGroupSource
        )
        assertTrue(
            "RoundedGroup position lookup must remain linear",
            ".indexOf(" !in roundedGroupSource
        )
    }

    @Test
    fun settingsSectionIsCustomizableAndLivesInCommonUi() {
        val sectionSource = File(
            "src/main/java/com/aozijx/passly/core/ui/components/settings/SettingsSection.kt"
        ).readText()
        val legacyParts = File(
            "src/main/java/com/aozijx/passly/feature/settings/shell/SettingsUiParts.kt"
        )

        assertTrue("SettingsUiParts must not return", !legacyParts.exists())
        assertTrue(
            "Section layout fields must be externally configurable",
            "SettingsSectionStyle" in sectionSource
        )
        assertTrue(
            "SettingsSection must accept caller-owned composable content",
            "content: @Composable ColumnScope.() -> Unit" in sectionSource
        )
    }

    @Test
    fun featuresUseCentralPermissionRequesters() {
        val offenders = productionKotlinFiles
            .filter { "/feature/" in it.invariantSeparatorsPath }
            .filter { source ->
                val text = source.readText()
                "Manifest.permission." in text ||
                        "ActivityResultContracts.RequestPermission" in text ||
                        "ActivityResultContracts.RequestMultiplePermissions" in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue(
            "Feature permission requests bypass the central module: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun notificationPermissionIsRequestedOnlyFromItsSetting() {
        val mainActivity = File("src/main/java/com/aozijx/passly/MainActivity.kt").readText()
        val notificationSettings = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/NotificationDetail.kt"
        ).readText()

        assertTrue(
            "Startup must not request notification permission",
            "request(RuntimePermission.POST_NOTIFICATIONS)" !in mainActivity
        )
        assertTrue(
            "Message setting must own notification permission requests",
            "permissionHost.request(RuntimePermission.POST_NOTIFICATIONS)" in notificationSettings
        )
    }

    @Test
    fun backupFeatureOwnsItsUiAndUsesBottomSheets() {
        val vaultRoot = File("src/main/java/com/aozijx/passly/feature/vault")
        val vaultOffenders = vaultRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter {
                val text = it.readText()
                "BackupViewModel" in text ||
                        "BackupAction" in text ||
                    "BackupPasswordDialog" in text ||
                    "CustomExportMenuItem" in text
            }
            .map { it.relativeTo(vaultRoot).path }
            .toList()
        val sheetSource = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/ui/BackupRestoreSheets.kt"
        ).readText()

        assertTrue("Vault must not own backup actions: $vaultOffenders", vaultOffenders.isEmpty())
        assertTrue("Backup UI must use Material bottom sheets", "ModalBottomSheet(" in sheetSource)
        assertTrue(
            "Format picker must expose all first-party exports",
            listOf("ENCRYPTED", "JSON", "TEXT").all { it in sheetSource }
        )
    }

    @Test
    fun legacyBackupUiCannotReturn() {
        val removedFiles = listOf(
            File("src/main/java/com/aozijx/passly/core/ui/components/BackupPasswordDialog.kt"),
            File("src/main/java/com/aozijx/passly/core/ui/components/PlainExportDialog.kt"),
            File("src/main/java/com/aozijx/passly/core/util/PlainExportTokenManager.kt"),
            File(
                "src/main/java/com/aozijx/passly/feature/vault/components/topbar/CustomExportMenuItem.kt"
            )
        )

        assertTrue(
            "Legacy backup UI files must stay deleted",
            removedFiles.none(File::exists)
        )
    }

    @Test
    fun messageSettingsOwnTopicPreferences() {
        val removedSecurityToasts = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/ui/SecurityToastSettingsSection.kt"
        )
        val generalNotifications = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/NotificationSettingsSection.kt"
        ).readText()

        assertTrue(
            "Legacy security Toast settings must be removed",
            !removedSecurityToasts.exists()
        )
        assertTrue(
            "General message settings must expose topic controls",
            "NoticeTopic.entries" in generalNotifications
        )
    }

    @Test
    fun legacyMessageAndPermissionStacksCannotReturn() {
        val forbiddenSymbols = listOf(
            "AppMessageCenter",
            "AppMessagePublisher",
            "AppStatusBarNotifier",
            "AppMessagePreferences",
            "AppPermission",
            "AppPermissionManager",
            "ActivityPermissionRequester",
            "PermissionManagerEntryPoint",
            "rememberAppPermissionRequester"
        )
        val offenders = productionKotlinFiles
            .filter { source -> forbiddenSymbols.any { it in source.readText() } }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Legacy message/permission references: $offenders", offenders.isEmpty())
    }

    @Test
    fun domainDoesNotEmitTelemetryOrUserMessages() {
        val forbidden = listOf("TelemetryEmitter", "AppNoticePublisher", "AppLog")
        val offenders = productionKotlinFiles
            .filter { "/domain/" in it.invariantSeparatorsPath }
            .filter { "/domain/notice/port/" !in it.invariantSeparatorsPath }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Domain side effects: $offenders", offenders.isEmpty())
    }

    @Test
    fun legacyAuthenticationStackCannotReturn() {
        val forbiddenSymbols = listOf(
            "VerificationGateway",
            "BiometricPromptLauncher",
            "BiometricAuthCoordinator",
            "UserSessionManager",
            "AppIdleMonitor"
        )
        val offenders = productionKotlinFiles
            .filter { source -> forbiddenSymbols.any { it in source.readText() } }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Legacy authentication references: $offenders", offenders.isEmpty())
    }

    @Test
    fun authenticationActivitiesStayInTheApplicationProcess() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(
            "Authentication hosts must not be moved to another process",
            "android:process" !in manifest
        )
    }

    @Test
    fun legacyAutofillAuthenticationReturnsTheMatchingPlatformType() {
        val activity = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillActivity.kt"
        ).readText()
        val viewModel = File(
            "src/main/java/com/aozijx/passly/feature/autofill/framework/" +
                    "AutofillFillViewModel.kt"
        ).readText()
        val responseFactory = File(
            "src/main/java/com/aozijx/passly/service/autofill/framework/builder/" +
                    "LegacyResponseFactory.kt"
        ).readText()

        assertTrue(
            "Dataset authentication must return a Dataset for immediate filling",
            "EXTRA_RETURN_DATASET" in activity &&
                    "AutofillAuthenticationPayload.DatasetResult(dataset)" in viewModel &&
                    "AutofillAuthenticationPayload.Response(" in viewModel
        )
        assertTrue(
            "The authentication Activity must accept both platform payload types",
            "is AutofillAuthenticationPayload.Response" in activity &&
                    "is AutofillAuthenticationPayload.DatasetResult" in activity &&
                    "EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET" in activity
        )
        assertTrue(
            "Autofill authentication PendingIntents must remain mutable for platform extras",
            "PendingIntent.FLAG_MUTABLE" in responseFactory &&
                    "PendingIntent.FLAG_IMMUTABLE" !in responseFactory
        )
    }

    @Test
    fun credentialProviderUsesSystemFinalRequestsAndKeepsPasskeysDisabled() {
        val responseViewModel = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseViewModel.kt"
        ).readText()
        val responseActivity = File(
            "src/main/java/com/aozijx/passly/feature/autofill/credential/" +
                    "CredentialResponseActivity.kt"
        ).readText()
        val pendingIntentFactory = File(
            "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                    "CredentialPendingIntentFactory.kt"
        ).readText()
        val createHandler = File(
            "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                    "CredentialBeginCreateHandler.kt"
        ).readText()
        val adapter = File(
            "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                    "CredentialPlatformAdapter.kt"
        ).readText()
        val responseUseCases = File(
            "src/main/java/com/aozijx/passly/domain/autofill/usecase/" +
                    "CredentialResponseUseCases.kt"
        ).readText()
        val providerConfig = File("src/main/res/xml/credential_service_config.xml").readText()

        assertTrue(
            "Final get/create phases must trust the system-injected provider request",
            "retrieveProviderGetCredentialRequest(sourceIntent)" in responseViewModel &&
                    "retrieveProviderCreateCredentialRequest(sourceIntent)" in responseViewModel &&
                    "EXTRA_PACKAGE_NAME" !in pendingIntentFactory
        )
        assertTrue(
            "Credential Manager valid exceptions must be returned with RESULT_OK",
            "UiState.Complete" in responseActivity &&
                    "setResult(RESULT_OK, state.resultIntent)" in responseActivity
        )
        assertTrue(
            "Password creation must complete the provider two-phase contract",
            "BeginCreatePasswordCredentialRequest" in createHandler &&
                    "createPasswordCreatePendingIntent" in createHandler &&
                    "CreatePasswordResponse" in File(
                "src/main/java/com/aozijx/passly/service/autofill/credential/" +
                        "CredentialResponseFactory.kt"
            ).readText()
        )
        assertTrue(
            "Allowed user ids must be checked before display and again before secret release",
            "option.allowedUserIds" in adapter &&
                    "allowedUserIds.isNotEmpty() && selected.username !in allowedUserIds" in
                    responseUseCases
        )
        assertTrue(
            "Passkey capability must stay unpublished until WebAuthn signing is implemented",
            "android.credentials.TYPE_PASSWORD_CREDENTIAL" in providerConfig &&
                    "TYPE_PUBLIC_KEY_CREDENTIAL" !in providerConfig
        )
    }

    @Test
    fun destructiveDatabaseRecoveryRequiresFreshAuthentication() {
        val mainViewModel = File(
            "src/main/java/com/aozijx/passly/feature/main/MainViewModel.kt"
        ).readText()
        val settingsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/SettingsViewModel.kt"
        ).readText()
        val recoveryDialog = File(
            "src/main/java/com/aozijx/passly/core/ui/components/DatabaseRecoveryDialog.kt"
        ).readText()
        val controller = File(
            "src/main/java/com/aozijx/passly/data/repository/database/DatabaseControllerImpl.kt"
        ).readText()
        val recoveryStore = File(
            "src/main/java/com/aozijx/passly/data/local/database/maintenance/" +
                "DatabaseRecoveryStore.kt"
        ).readText()
        val authenticationManager = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt"
        ).readText()
        val authPolicy = File(
            "src/main/java/com/aozijx/passly/domain/authentication/AuthenticationMethodPolicy.kt"
        ).readText()
        val credentialExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/CredentialMethodExecutor.kt"
        ).readText()
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val recoveryCompletion =
            sessionController.substringAfter("suspend fun completeDatabaseRecovery")

        assertTrue(
            "Crash recovery must request its non-destructive authentication purpose",
            "AuthenticationPurpose.RECOVER_DATABASE" in mainViewModel
        )
        assertTrue(
            "Permanent deletion must live behind authenticated settings",
            "AuthenticationPurpose.CLEAR_DATABASE" in settingsViewModel
        )
        assertTrue(
            "Crash dialog must not expose permanent database deletion",
            "onClearDatabase" !in recoveryDialog &&
                "database_recovery_clear_action" !in recoveryDialog
        )
        assertTrue(
            "Database recovery must preserve storage after sealing leases",
            controller.indexOf("sessionManager.seal()") <
                controller.indexOf("recoveryStore.preserveAndClearActiveVault()")
        )
        assertTrue(
            "Recovery storage must copy before clearing the active database",
            recoveryStore.indexOf("source.copyTo") <
                recoveryStore.indexOf("clearActiveDatabase(databaseFile)")
        )
        assertTrue(
            "Recovery code must remain available for damaged database recovery only",
            "AuthenticationPurpose.RECOVER_DATABASE" in authPolicy &&
                    "AuthenticationPurpose.RECOVER_DATABASE -> AuthenticationMethod.entries.toSet()" in
                    authPolicy
        )
        assertTrue(
            "Permanent database deletion must reject the recovery code",
            "AuthenticationPurpose.CLEAR_DATABASE -> PRIMARY_METHODS" in authPolicy ||
                    "CLEAR_DATABASE -> PRIMARY_METHODS" in authPolicy
        )
        assertTrue(
            "Cold-start recovery must stage the DEK without opening the broken database",
            "session.stageDatabaseRecovery(type, ownedDek)" in credentialExecutor
        )
        assertTrue(
            "A recovered database must open before authentication state is published",
            recoveryCompletion.indexOf("sessionManager.lockState != LockState.UNLOCKED") <
                recoveryCompletion.indexOf("markAuthenticatedInternal()")
        )
    }

    @Test
    fun biometricUnlockCannotPublishAuthenticatedBeforeDatabaseOpens() {
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val biometricExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/BiometricMethodExecutor.kt"
        ).readText()

        assertTrue(
            "Sealed biometric sessions must open the database",
            "if (lockLevel == VaultLockState.SEALED)" in sessionController &&
                "val err = sessionManager.unlock()" in sessionController
        )
        assertTrue(
            "Biometric authentication must fail when the database session cannot open",
            "if (session.markAuthenticated())" in biometricExecutor &&
                "AuthenticationFailureCode.SESSION_TRANSITION_FAILED" in biometricExecutor
        )
    }

    @Test
    fun recoveryDraftViewModelDependsOnlyOnAuthenticationContracts() {
        val source = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/RecoveryDraftViewModel.kt"
        ).readText()

        assertTrue(
            "Recovery draft UI must not access cryptographic implementations",
            "import com.aozijx.passly.security." !in source
        )
        assertTrue("Recovery draft must use its domain factory", "RecoveryCodeDraftFactory" in source)
    }

    @Test
    fun legacyLoggingStackCannotReturn() {
        val legacyRoots = listOf(
            File("src/main/java/com/aozijx/passly/core/log"),
            File("src/main/java/com/aozijx/passly/core/diagnostics")
        )
        val forbiddenSymbols = listOf(
            "core.log.Logcat",
            "core.log.LogExporter",
            "LogFilter",
            "AppLog",
            "core.diagnostics.DiagnosticsRuntime",
            "core.diagnostics.DiagnosticsPolicy",
            "PerFileEncryptedLogSink",
            "LogSanitizer"
        )
        val legacyReferences = productionKotlinFiles
            .filter { source ->
                val text = source.readText()
                forbiddenSymbols.any(text::contains)
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Legacy logging references: $legacyReferences", legacyReferences.isEmpty())
        assertTrue(
            "Legacy logging source directory must be removed",
            legacyRoots.all { root ->
                !root.exists() || root.walkTopDown().none { it.extension == "kt" }
            }
        )
    }

    @Test
    fun encryptedDiagnosticsUseBoundedQueueAndPreparedCrashKey() {
        val source = File(
            "src/main/java/com/aozijx/passly/data/diagnostics/EncryptedLogStore.kt"
        ).readText()
        val emergencyBlock = source
            .substringAfter("fun crashEmergencyWrite")
            .substringBefore("private fun fallbackEmergencyWrite")

        assertTrue("Log writer queue must remain bounded", "ArrayBlockingQueue" in source)
        assertTrue("Every diagnostics file needs an encrypted header", "writeHeader(file" in source)
        assertTrue(
            "Crash writer must use the preloaded key instead of Keystore",
            "getOrCreateWrappingKey" !in emergencyBlock
        )
        assertTrue(
            "Crash writer must not wait for the normal writer lock",
            "lockWrites = false" in emergencyBlock
        )
        assertTrue("Every record must authenticate sequence and level", "buildRecordAad" in source)
        assertTrue("Record nonce must have an exact length", "checkedExact(NONCE_BYTES)" in source)
    }

    @Test
    fun platformEffectsStayBehindTheirSingleGateway() {
        val directLogOffenders = productionKotlinFiles
            .filter {
                it.invariantSeparatorsPath
                    .endsWith("/core/telemetry/AndroidLogSink.kt")
                    .not()
            }
            .filter { "android.util.Log" in it.readText() || "printStackTrace(" in it.readText() }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()
        val directNotificationOffenders = productionKotlinFiles
            .filter {
                it.invariantSeparatorsPath
                    .endsWith("/app/message/system/AndroidSystemNotificationGateway.kt")
                    .not()
            }
            .filter {
                val text = it.readText()
                "NotificationManagerCompat" in text || "NotificationCompat." in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()
        val directPermissionOffenders = productionKotlinFiles
            .filter {
                val path = it.invariantSeparatorsPath
                !path.endsWith("/core/permission/catalog/RuntimePermissionCatalog.kt") &&
                    !path.endsWith("/core/permission/compose/PermissionRequestHost.kt")
            }
            .filter {
                val text = it.readText()
                "Manifest.permission." in text ||
                    "ActivityResultContracts.RequestPermission" in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Direct logging bypasses AndroidLogSink: $directLogOffenders", directLogOffenders.isEmpty())
        assertTrue(
            "Direct notification bypasses AndroidSystemNotificationGateway: $directNotificationOffenders",
            directNotificationOffenders.isEmpty()
        )
        assertTrue(
            "Direct runtime permission bypasses the permission center: $directPermissionOffenders",
            directPermissionOffenders.isEmpty()
        )
    }

    @Test
    fun newCentersContainNoStubOrTodoImplementation() {
        val scopedRoots = listOf(
            File("src/main/java/com/aozijx/passly/app/message"),
            File("src/main/java/com/aozijx/passly/app/permission"),
            File("src/main/java/com/aozijx/passly/app/diagnostics"),
            File("src/main/java/com/aozijx/passly/core/permission"),
            File("src/main/java/com/aozijx/passly/data/notice"),
            File("src/main/java/com/aozijx/passly/data/diagnostics"),
            File("src/main/java/com/aozijx/passly/domain/notice")
        )
        val offenders = scopedRoots.asSequence()
            .filter(File::exists)
            .flatMap { it.walkTopDown() }
            .filter { it.isFile && it.extension == "kt" }
            .filter {
                val text = it.readText()
                "TODO" in text || "FIXME" in text || "StubSystemNotificationGateway" in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue("Incomplete center implementations: $offenders", offenders.isEmpty())
    }

    @Test
    fun coreErrorDoesNotDependOnTelemetryOrMessageOrAppTelemetry() {
        val errorRoot = File("src/main/java/com/aozijx/passly/core/error")
        val forbidden = listOf(
            "import com.aozijx.passly.core.telemetry",
            "import com.aozijx.passly.app.diagnostics.AppTelemetry",
            "import com.aozijx.passly.app.message",
            "import com.aozijx.passly.domain.notice",
        )
        val offenders = errorRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(errorRoot).path }
            .toList()

        assertTrue(
            "core/error must not depend on telemetry, message, AppTelemetry: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun coreErrorDoesNotDependOnAndroid() {
        val errorRoot = File("src/main/java/com/aozijx/passly/core/error")
        val forbidden = listOf(
            "import android.",
            "import androidx.",
        )
        // boundary exceptions are allowed to use Android types since they wrap platform exceptions
        val exemptPaths = listOf("boundary" + File.separator)
        val offenders = errorRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { source -> exemptPaths.none { it in source.relativeTo(errorRoot).path } }
            .filter { source -> forbidden.any { it in source.readText() } }
            .map { it.relativeTo(errorRoot).path }
            .toList()

        assertTrue(
            "core/error model/result/mapping must not depend on Android: $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun errorMessagesLiveInAppMessageMappingNotCoreError() {
        val oldPresentationDir = File(
            "src/main/java/com/aozijx/passly/core/error/presentation"
        )
        val newMappingFile = File(
            "src/main/java/com/aozijx/passly/app/message/mapping/ErrorMessages.kt"
        )

        assertTrue(
            "ErrorMessages must not remain in core/error/presentation",
            !oldPresentationDir.exists() || oldPresentationDir.walkTopDown()
                .none { it.extension == "kt" }
        )
        assertTrue(
            "ErrorMessages must live in app/message/mapping",
            newMappingFile.exists()
        )
    }

    @Test
    fun recoveryCodeCannotActAsEverydayUnlockMethod() {
        val authPolicy = File(
            "src/main/java/com/aozijx/passly/domain/authentication/AuthenticationMethodPolicy.kt"
        ).readText()
        val authModels = File(
            "src/main/java/com/aozijx/passly/domain/authentication/AuthenticationModels.kt"
        ).readText()
        val authManager = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt"
        ).readText()
        val sessionController = File(
            "src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt"
        ).readText()
        val credentialExecutor = File(
            "src/main/java/com/aozijx/passly/security/authentication/CredentialMethodExecutor.kt"
        ).readText()
        val provisioner = File(
            "src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationMethodProvisioner.kt"
        ).readText()
        val provisionerInterface = File(
            "src/main/java/com/aozijx/passly/domain/authentication/AuthenticationMethodProvisioner.kt"
        ).readText()
        val authViewModel = File(
            "src/main/java/com/aozijx/passly/feature/auth/presentation/AuthenticationViewModel.kt"
        ).readText()
        val mainViewModel = File(
            "src/main/java/com/aozijx/passly/feature/main/MainViewModel.kt"
        ).readText()
        val mainUiState = File(
            "src/main/java/com/aozijx/passly/feature/main/contract/MainUiState.kt"
        ).readText()
        val mainScreen = File(
            "src/main/java/com/aozijx/passly/feature/main/ui/MainScreen.kt"
        ).readText()
        val recoveryModeScreen = File(
            "src/main/java/com/aozijx/passly/feature/recovery/RecoveryModeScreen.kt"
        ).readText()
        val backupViewModel = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/BackupViewModel.kt"
        ).readText()
        val backupSessionPolicy = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/BackupSessionPolicy.kt"
        ).readText()
        val backupCoordinator = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation/BackupOperationCoordinator.kt"
        ).readText()
        val securityViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/security/SecuritySettingsViewModel.kt"
        ).readText()
        val transactionRunner = File(
            "src/main/java/com/aozijx/passly/data/repository/VaultTransactionRunner.kt"
        ).readText()
        val entryQueryRepository = File(
            "src/main/java/com/aozijx/passly/data/repository/entry/RoomEntryQueryRepository.kt"
        ).readText()
        val otpConfigRepository = File(
            "src/main/java/com/aozijx/passly/data/repository/otp/RoomOtpConfigRepository.kt"
        ).readText()
        val attachmentRepository = File(
            "src/main/java/com/aozijx/passly/data/repository/attachment/FileBackedAttachmentRepository.kt"
        ).readText()
        val vaultViewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/VaultViewModel.kt"
        ).readText()
        val vaultAccessPolicy = File(
            "src/main/java/com/aozijx/passly/feature/vault/VaultAccessPolicy.kt"
        ).readText()
        val createEntryViewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/editor/common/CreateEntryViewModel.kt"
        ).readText()
        val detailViewModel = File(
            "src/main/java/com/aozijx/passly/feature/detail/DetailViewModel.kt"
        ).readText()
        val detailAccessPolicy = File(
            "src/main/java/com/aozijx/passly/feature/detail/DetailAccessPolicy.kt"
        ).readText()
        val recoveryModeViewModel = File(
            "src/main/java/com/aozijx/passly/feature/recovery/RecoveryModeViewModel.kt"
        ).readText()
        val dataViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/datamanagement/DataManagementSettingsViewModel.kt"
        ).readText()
        val diagnosticsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/general/DiagnosticsSettingsViewModel.kt"
        ).readText()

        // 1. UNLOCK_VAULT must not allow recovery code
        assertTrue(
            "UNLOCK_VAULT must use PRIMARY_METHODS only",
            "UNLOCK_VAULT," in authPolicy && "CLEAR_DATABASE -> PRIMARY_METHODS" in authPolicy
        )

        // 2. Recovery mode state must exist
        assertTrue(
            "RecoveryMode state must be defined",
            "data class RecoveryMode" in authModels
        )
        assertTrue(
            "MainUiState must track recovery mode",
            "isRecoveryMode: Boolean" in mainUiState
        )
        assertTrue(
            "MainScreen must render RecoveryModeScreen",
            "RecoveryModeScreen(" in mainScreen
        )

        // 3. VaultSessionController must support recovery unlock
        assertTrue(
            "commitRecoveryUnlock must exist",
            "fun commitRecoveryUnlock" in sessionController
        )
        assertTrue(
            "markRecoveryMode must exist",
            "fun markRecoveryMode" in sessionController
        )
        assertTrue(
            "isRecoveryMode must check RecoveryMode state",
            "is AuthenticationState.RecoveryMode" in sessionController
        )

        // 4. CredentialMethodExecutor must route recovery purposes
        assertTrue(
            "RECOVER_AUTH_METHODS must call commitRecoveryUnlock",
            "AuthenticationPurpose.RECOVER_AUTH_METHODS" in credentialExecutor &&
                    "commitRecoveryUnlock" in credentialExecutor
        )
        assertTrue(
            "RECOVERY_EXPORT must call commitRecoveryUnlock",
            "AuthenticationPurpose.RECOVERY_EXPORT" in credentialExecutor &&
                    "commitRecoveryUnlock" in credentialExecutor
        )

        // 5. AuthenticationManager must gate recovery mode purposes
        assertTrue(
            "AuthenticationManager must check RECOVERY_MODE_PURPOSES",
            "RECOVERY_MODE_PURPOSES" in authManager
        )
        assertTrue(
            "AuthenticationManager must allow recovery mode reuse",
            "RECOVERY_MODE_REUSABLE_PURPOSES" in authManager
        )

        // 6. Provisioner must not allow recovery code as substitute for primary methods
        assertTrue(
            "disableAppPassword must use hasAlternativePrimaryFactor",
            "availabilityResolver.hasAlternativePrimaryFactor" in provisioner &&
                    "EnvelopeType.APP_PASSWORD" in provisioner
        )
        assertTrue(
            "disableBiometric must use hasAlternativePrimaryFactor",
            "availabilityResolver.hasAlternativePrimaryFactor" in provisioner &&
                    "EnvelopeType.BIOMETRIC" in provisioner
        )

        // 7. checkRecoveryCode must not produce authentication success
        assertTrue(
            "checkRecoveryCode must exist in interface",
            "suspend fun checkRecoveryCode" in provisionerInterface
        )
        assertTrue(
            "SecuritySettingsViewModel must use checkRecoveryCode",
            "methodProvisioner.checkRecoveryCode" in securityViewModel
        )

        // 8. Recovery code unlock must use RECOVER_AUTH_METHODS purpose
        assertTrue(
            "unlockWithRecoveryCode must use RECOVER_AUTH_METHODS",
            "AuthenticationPurpose.RECOVER_AUTH_METHODS" in authViewModel
        )

        // 9. Recovery mode exit must lock with RECOVERY_EXIT
        assertTrue(
            "ExitRecovery must lock with RECOVERY_EXIT",
            "LockReason.RECOVERY_EXIT" in mainViewModel
        )

        // 10. Backup flow must support recovery export
        assertTrue(
            "Backup coordinator must use RECOVERY_EXPORT purpose",
            "AuthenticationPurpose.RECOVERY_EXPORT" in backupCoordinator
        )
        assertTrue(
            "Backup coordinator must preserve the recovery export boundary",
            "isRecoveryExport" in backupCoordinator
        )

        // 11. Plain Vault repositories must not treat an open recovery database as full access.
        assertTrue(
            "VaultTransactionRunner must require full Vault access before write/read",
            "hasFullVaultAccess()" in transactionRunner &&
                    "SessionModeRestricted" in transactionRunner
        )
        assertTrue(
            "Entry query repository must gate normal and high-sensitivity reads",
            entryQueryRepository.split("hasFullVaultAccess()").size - 1 >= 4
        )
        assertTrue(
            "OTP config repository must gate secret reads",
            "hasFullVaultAccess()" in otpConfigRepository
        )
        assertTrue(
            "Attachment repository must gate metadata and file writes",
            attachmentRepository.split("hasFullVaultAccess()").size - 1 >= 3 &&
                    "SessionModeRestricted" in attachmentRepository
        )

        // 12. Sensitive ViewModels must express their session-mode boundary explicitly.
        assertTrue(
            "VaultViewModel must gate normal Vault actions",
            "VaultAccessPolicy" in vaultViewModel &&
                    "VaultAccessState" !in vaultViewModel &&
                    "hasFullVaultAccess()" in vaultAccessPolicy &&
                    vaultViewModel.split("hasFullAccess()").size - 1 >= 4
        )
        assertTrue(
            "CreateEntryViewModel must gate entry creation",
            "VaultAccessState" in createEntryViewModel &&
                    "hasFullVaultAccess()" in createEntryViewModel
        )
        assertTrue(
            "DetailViewModel must gate detail reads and reveal actions",
            "DetailAccessPolicy" in detailViewModel &&
                    "VaultAccessState" !in detailViewModel &&
                    "hasFullVaultAccess()" in detailAccessPolicy &&
                    detailViewModel.split("hasFullAccess()").size - 1 >= 4
        )
        assertTrue(
            "RecoveryModeViewModel must only run in RecoveryMode",
            "AuthenticationState.RecoveryMode" in recoveryModeViewModel &&
                    "ensureRecoveryMode" in recoveryModeViewModel
        )
        assertTrue(
            "Recovery password reset must seal recovery session instead of promoting it",
            "wasRecoveryMode" in provisioner &&
                    "LockReason.RECOVERY_EXIT" in provisioner &&
                    "session.lock(LockReason.RECOVERY_EXIT)" in provisioner
        )
        assertTrue(
            "markAuthenticated must not promote RecoveryMode to full Vault access",
            "Recovery mode must not be promoted" in sessionController &&
                    "markAuthenticatedInternal()" !in sessionController.substringAfter(
                        "if (_state.value is AuthenticationState.RecoveryMode)"
                    ).substringBefore("} else")
        )
        assertTrue(
            "BackupViewModel must gate normal backup versus recovery export",
            "BackupSessionPolicy" in backupViewModel &&
                    "VaultAccessState" !in backupViewModel &&
                    "hasFullVaultAccess()" in backupSessionPolicy &&
                    "isRecoveryMode()" in backupSessionPolicy
        )
        assertTrue(
            "DataManagementSettingsViewModel must gate trash operations",
            "VaultAccessState" in dataViewModel &&
                    "hasFullVaultAccess()" in dataViewModel
        )
        assertTrue(
            "DiagnosticsSettingsViewModel must not expose diagnostics outside full auth",
            "AuthenticationState.Authenticated" in diagnosticsViewModel
        )
    }

    // ============================================================
    // 阶段 0 — 护栏扩展：跨 Feature 依赖、MVI 入口、文件大小
    // ============================================================

    @Test
    fun featureDependenciesUseApprovedPublicApisAndStayAcyclic() {
        // main 是当前 App 导航组合根，允许导入其他 feature 进行组装。
        // 其他 feature 只能通过目标 feature 的 api 包形成显式、有向依赖。
        val compositionRoot = setOf("main")
        val allowedDependencies = setOf(
            "settings" to "backup",
            "recovery" to "backup",
        )
        val featureRoots = File("src/main/java/com/aozijx/passly/feature")
            .listFiles(File::isDirectory) ?: emptyArray()
        val offenders = mutableListOf<String>()
        val dependencies = mutableMapOf<String, MutableSet<String>>()
        val featureImport = Regex(
            """import com\.aozijx\.passly\.feature\.([^.\s]+)\.([\w.]+)"""
        )

        for (featureDir in featureRoots) {
            if (featureDir.name in compositionRoot) continue
            featureDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { source ->
                    featureImport.findAll(source.readText()).forEach { match ->
                        val targetFeature = match.groupValues[1]
                        if (targetFeature != featureDir.name) {
                            dependencies.getOrPut(featureDir.name) { mutableSetOf() }
                                .add(targetFeature)
                            val importedMember = match.groupValues[2]
                            val throughPublicApi =
                                importedMember == "api" || importedMember.startsWith("api.")
                            val allowedEdge =
                                featureDir.name to targetFeature in allowedDependencies
                            if (!throughPublicApi || !allowedEdge) {
                                offenders += buildString {
                                    append(source.relativeTo(featureDir).path)
                                    append(": ")
                                    append(match.value)
                                }
                            }
                        }
                    }
                }
        }

        assertTrue(
            "Feature cross-imports must use an explicitly allowed api edge: $offenders",
            offenders.isEmpty()
        )

        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun containsCycle(feature: String): Boolean {
            if (feature in visiting) return true
            if (!visited.add(feature)) return false
            visiting += feature
            val cyclic = dependencies[feature].orEmpty().any(::containsCycle)
            visiting -= feature
            return cyclic
        }

        val cyclicRoots = featureRoots
            .map(File::getName)
            .filter(::containsCycle)

        assertTrue(
            "Feature dependency graph must stay acyclic: $dependencies",
            cyclicRoots.isEmpty()
        )
    }

    @Test
    fun coreDoesNotOwnBackupFeaturePresentation() {
        val leakedPresentationFiles = listOf(
            "BackupViewModel.kt",
            "BackupAction.kt",
            "BackupUiState.kt",
            "BackupSessionPolicy.kt",
            "BackupReducer.kt",
            "BackupOperationCoordinator.kt",
        ).map { name ->
            File("src/main/java/com/aozijx/passly/core/backup/$name")
        }
        val leakedBackupUi = File(
            "src/main/java/com/aozijx/passly/core/ui/components/backup"
        )

        assertTrue(
            "Core must not own Backup feature presentation/contracts",
            leakedPresentationFiles.none(File::exists)
        )
        assertTrue(
            "Backup-specific UI belongs to feature/backup, not core/ui",
            !leakedBackupUi.exists() || leakedBackupUi.walkTopDown().none {
                it.isFile && it.extension == "kt"
            }
        )
    }

    @Test
    fun backupMviSeparatesStateReductionFromSideEffects() {
        val presentationRoot = File(
            "src/main/java/com/aozijx/passly/feature/backup/internal/presentation"
        )
        val viewModel = File(presentationRoot, "BackupViewModel.kt").readText()
        val reducer = File(presentationRoot, "BackupReducer.kt")
        val coordinator = File(presentationRoot, "BackupOperationCoordinator.kt")
        val forbiddenViewModelDependencies = listOf(
            "AppSettingsRepository",
            "AuthenticationManager",
            "BackupStorageSupport",
            "VaultBackupService",
        ).filter { dependency -> dependency in viewModel }

        assertTrue("Backup must have a dedicated pure reducer", reducer.exists())
        assertTrue("Backup must isolate side effects in a coordinator", coordinator.exists())
        assertTrue(
            "BackupViewModel must not directly orchestrate infrastructure: " +
                    forbiddenViewModelDependencies,
            forbiddenViewModelDependencies.isEmpty(),
        )
        assertTrue(
            "BackupViewModel must reduce mutations through BackupReducer",
            "BackupReducer.reduce" in viewModel,
        )
    }

    @Test
    fun mviViewModelsHaveOneActionEntryPoint() {
        // 只检查有对应 Intent/Action 合约文件的 ViewModel（完整 MVI 页面）。
        // 简单 UDF 页面（仅有 UiState）不强制统一事件入口。
        // 兼容迁移中的 onIntent/handleIntent，新代码使用 onAction。
        val offenders = productionKotlinFiles
            .filter { "/feature/" in it.invariantSeparatorsPath }
            .filter { it.name.endsWith("ViewModel.kt") }
            .filter { viewModelFile ->
                // 在 feature 目录树中查找对应的事件合约文件
                // 从 feature 根目录开始搜索，确保找到 contract/ 子目录下的文件
                val vmName = viewModelFile.name.removeSuffix("ViewModel.kt")
                val featureRoot = findFeatureRoot(viewModelFile)
                featureRoot != null && featureRoot.walkTopDown()
                    .any { f ->
                        f.isFile && f.extension == "kt" &&
                                (
                                        f.name == "${vmName}Intent.kt" ||
                                                f.name == "${vmName}Action.kt" ||
                                                f.name == "${vmName}UiAction.kt"
                                        )
                    }
            }
            .filter { viewModelFile ->
                val text = viewModelFile.readText()
                "fun onAction(" !in text &&
                        "fun onIntent(" !in text &&
                        "fun handleIntent(" !in text
            }
            .map { it.relativeTo(File("src/main/java")).path }
            .toList()

        assertTrue(
            "MVI ViewModels must expose one action/intent entry point: $offenders",
            offenders.isEmpty()
        )
    }

    /**
     * 从 ViewModel 文件向上查找 feature 根目录（feature/<name>/ 的直接父目录）。
     * 返回 feature 根目录，例如 feature/main/。
     */
    private fun findFeatureRoot(viewModelFile: File): File? {
        val featureBase = File("src/main/java/com/aozijx/passly/feature")
        var current: File? = viewModelFile.parentFile
        while (current != null && current.canonicalPath != featureBase.canonicalPath) {
            if (current.parentFile?.canonicalPath == featureBase.canonicalPath) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    @Test
    fun productionFilesDoNotExceedReasonableSize() {
        val maxLines = 500
        val offenders = productionKotlinFiles
            .mapNotNull { file ->
                val lineCount = file.useLines { it.count() }
                if (lineCount > maxLines) {
                    "${file.relativeTo(File("src/main/java")).path} ($lineCount lines)"
                } else {
                    null
                }
            }
            .toList()

        assertTrue(
            "Files exceeding $maxLines lines (should be split by responsibility): $offenders",
            offenders.isEmpty()
        )
    }

    @Test
    fun vaultFeatureHasExplicitIntentContract() {
        val vaultIntentFile = File(
            "src/main/java/com/aozijx/passly/feature/vault/contract/VaultIntent.kt"
        )
        val vaultViewModel = File(
            "src/main/java/com/aozijx/passly/feature/vault/VaultViewModel.kt"
        ).readText()

        assertTrue(
            "Vault feature must have VaultIntent.kt contract",
            vaultIntentFile.exists()
        )
        assertTrue(
            "VaultViewModel must use onIntent as single entry point",
            "fun onIntent(" in vaultViewModel
        )
    }

    @Test
    fun settingsViewModelHasExplicitIntentContract() {
        val settingsViewModel = File(
            "src/main/java/com/aozijx/passly/feature/settings/SettingsViewModel.kt"
        )
        if (settingsViewModel.exists()) {
            val text = settingsViewModel.readText()
            val hasIntentContract = File(
                "src/main/java/com/aozijx/passly/feature/settings/contract/SettingsIntent.kt"
            ).exists()

            assertTrue(
                "Settings feature must have SettingsIntent.kt contract",
                hasIntentContract
            )
            assertTrue(
                "SettingsViewModel must use onIntent/handleIntent as entry point",
                "fun onIntent(" in text || "fun handleIntent(" in text
            )
        }
    }
}
