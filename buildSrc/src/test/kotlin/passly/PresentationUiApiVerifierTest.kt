package passly

import kotlin.test.Test
import kotlin.test.assertEquals

class PresentationUiApiVerifierTest {
    @Test
    fun reportsLongComposablesAcrossPresentationUiAreas() {
        val settingsPath = "app/src/main/java/com/example/feature/settings/presentation/ui/SettingsContent.kt"
        val signals =
            PresentationUiApiVerifier.inspect(
                listOf(
                    longComposable(
                        path = "app/src/main/java/com/example/feature/recovery/presentation/ui/RecoveryScreen.kt",
                        functionName = "RecoveryScreen",
                    ),
                    longComposable(
                        path = "app/src/main/java/com/example/presentation/shared/component/SharedCard.kt",
                        functionName = "SharedCard",
                    ),
                    EditorSource(
                        path = settingsPath,
                        content =
                            """
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
                "app/src/main/java/com/example/feature/recovery/presentation/ui/RecoveryScreen.kt: " +
                    "RecoveryScreen has 9 business parameters (threshold 8)",
                "app/src/main/java/com/example/feature/settings/presentation/ui/SettingsContent.kt: " +
                    "SettingsContent has 9 business parameters (threshold 8)",
                "app/src/main/java/com/example/presentation/shared/component/SharedCard.kt: " +
                    "SharedCard has 9 business parameters (threshold 8)",
            ),
            signals,
        )
    }

    @Test
    fun ignoresPrivateComposablesAndExplicitComposePlumbing() {
        val signals =
            PresentationUiApiVerifier.inspect(
                listOf(
                    EditorSource(
                        path = "app/src/main/java/com/example/presentation/ui/vault/VaultContent.kt",
                        content =
                            """
                            @Composable
                            private fun PrivateContent(
                                one: String, two: String, three: String, four: String, five: String,
                                six: String, seven: String, eight: String, nine: String,
                            ) = Unit

                            @Composable
                            internal fun VaultContent(
                                one: Map<String, List<Int>>, two: String, three: String, four: String,
                                five: String, six: String, seven: String, eight: String,
                                rowScope: RowScope,
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
        val signals =
            PresentationUiApiVerifier.inspect(
                listOf(
                    EditorSource(
                        path = "app/src/main/java/com/example/feature/vault/presentation/VaultHost.kt",
                        content =
                            """
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

    private fun longComposable(
        path: String,
        functionName: String,
    ): EditorSource {
        val content =
            """
            @Composable
            internal fun $functionName(
                one: String, two: String, three: String, four: String, five: String,
                six: String, seven: String, eight: String, nine: String,
            ) = Unit
            """.trimIndent()
        return EditorSource(path = path, content = content)
    }
}
