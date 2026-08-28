package passly

import kotlin.test.Test
import kotlin.test.assertEquals

class PresentationUiApiVerifierTest {
    @Test
    fun reportsNamedPresentationUiComposableWhenBusinessParametersExceedThreshold() {
        val signals = PresentationUiApiVerifier.inspect(
            listOf(
                EditorSource(
                    path = "app/src/main/java/com/example/presentation/ui/settings/SettingsContent.kt",
                    content = """
                        @Composable
                        internal fun SettingsContent(
                            title: String,
                            subtitle: String,
                            enabled: Boolean,
                            selected: Boolean,
                            loading: Boolean,
                            error: String?,
                            count: Int,
                            mode: String,
                            onRetry: () -> Unit,
                            modifier: Modifier = Modifier,
                            content: @Composable () -> Unit = {},
                        ) = Unit
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(
            listOf(
                "app/src/main/java/com/example/presentation/ui/settings/SettingsContent.kt: " +
                    "SettingsContent has 9 business parameters (threshold 8)",
            ),
            signals,
        )
    }

    @Test
    fun ignoresPrivateComposablesAndExplicitComposePlumbing() {
        val signals = PresentationUiApiVerifier.inspect(
            listOf(
                EditorSource(
                    path = "app/src/main/java/com/example/presentation/ui/vault/VaultContent.kt",
                    content = """
                        @Composable
                        private fun PrivateContent(
                            one: String, two: String, three: String, four: String, five: String,
                            six: String, seven: String, eight: String, nine: String,
                        ) = Unit

                        @Composable
                        internal fun VaultContent(
                            one: String, two: String, three: String, four: String,
                            five: String, six: String, seven: String, eight: String,
                            modifier: Modifier = Modifier,
                            content: @Composable () -> Unit,
                        ) = Unit
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(emptyList(), signals)
    }

    @Test
    fun ignoresSourcesOutsidePresentationUi() {
        val signals = PresentationUiApiVerifier.inspect(
            listOf(
                EditorSource(
                    path = "app/src/main/java/com/example/presentation/feature/vault/VaultHost.kt",
                    content = """
                        @Composable
                        internal fun VaultHost(
                            one: String, two: String, three: String, four: String, five: String,
                            six: String, seven: String, eight: String, nine: String,
                        ) = Unit
                    """.trimIndent(),
                ),
            ),
        )

        assertEquals(emptyList(), signals)
    }
}
