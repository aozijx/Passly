package passly

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchitectureVerificationMatrixTest {
    @Test
    fun everyGradleModuleRequiresAnExplicitBoundaryPolicy() {
        val actual = setOf(":app", ":core", ":future")
        val policies = setOf(":app", ":core")

        assertEquals(setOf(":future"), missingPolicyModules(actual, policies))
        assertEquals(emptySet(), missingPolicyModules(actual, policies + ":future"))
    }

    @Test
    fun staleBoundaryPoliciesAreRejected() {
        val actual = setOf(":app", ":core")
        val policies = setOf(":app", ":core", ":removed")

        assertEquals(setOf(":removed"), stalePolicyModules(actual, policies))
    }

    @Test
    fun everyModuleContributesItsKotlinSourceRootsToVerification() {
        assertEquals(
            setOf(
                "app/src/**/*.kt",
                "core/src/**/*.kt",
                "core/common/src/**/*.kt",
                "data/src/**/*.kt",
                "domain/src/**/*.kt",
                "runtime/session/src/**/*.kt",
            ),
            architectureSourcePatterns(
                setOf(":app", ":core", ":core:common", ":data", ":domain", ":runtime:session"),
            ),
        )
    }
}
