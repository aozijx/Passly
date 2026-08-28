package passly

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchitectureMetricsTaskTest {
    @Test
    fun reportIsStableSortedAndSeparatesInvariantsFromReviewSignals() {
        val largeSource = (1..301).joinToString("\n") { "line$it" }
        val sources = listOf(
            source(
                "domain/src/main/kotlin/com/example/DomainThing.kt",
                "import android.content.Context",
            ),
            source(
                "app/src/main/java/com/example/presentation/feature/vault/VaultViewModel.kt",
                """
                    import com.example.data.SecretStore
                    import com.example.presentation.navigation.data.SettingsRoute
                    class VaultViewModel
                """.trimIndent(),
            ),
            source(
                "app/src/test/java/com/example/presentation/feature/vault/VaultViewModelTest.kt",
                "class VaultViewModelTest",
            ),
            source(
                "app/src/main/java/com/example/presentation/feature/settings/SettingsReducer.kt",
                "object SettingsReducer { fun reduce(value: Int) = helper(value) }",
            ),
            source(
                "app/src/main/java/com/aozijx/passly/core/Legacy.kt",
                "class Legacy",
            ),
            source(
                "app/src/main/java/com/example/presentation/feature/vault/VaultScreen.kt",
                largeSource,
            ),
        )

        val report = ArchitectureMetrics.analyze(sources).render()

        assertEquals(
            """
            Kotlin files by module/source-set:
              :app main=4 test=1
              :domain main=1 test=0
            Major App ownership areas:
              app-local-core=1
              presentation.feature=3
            Direct import invariants:
              data->app/feature/presentation=0
              domain->android=1
              feature->presentation=0
              presentation->data=1
            Review signals:
              app/src/main/java/com/example/presentation/feature/settings/SettingsReducer.kt: trivial reducer
              app/src/main/java/com/example/presentation/feature/vault/VaultScreen.kt: 301 lines
              app/src/main/java/com/example/presentation/feature/vault/VaultScreen.kt: passive UI below presentation.feature
            """.trimIndent(),
            report,
        )
    }

    @Test
    fun viewModelWithoutMatchingFocusedTestIsReported() {
        val report = ArchitectureMetrics.analyze(
            listOf(
                source(
                    "app/src/main/java/com/example/presentation/feature/detail/DetailViewModel.kt",
                    "class DetailViewModel",
                ),
            ),
        )

        assertEquals(
            listOf(
                "app/src/main/java/com/example/presentation/feature/detail/DetailViewModel.kt: ViewModel without focused test",
            ),
            report.reviewSignals,
        )
    }

    private fun source(path: String, content: String) = EditorSource(path, content)
}
