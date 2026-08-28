package passly

internal data class SourceBoundaryRule(
    val id: String,
    val owner: String = id,
    val sourcePathContains: String,
    val forbiddenImportPrefixes: Set<String> = emptySet(),
    val allowedImportPrefixes: Set<String> = emptySet(),
    val forbiddenContentMarkers: Set<String> = emptySet(),
    val allowedSourcePathContains: Set<String> = emptySet(),
    val message: String,
)

internal data class SourceBoundaryViolation(
    val ruleId: String,
    val owner: String,
    val path: String,
    val evidence: String,
    val message: String,
) {
    fun format(): String = "[$ruleId] $path: owner=$owner; $message | $evidence"
}
