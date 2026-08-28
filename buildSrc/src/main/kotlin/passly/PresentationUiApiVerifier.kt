package passly

internal object PresentationUiApiVerifier {
    private const val BUSINESS_PARAMETER_THRESHOLD = 8
    private val composableFunction = Regex(
        pattern = "@Composable(?:\\s*\\([^)]*\\))?\\s*" +
            "(?:(public|internal|private|protected)\\s+)?(?:inline\\s+)?fun\\s+" +
            "(?:<[^>]+>\\s*)?([A-Za-z_][A-Za-z0-9_]*)\\s*\\(",
    )

    fun inspect(sources: List<EditorSource>): List<String> = buildList {
        sources.asSequence()
            .map { source -> source.copy(path = source.path.replace('\\', '/')) }
            .filter { source -> "/presentation/ui/" in source.path }
            .forEach { source ->
                composableFunction.findAll(source.content).forEach { match ->
                    if (match.groupValues[1] == "private") return@forEach

                    val functionName = match.groupValues[2]
                    val openParenthesis = match.range.last
                    val closeParenthesis = matchingParenthesis(source.content, openParenthesis)
                    if (closeParenthesis < 0) return@forEach

                    val businessParameterCount = splitTopLevelParameters(
                        source.content.substring(openParenthesis + 1, closeParenthesis),
                    ).count(::isBusinessParameter)
                    if (businessParameterCount > BUSINESS_PARAMETER_THRESHOLD) {
                        add(
                            "${source.path}: $functionName has $businessParameterCount " +
                                "business parameters (threshold $BUSINESS_PARAMETER_THRESHOLD)",
                        )
                    }
                }
            }
    }.sorted()

    private fun isBusinessParameter(parameter: String): Boolean {
        val declaration = parameter.substringBefore('=').trim()
        val name = declaration.substringBefore(':').trim()
        val type = declaration.substringAfter(':', missingDelimiterValue = "").trim()
        if (name == "modifier" || type == "Modifier" || type.endsWith(".Modifier")) return false
        if ("@Composable" in type && "->" in type) return false
        return !type.substringBefore('<').substringAfterLast('.').endsWith("Scope")
    }

    private fun splitTopLevelParameters(parameters: String): List<String> {
        if (parameters.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        var nesting = 0
        parameters.forEachIndexed { index, character ->
            when (character) {
                '(', '<', '[', '{' -> nesting++
                ')', '>', ']', '}' -> nesting--
                ',' -> if (nesting == 0) {
                    parameters.substring(start, index).trim().takeIf(String::isNotEmpty)?.let(result::add)
                    start = index + 1
                }
            }
        }
        parameters.substring(start).trim().takeIf(String::isNotEmpty)?.let(result::add)
        return result
    }

    private fun matchingParenthesis(content: String, openParenthesis: Int): Int {
        var depth = 0
        for (index in openParenthesis until content.length) {
            when (content[index]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return index
            }
        }
        return -1
    }
}
