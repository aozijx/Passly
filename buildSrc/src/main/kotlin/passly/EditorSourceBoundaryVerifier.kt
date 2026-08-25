package passly

internal data class EditorSource(
    val path: String,
    val content: String,
)

internal object EditorSourceBoundaryVerifier {
    private val temporarySettingsUiAllowlist = setOf(
        "/presentation/feature/settings/backup/component/backuprestoredetail.kt",
        "/presentation/feature/settings/backup/component/backuprestoresettingssection.kt",
        "/presentation/feature/settings/backup/component/backuprestoresheets.kt",
        "/presentation/feature/settings/backup/component/databaserecoverysheet.kt",
        "/presentation/feature/settings/backup/component/datamanagementdetail.kt",
        "/presentation/feature/settings/backup/component/datasettingssection.kt",
        "/presentation/feature/settings/main/settingsscreendialogs.kt",
        "/presentation/feature/settings/main/settingsscreenlocalstate.kt",
        "/presentation/feature/settings/main/settingsscreenstatebuilders.kt",
        "/presentation/feature/settings/main/component/settingsdialogmodels.kt",
        "/presentation/feature/settings/main/general/generalsection.kt",
        "/presentation/feature/settings/main/general/logsettingssection.kt",
        "/presentation/feature/settings/main/general/notificationsettingssection.kt",
        "/presentation/feature/settings/main/interaction/swipeactionselectdialog.kt",
        "/presentation/feature/settings/main/interaction/swipegesturesettingssection.kt",
        "/presentation/feature/settings/security/component/recoverycodedetail.kt",
    )
    private val mapperForbiddenMarkers = linkedMapOf(
        "repository" to listOf(".repository.", ".port.EntryCommandRepository"),
        "UUID" to listOf("UuidCreator", "java.util.UUID"),
        "clock" to listOf("System.currentTimeMillis", "System.nanoTime", ".Clock"),
        "codec" to listOf("Codec", ".codec."),
        "crypto" to listOf(".crypto.", "Cipher"),
        "EntryLink" to listOf("EntryLink"),
    )

    fun verify(sources: List<EditorSource>): List<String> = buildList {
        sources.forEach { source ->
            val path = source.path.replace('\\', '/')
            val lowerPath = path.lowercase()
            val isPresentationEditor = "/presentation/feature/vault/editor/" in lowerPath

            val migratedUiDirectoryMarkers = listOf(
                "/feature/auth/",
                "/feature/detail/",
                "/feature/recovery/",
                "/feature/scanner/",
                "/feature/settings/",
                "/feature/vault/presentation/",
                "/presentation/settings/",
                "/app/navigation/",
                "/app/shell/contract/",
                "/app/shell/presentation/",
                "/app/shell/ui/",
            )
            val migratedUiFileNames = setOf(
                "appshellsettingscontract.kt",
                "appshellsettingsviewmodel.kt",
                "appshellviewmodel.kt",
                "autofillfillactivity.kt",
                "autofillfillcontract.kt",
                "autofillfillreducer.kt",
                "autofillfilluiaction.kt",
                "autofillfillviewmodel.kt",
                "backupfeature.kt",
                "backupreducer.kt",
                "backupuiaction.kt",
                "backupuistate.kt",
                "backupviewmodel.kt",
                "credentialresponseactivity.kt",
                "credentialresponsecontract.kt",
                "credentialresponsereducer.kt",
                "credentialresponseuiaction.kt",
                "credentialresponseviewmodel.kt",
            )
            val fileName = lowerPath.substringAfterLast('/')
            val passiveUiNames = listOf("screen", "content", "component", "dialog", "sheet")
            listOf("list", "detail").forEach { page ->
                val isVaultPageFeature = "/presentation/feature/vault/$page/" in lowerPath
                val isFeatureHost = fileName.endsWith("host.kt")
                val isUiSection = fileName.endsWith("section.kt")
                if (isVaultPageFeature && !isFeatureHost && (
                    passiveUiNames.any(fileName::contains) || isUiSection ||
                            listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                        )
                ) {
                    add("$path: passive vault-$page UI must live below presentation/ui/vault/$page")
                }
            }
            val isSettingsFeature = "/presentation/feature/settings/" in lowerPath
            val isSettingsHost = fileName.endsWith("host.kt")
            val isSettingsUiSection = fileName.endsWith("section.kt")
            val isTemporarilyAllowedSettingsUi = temporarySettingsUiAllowlist.any(lowerPath::endsWith)
            if (isSettingsFeature && !isSettingsHost && !isTemporarilyAllowedSettingsUi && (
                passiveUiNames.any(fileName::contains) || isSettingsUiSection ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add("$path: passive settings UI must live below presentation/ui/settings")
            }
            val isPresentationUi = "/presentation/ui/" in lowerPath
            if (isPresentationUi) {
                val forbiddenUiImports = listOf(
                    "import com.aozijx.passly.presentation.feature.",
                    "import com.aozijx.passly.feature.",
                    "import com.aozijx.passly.domain.",
                    "import com.aozijx.passly.data.",
                )
                if (forbiddenUiImports.any(source.content::contains)) {
                    add("$path: presentation UI imports a forbidden project layer")
                }
                if ("ViewModel" in source.content || "hiltViewModel" in source.content) {
                    add("$path: presentation UI cannot own or look up a ViewModel")
                }
            }
            val isOutsidePresentationFeature = "/presentation/feature/" !in lowerPath
            val isLegacyBackupPresentation =
                isOutsidePresentationFeature && (
                "/feature/backup/presentation/" in lowerPath ||
                    ("/feature/backup/internal/presentation/" in lowerPath && fileName in migratedUiFileNames)
                )
            val isLegacyAutofillPresentation =
                isOutsidePresentationFeature &&
                ("/feature/autofill/credential/" in lowerPath ||
                    "/feature/autofill/legacy/" in lowerPath) &&
                    fileName in migratedUiFileNames
            val isLegacyShellPresentation =
                isOutsidePresentationFeature &&
                    "/app/shell/" in lowerPath && fileName in migratedUiFileNames
            if ((isOutsidePresentationFeature && migratedUiDirectoryMarkers.any(lowerPath::contains)) ||
                isLegacyBackupPresentation ||
                isLegacyAutofillPresentation ||
                isLegacyShellPresentation
            ) {
                add("$path: migrated UI must live below presentation/feature")
            }

            if (isPresentationEditor && "import com.aozijx.passly.data." in source.content) {
                add("$path: presentation editor imports a data implementation")
            }

            val isLowerModule = lowerPath.startsWith("domain/") ||
                lowerPath.startsWith("data/") ||
                lowerPath.startsWith("core/")
            if (isLowerModule && "import com.aozijx.passly.presentation.feature.vault.editor." in source.content) {
                add("$path: lower module imports presentation editor state")
            }

            if ("/vault/editor/" in lowerPath && lowerPath.endsWith("entryfactory.kt")) {
                add("$path: vault editor EntryFactory is forbidden")
            }

            if (lowerPath.endsWith("/feature/vault/model/otpformstate.kt")) {
                add("$path: legacy OtpFormState location is forbidden")
            }

            if ("/presentation/vault/editor/" in lowerPath ||
                "com.aozijx.passly.presentation.vault.editor" in source.content
            ) {
                add("$path: legacy presentation editor package is forbidden")
            }

            if (isPresentationEditor && lowerPath.endsWith("formmapper.kt")) {
                mapperForbiddenMarkers.forEach { (label, markers) ->
                    if (markers.any(source.content::contains)) {
                        add("$path: FormMapper cannot depend on $label")
                    }
                }
            }
        }
    }
}
