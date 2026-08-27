package passly

internal data class SourceBoundaryRule(
    val id: String,
    val sourcePathContains: String,
    val forbiddenImportPrefixes: Set<String> = emptySet(),
    val allowedImportPrefixes: Set<String> = emptySet(),
    val forbiddenContentMarkers: Set<String> = emptySet(),
    val allowedSourcePathContains: Set<String> = emptySet(),
    val message: String,
)

internal data class SourceBoundaryViolation(
    val ruleId: String,
    val path: String,
    val evidence: String,
    val message: String,
) {
    fun format(): String = "[$ruleId] $path: $message | $evidence"
}
