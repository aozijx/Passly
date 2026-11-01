package passly

internal data class FeatureIntegrationAllowance(
    val sourcePathContains: String,
    val targetFeature: String,
    val allowedImportPrefixes: Set<String>,
    val owner: String,
    val rationale: String,
)
