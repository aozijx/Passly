package passly

internal object SourceBoundaryPolicy {
    val editorMapperForbiddenMarkers = linkedMapOf(
        "repository" to listOf(".repository.", ".port.EntryCommandRepository"),
        "UUID" to listOf("UuidCreator", "java.util.UUID"),
        "clock" to listOf("System.currentTimeMillis", "System.nanoTime", ".Clock"),
        "codec" to listOf("Codec", ".codec."),
        "crypto" to listOf(".crypto.", "Cipher"),
        "EntryLink" to listOf("EntryLink"),
    )

    val generalRules = listOf(
        SourceBoundaryRule(
            id = "PRESENTATION_UI_IMPORT",
            sourcePathContains = "/presentation/ui/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.presentation.feature.",
                "com.aozijx.passly.feature.",
                "com.aozijx.passly.domain.",
                "com.aozijx.passly.data.",
            ),
            message = "presentation UI imports a forbidden project layer",
        ),
        SourceBoundaryRule(
            id = "PRESENTATION_UI_VIEW_MODEL",
            sourcePathContains = "/presentation/ui/",
            forbiddenContentMarkers = setOf("ViewModel", "hiltViewModel", "viewModel("),
            message = "presentation UI cannot own or look up a ViewModel",
        ),
        SourceBoundaryRule(
            id = "EDITOR_DATA_IMPORT",
            sourcePathContains = "/presentation/feature/vault/editor/",
            forbiddenImportPrefixes = setOf("com.aozijx.passly.data."),
            message = "presentation editor imports a data implementation",
        ),
        *listOf(
            "/domain/src/",
            "/data/src/",
            "/core/src/",
            "/core/common/src/",
            "/runtime/session/src/",
        ).map { moduleRoot ->
            SourceBoundaryRule(
                id = "LOWER_MODULE_EDITOR_IMPORT",
                sourcePathContains = moduleRoot,
                forbiddenImportPrefixes = setOf(
                    "com.aozijx.passly.presentation.feature.vault.editor.",
                ),
                message = "lower module imports presentation editor state",
            )
        }.toTypedArray(),
    )
}
