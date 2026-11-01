package passly

internal object SourceBoundaryVerifier {
    private val qualifiedNamePattern = Regex(
        """(?:[A-Za-z_][A-Za-z0-9_]*|`[^`\r\n]+`)(?:\s*\.\s*(?:[A-Za-z_][A-Za-z0-9_]*|`[^`\r\n]+`))+""",
    )
    private val dotWhitespacePattern = Regex("""\s*\.\s*""")
    private val importAliasPattern = Regex("""\s+as\s+.*$""")
    private val escapedIdentifierPattern = Regex("""`([A-Za-z_][A-Za-z0-9_]*)`""")
    private val featurePathPattern = Regex("""/feature/([^/]+)/""")
    private val featureReferencePattern = Regex("""^com\.aozijx\.passly\.feature\.([^.]+)\.""")

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
                        rule.referenceViolation(source.path, normalizedPath, importedType, evidence)?.let(::add)
                    }
                    if (rule.forbiddenContentMarkers.any(codeEvidence::contains)
                    ) {
                        add(rule.violation(source.path, evidence))
                    }
                }
                val forbiddenReferences = qualifiedNamePattern.findAll(code)
                    .map { match ->
                        val lineIndex = code.substring(0, match.range.first).count { it == '\n' }
                        val reference = escapedIdentifierPattern.replace(match.value) { escaped ->
                            escaped.groupValues[1]
                        }.replace(dotWhitespacePattern, ".")
                        Triple(lineIndex, reference, match.value)
                    }
                    .filter { (lineIndex, reference) ->
                        val line = codeLines.getOrNull(lineIndex).orEmpty().trimStart()
                        !line.startsWith("import ") &&
                            rule.referenceViolation(source.path, normalizedPath, reference, "") != null
                    }
                    .distinctBy { (lineIndex) -> lineIndex }
                forbiddenReferences.forEach { (lineIndex, reference) ->
                    val codeLine = codeLines.getOrNull(lineIndex).orEmpty().trimStart()
                    if (!codeLine.startsWith("import ")) {
                        val evidence = rawLines.getOrNull(lineIndex).orEmpty().trim()
                        rule.referenceViolation(source.path, normalizedPath, reference, evidence)?.let(::add)
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
        val importedType = line.removePrefix("import ").replace(importAliasPattern, "").trim()
        return escapedIdentifierPattern.replace(importedType) { match ->
            match.groupValues[1]
        }.takeIf(String::isNotEmpty)
    }

    private fun SourceBoundaryRule.isForbiddenReference(reference: String): Boolean {
        val forbidden = forbiddenImports.any { reference == it || reference.startsWith("$it.") } ||
            forbiddenImportPrefixes.any(reference::startsWith) ||
            forbiddenImportPatterns.any { it.containsMatchIn(reference) }
        val allowed = allowedImports.any { reference == it || reference.startsWith("$it.") } ||
            allowedImportPrefixes.any(reference::startsWith)
        return forbidden && !allowed
    }

    private fun SourceBoundaryRule.referenceViolation(
        path: String,
        normalizedPath: String,
        reference: String,
        evidence: String,
    ): SourceBoundaryViolation? {
        if (id != "FEATURE_CROSS_FEATURE") {
            return if (isForbiddenReference(reference)) violation(path, evidence) else null
        }
        val sourceFeature = featurePathPattern.find(normalizedPath)?.groupValues?.get(1) ?: return null
        val targetFeature = featureReferencePattern.find(reference)?.groupValues?.get(1) ?: return null
        if (sourceFeature == targetFeature) return null
        val isReviewed = SourceBoundaryPolicy.featureIntegrationAllowances.any { allowance ->
            normalizedPath.contains(allowance.sourcePathContains, ignoreCase = true) &&
                allowance.targetFeature == targetFeature &&
                allowance.allowedImportPrefixes.any(reference::startsWith)
        }
        return if (isReviewed) null else SourceBoundaryViolation(
            ruleId = id,
            owner = "feature.$sourceFeature",
            path = path.replace('\\', '/'),
            evidence = evidence,
            message = "references feature.$targetFeature without an explicit integration owner",
        )
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
