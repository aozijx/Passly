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
            val isPresentationEditor = "/feature/vault/presentation/editor/" in lowerPath

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
                val isVaultPageFeature = "/feature/vault/presentation/$page/" in lowerPath
                val isFeatureUi = "/feature/vault/presentation/$page/ui/" in lowerPath
                val isFeatureHost = fileName.endsWith("host.kt")
                val isUiSection = fileName.endsWith("section.kt")
                if (isVaultPageFeature && !isFeatureUi && !isFeatureHost && (
                    passiveUiFileSuffixes.any(fileName::endsWith) || isUiSection ||
                            listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                        )
                ) {
                    val expectedRoot = "feature/vault/presentation/$page/ui"
                    add("$path: passive vault-$page UI must live below $expectedRoot")
                }
            }
            val isScannerFeature = "/feature/scanner/presentation/" in lowerPath
            val isScannerUi = "/feature/scanner/presentation/ui/" in lowerPath
            val isScannerHost = fileName.endsWith("host.kt")
            if (isScannerFeature && !isScannerUi && !isScannerHost && (
                passiveUiFileSuffixes.any(fileName::endsWith) ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add(
                    "$path: passive scanner UI must live below " +
                        "feature/scanner/presentation/ui",
                )
            }
            val isBackupFeature = "/feature/backup/presentation/" in lowerPath
            val isBackupUi = "/feature/backup/presentation/ui/" in lowerPath
            if (isBackupFeature && !isBackupUi && (
                passiveUiFileSuffixes.any(fileName::endsWith) ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add(
                    "$path: passive backup UI must live below " +
                        "feature/backup/presentation/ui",
                )
            }
            val isSettingsFeature = "/feature/settings/presentation/" in lowerPath
            val isSettingsUi = "/feature/settings/presentation/ui/" in lowerPath
            val isSettingsHost = fileName.endsWith("host.kt")
            val isSettingsUiSection = fileName.endsWith("section.kt")
            if (isSettingsFeature && !isSettingsUi && !isSettingsHost && (
                passiveUiFileSuffixes.any(fileName::endsWith) || isSettingsUiSection ||
                    listOf("/component/", "/dialog/", "/sheet/").any(lowerPath::contains)
                )
            ) {
                add("$path: passive settings UI must live below feature/settings/presentation/ui")
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
