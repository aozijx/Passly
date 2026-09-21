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

            val fileName = lowerPath.substringAfterLast('/')
            val passiveUiFileSuffixes = listOf(
                "screen.kt",
                "content.kt",
                "component.kt",
                "dialog.kt",
                "sheet.kt",
                "sheets.kt",
                "scaffold.kt",
            )
            listOf("list", "detail", "editor").forEach { page ->
                val isVaultPageFeature = "/presentation/feature/vault/$page/" in lowerPath
                val isFeatureUi = "/presentation/feature/vault/$page/ui/" in lowerPath
                val isFeatureHost = fileName.endsWith("host.kt")
                val isUiSection = fileName.endsWith("section.kt")
                if (isVaultPageFeature && !isFeatureUi && !isFeatureHost && (
                    passiveUiFileSuffixes.any(fileName::endsWith) || isUiSection ||
                            listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                        )
                ) {
                    val expectedRoot = "presentation/feature/vault/$page/ui"
                    add("$path: passive vault-$page UI must live below $expectedRoot")
                }
            }
            val isScannerFeature = "/presentation/feature/scanner/" in lowerPath
            val isScannerUi = "/presentation/feature/scanner/ui/" in lowerPath
            val isScannerHost = fileName.endsWith("host.kt")
            if (isScannerFeature && !isScannerUi && !isScannerHost && (
                passiveUiFileSuffixes.any(fileName::endsWith) ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add(
                    "$path: passive scanner UI must live below " +
                        "presentation/feature/scanner/ui",
                )
            }
            val isBackupFeature = "/presentation/feature/backup/" in lowerPath
            val isBackupUi = "/presentation/feature/backup/ui/" in lowerPath
            if (isBackupFeature && !isBackupUi && (
                passiveUiFileSuffixes.any(fileName::endsWith) ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add(
                    "$path: passive backup UI must live below " +
                        "presentation/feature/backup/ui",
                )
            }
            val isSettingsFeature = "/presentation/feature/settings/" in lowerPath
            val isSettingsUi = "/presentation/feature/settings/ui/" in lowerPath
            val isSettingsHost = fileName.endsWith("host.kt")
            val isSettingsUiSection = fileName.endsWith("section.kt")
            if (isSettingsFeature && !isSettingsUi && !isSettingsHost && (
                passiveUiFileSuffixes.any(fileName::endsWith) || isSettingsUiSection ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add("$path: passive settings UI must live below presentation/feature/settings/ui")
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
