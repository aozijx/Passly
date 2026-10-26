package passly

internal object SourceBoundaryVerifier {
    fun verify(
        sources: List<EditorSource>,
        rules: List<SourceBoundaryRule>,
    ): List<SourceBoundaryViolation> = buildList {
        sources.forEach { source ->
            val normalizedPath = "/${source.path.replace('\\', '/').trimStart('/')}"
            rules.filter { it.appliesTo(normalizedPath) }.forEach { rule ->
                source.content.lineSequence().forEach { rawLine ->
                    val evidence = rawLine.trim()
                    parseImport(evidence)?.let { importedType ->
                        if ((importedType in rule.forbiddenImports ||
                            rule.forbiddenImportPrefixes.any(importedType::startsWith) ||
                            rule.forbiddenImportPatterns.any { it.containsMatchIn(importedType) }) &&
                            rule.allowedImportPrefixes.none(importedType::startsWith)
                        ) {
                            add(rule.violation(source.path, evidence))
                        }
                    }
                    if (!evidence.startsWith("//") &&
                        rule.forbiddenContentMarkers.any(evidence::contains)
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

    private fun SourceBoundaryRule.violation(path: String, evidence: String) =
        SourceBoundaryViolation(id, owner, path.replace('\\', '/'), evidence, message)
}
