package passly

internal data class EditorSource(
    val path: String,
    val content: String,
)

internal object EditorSourceBoundaryVerifier {
    fun verify(sources: List<EditorSource>): List<String> = buildList {
        addAll(
            SourceBoundaryVerifier.verify(sources, SourceBoundaryPolicy.generalRules)
                .map(SourceBoundaryViolation::format),
        )
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
            if (isSettingsFeature && !isSettingsHost && (
                passiveUiNames.any(fileName::contains) || isSettingsUiSection ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add("$path: passive settings UI must live below presentation/ui/settings")
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
                SourceBoundaryPolicy.editorMapperForbiddenMarkers.forEach { (label, markers) ->
                    if (markers.any(source.content::contains)) {
                        add("$path: FormMapper cannot depend on $label")
                    }
                }
            }
        }
    }
}
