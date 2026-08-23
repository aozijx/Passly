package passly

internal data class EditorSource(
    val path: String,
    val content: String,
)

internal object EditorSourceBoundaryVerifier {
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
