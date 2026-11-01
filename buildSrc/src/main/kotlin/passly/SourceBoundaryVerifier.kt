package passly

internal object SourceBoundaryVerifier {
    private val qualifiedNamePattern = Regex("""[A-Za-z_][A-Za-z0-9_]*(?:\s*\.\s*[A-Za-z_][A-Za-z0-9_]*)+""")
    private val dotWhitespacePattern = Regex("""\s*\.\s*""")

    fun verify(
        sources: List<EditorSource>,
        rules: List<SourceBoundaryRule>,
    ): List<SourceBoundaryViolation> = buildList {
        sources.forEach { source ->
            val normalizedPath = "/${source.path.replace('\\', '/').trimStart('/')}"
            val code = maskCommentsAndStrings(source.content)
            val rawLines = source.content.lineSequence().toList()
            val codeLines = code.lineSequence().toList()
            rules.filter { it.appliesTo(normalizedPath) }.forEach { rule ->
                rawLines.zip(codeLines).forEach { (rawLine, codeLine) ->
                    val evidence = rawLine.trim()
                    val codeEvidence = codeLine.trim()
                    parseImport(codeEvidence)?.let { importedType ->
                        if (rule.isForbiddenReference(importedType)) {
                            add(rule.violation(source.path, evidence))
                        }
                    }
                    if (rule.forbiddenContentMarkers.any(codeEvidence::contains)
                    ) {
                        add(rule.violation(source.path, evidence))
                    }
                }
                val forbiddenReferenceLines = qualifiedNamePattern.findAll(code)
                    .filter { match ->
                        val reference = match.value.replace(dotWhitespacePattern, ".")
                        rule.isForbiddenReference(reference)
                    }
                    .map { match -> code.substring(0, match.range.first).count { it == '\n' } }
                    .distinct()
                forbiddenReferenceLines.forEach { lineIndex ->
                    val codeLine = codeLines.getOrNull(lineIndex).orEmpty().trimStart()
                    if (!codeLine.startsWith("import ")) {
                        add(rule.violation(source.path, rawLines.getOrNull(lineIndex).orEmpty().trim()))
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

    private fun maskCommentsAndStrings(source: String): String = KotlinCodeMasker(source).mask()

    private fun SourceBoundaryRule.violation(path: String, evidence: String) =
        SourceBoundaryViolation(id, owner, path.replace('\\', '/'), evidence, message)
}

private class KotlinCodeMasker(private val source: String) {
    private val output = CharArray(source.length) { index ->
        source[index].takeIf { it == '\n' || it == '\r' } ?: ' '
    }

    fun mask(): String {
        scanCode(0, stopAtTemplateBrace = false)
        return output.concatToString()
    }

    private fun scanCode(start: Int, stopAtTemplateBrace: Boolean): Int {
        var index = start
        var braceDepth = 0
        while (index < source.length) {
            val current = source[index]
            val next = source.getOrNull(index + 1)
            when {
                current == '/' && next == '/' -> index = skipLineComment(index)
                current == '/' && next == '*' -> index = skipBlockComment(index)
                source.startsWith("\"\"\"", index) -> index = scanString(index, raw = true)
                current == '"' -> index = scanString(index, raw = false)
                current == '\'' -> index = skipCharacterLiteral(index)
                stopAtTemplateBrace && current == '}' && braceDepth == 0 -> return index + 1
                current == '{' -> {
                    preserve(index)
                    braceDepth++
                    index++
                }
                current == '}' && braceDepth > 0 -> {
                    preserve(index)
                    braceDepth--
                    index++
                }
                else -> {
                    preserve(index)
                    index++
                }
            }
        }
        return index
    }

    private fun scanString(start: Int, raw: Boolean): Int {
        val delimiterLength = if (raw) 3 else 1
        var index = start + delimiterLength
        while (index < source.length) {
            if (raw && source.startsWith("\"\"\"", index)) return index + 3
            val current = source[index]
            if (!raw && current == '"') return index + 1
            if (!raw && current == '\\') {
                index = (index + 2).coerceAtMost(source.length)
                continue
            }
            if (current == '$') {
                when (source.getOrNull(index + 1)) {
                    '{' -> index = scanCode(index + 2, stopAtTemplateBrace = true)
                    else -> index = skipSimpleTemplate(index)
                }
                continue
            }
            index++
        }
        return index
    }

    private fun skipSimpleTemplate(start: Int): Int {
        var index = start + 1
        if (!source.getOrNull(index).isIdentifierStart()) return index
        index++
        while (source.getOrNull(index).isIdentifierPart()) index++
        return index
    }

    private fun skipLineComment(start: Int): Int {
        val lineEnd = source.indexOf('\n', start + 2)
        return if (lineEnd < 0) source.length else lineEnd
    }

    private fun skipBlockComment(start: Int): Int {
        var index = start + 2
        var depth = 1
        while (index < source.length && depth > 0) {
            when {
                source.startsWith("/*", index) -> { depth++; index += 2 }
                source.startsWith("*/", index) -> { depth--; index += 2 }
                else -> index++
            }
        }
        return index
    }

    private fun skipCharacterLiteral(start: Int): Int {
        var index = start + 1
        while (index < source.length) {
            when {
                source[index] == '\\' -> index = (index + 2).coerceAtMost(source.length)
                source[index] == '\'' -> return index + 1
                else -> index++
            }
        }
        return index
    }

    private fun preserve(index: Int) {
        output[index] = source[index]
    }

    private fun Char?.isIdentifierStart(): Boolean = this == '_' || this?.isLetter() == true

    private fun Char?.isIdentifierPart(): Boolean =
        this == '_' || this?.isLetterOrDigit() == true
}
