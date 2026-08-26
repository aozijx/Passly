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
            id = "PRESENTATION_FEATURE_DATA_IMPORT",
            sourcePathContains = "/presentation/feature/vault/",
            forbiddenImportPrefixes = setOf("com.aozijx.passly.data."),
            message = "presentation feature imports a data implementation",
        ),
        SourceBoundaryRule(
            id = "VAULT_DATA_ADAPTER_ONLY",
            sourcePathContains = "/app/src/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.data.repository.entry.paging.EntryPagingStore",
                "com.aozijx.passly.data.local.database.port.EntryDataRefreshNotifier",
            ),
            allowedSourcePathContains = setOf(
                "/app/entry/paging/DataVaultEntryPageSource.kt",
                "/app/entry/paging/DataVaultDataChangeSignal.kt",
                "/presentation/feature/",
                "/feature/vault/entry/VaultDataAdaptersTest.kt",
            ),
            message = "vault paging Data types may only be imported by App adapters",
        ),
        SourceBoundaryRule(
            id = "DATABASE_RECOVERY_DATA_ADAPTER_ONLY",
            sourcePathContains = "/app/src/",
            forbiddenImportPrefixes = setOf(
                "com.aozijx.passly.data.local.database.model.DatabaseRecovery",
                "com.aozijx.passly.data.local.database.port.DatabaseRecoveryRepository",
            ),
            allowedSourcePathContains = setOf(
                "/app/database/recovery/DataDatabaseRecoveryGateway.kt",
                "/app/database/recovery/DataDatabaseRecoveryGatewayTest.kt",
            ),
            message = "database recovery Data types may only be imported by the App adapter",
        ),
        SourceBoundaryRule(
            id = "FEATURE_PRESENTATION_IMPORT",
            sourcePathContains = "/app/src/main/java/com/aozijx/passly/feature/",
            forbiddenImportPrefixes = setOf("com.aozijx.passly.presentation."),
            allowedSourcePathContains = setOf(
                "/feature/autofill/credential/service/CredentialPendingIntentFactory.kt",
                "/feature/autofill/legacy/AutofillPendingIntentFactory.kt",
            ),
            message = "feature implementation imports presentation",
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
