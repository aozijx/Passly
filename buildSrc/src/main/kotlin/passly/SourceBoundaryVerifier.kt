package passly

internal object SourceBoundaryVerifier {
    private val qualifiedNamePattern = Regex("""[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+""")

    fun verify(
        sources: List<EditorSource>,
        rules: List<SourceBoundaryRule>,
    ): List<SourceBoundaryViolation> = buildList {
        sources.forEach { source ->
            val normalizedPath = "/${source.path.replace('\\', '/').trimStart('/')}"
            val code = maskCommentsAndStrings(source.content)
            rules.filter { it.appliesTo(normalizedPath) }.forEach { rule ->
                source.content.lineSequence().zip(code.lineSequence()).forEach { (rawLine, codeLine) ->
                    val evidence = rawLine.trim()
                    val codeEvidence = codeLine.trim()
                    parseImport(codeEvidence)?.let { importedType ->
                        if (rule.isForbiddenReference(importedType)) {
                            add(rule.violation(source.path, evidence))
                        }
                    }
                    if (!codeEvidence.startsWith("import ") &&
                        qualifiedNamePattern.findAll(codeLine).any { match ->
                            rule.isForbiddenReference(match.value)
                        }
                    ) {
                        add(rule.violation(source.path, evidence))
                    }
                    if (rule.forbiddenContentMarkers.any(codeEvidence::contains)
                    ) {
                        add(rule.violation(source.path, evidence))
                    }
                }
            }
        }
    }

    private fun SourceBoundaryRule.appliesTo(path: String): Boolean =
        path.contains(sourcePathContains, ignoreCase = true) &&
            (sourcePathPatterns.isEmpty() || sourcePathPatterns.any { it.containsMatchIn(path) }) &&
            allowedSourcePathContains.none { path.contains(it, ignoreCase = true) } &&
            allowedSourcePathPatterns.none { it.containsMatchIn(path) }

    private fun parseImport(line: String): String? {
        if (!line.startsWith("import ")) return null
        return line.removePrefix("import ").substringBefore(" as ").trim().takeIf(String::isNotEmpty)
    }

    private fun SourceBoundaryRule.isForbiddenReference(reference: String): Boolean {
        val forbidden = forbiddenImports.any { reference == it || reference.startsWith("$it.") } ||
            forbiddenImportPrefixes.any(reference::startsWith) ||
            forbiddenImportPatterns.any { it.containsMatchIn(reference) }
        val allowed = allowedImports.any { reference == it || reference.startsWith("$it.") } ||
            allowedImportPrefixes.any(reference::startsWith)
        return forbidden && !allowed
    }

    private fun maskCommentsAndStrings(source: String): String {
        val masked = StringBuilder(source.length)
        var index = 0
        var blockDepth = 0
        var lineComment = false
        var quote: Char? = null
        var rawString = false

        fun mask(character: Char) = masked.append(
            if (character == '\n' || character == '\r') character else ' ',
        )

        while (index < source.length) {
            val current = source[index]
            val next = source.getOrNull(index + 1)
            val startsRawString = source.startsWith("\"\"\"", index)

            when {
                lineComment -> {
                    mask(current)
                    index++
                    if (current == '\n') lineComment = false
                }
                blockDepth > 0 && current == '/' && next == '*' -> {
                    mask(current); mask(next!!); index += 2; blockDepth++
                }
                blockDepth > 0 && current == '*' && next == '/' -> {
                    mask(current); mask(next!!); index += 2; blockDepth--
                }
                blockDepth > 0 -> { mask(current); index++ }
                rawString && startsRawString -> {
                    repeat(3) { mask(source[index + it]) }
                    index += 3
                    rawString = false
                }
                rawString -> { mask(current); index++ }
                quote != null && current == '\\' && next != null -> {
                    mask(current); mask(next); index += 2
                }
                quote != null -> {
                    mask(current)
                    index++
                    if (current == quote) quote = null
                }
                current == '/' && next == '/' -> {
                    mask(current); mask(next!!); index += 2; lineComment = true
                }
                current == '/' && next == '*' -> {
                    mask(current); mask(next!!); index += 2; blockDepth = 1
                }
                startsRawString -> {
                    repeat(3) { mask(source[index + it]) }
                    index += 3
                    rawString = true
                }
                current == '"' || current == '\'' -> { mask(current); index++; quote = current }
                else -> { masked.append(current); index++ }
            }
        }

        return masked.toString()
    }

    private fun SourceBoundaryRule.violation(path: String, evidence: String) =
        SourceBoundaryViolation(id, owner, path.replace('\\', '/'), evidence, message)
}
