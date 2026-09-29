package com.aozijx.passly.presentation.feature.shell

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationRouteOwnershipBoundaryTest {

    @Test
    fun `navigation destination routes create their own view models`() {
        val routes = mapOf(
            "com/aozijx/passly/presentation/feature/vault/trash/TrashRoute.kt" to "TrashRoute",
            "com/aozijx/passly/presentation/feature/vault/detail/DetailRoute.kt" to "DetailRoute",
            "com/aozijx/passly/presentation/feature/vault/editor/password/AddPasswordEditorRoute.kt" to
                "AddPasswordEditorRoute",
            "com/aozijx/passly/presentation/feature/vault/editor/otp/AddOtpEditorRoute.kt" to
                "AddOtpEditorRoute",
            "com/aozijx/passly/presentation/feature/vault/editor/bankcard/AddBankCardEditorRoute.kt" to
                "AddBankCardEditorRoute",
            "com/aozijx/passly/presentation/feature/settings/main/navigation/core/RecoveryCodeRoute.kt" to
                "RecoveryCodeRoute",
        )

        routes.forEach { (path, functionName) ->
            val route = source(path)
            val signature = route.substringAfter("fun $functionName(").substringBefore(") {")

            assertFalse("$functionName exposes its ViewModel", signature.contains("viewModel:"))
            assertTrue("$functionName does not create its ViewModel", route.contains("hiltViewModel<"))
        }
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}
