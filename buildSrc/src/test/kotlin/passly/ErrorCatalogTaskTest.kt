package passly

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ErrorCatalogTaskTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `duplicate codes and unknown default reasons are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors = """
                    {"symbol":"One","code":"DUPLICATE","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"one"},
                    {"symbol":"Two","code":"DUPLICATE","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"two"}
                """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors = """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"MISSING","category":"APPLICATION","exposure":"INTERNAL","description":"one"}
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `message mapping exists only for user actionable errors`() {
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors = """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"KNOWN","category":"APPLICATION","exposure":"USER_ACTIONABLE","description":"one"}
                """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors = """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"one","message_key":"auth_error_failed"}
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `rendered kotlin and markdown are deterministic`() {
        val catalog = parse(
            errors = """
                {"symbol":"One","code":"ONE_ERROR","default_reason":"KNOWN","category":"SECURITY","exposure":"USER_ACTIONABLE","description":"Visible failure","message_key":"auth_error_failed"}
            """.trimIndent(),
        )

        val first = ErrorCatalogRenderer.render(catalog)
        val second = ErrorCatalogRenderer.render(catalog)

        assertEquals(first, second)
        assertTrue(first.coreKotlin.contains("val One = ErrorCode(\"ONE_ERROR\")"))
        assertTrue(first.appKotlin.contains("R.string.auth_error_failed"))
        assertTrue(first.appKotlin.contains("else -> null"))
        assertTrue(!first.appKotlin.contains("errorCode.value"))
        assertTrue(first.markdown.contains("| `ONE_ERROR` | `KNOWN` | SECURITY | USER_ACTIONABLE | Visible failure |"))
    }

    private fun parse(errors: String): ErrorCatalog = ErrorCatalogParser.parse(
        directory.resolve("catalog.json").apply {
            writeText(
                """
                    {
                      "schema_version": 1,
                      "reasons": [
                        {"symbol":"Known","code":"KNOWN","description":"Known reason"}
                      ],
                      "errors": [
                        $errors
                      ]
                    }
                """.trimIndent(),
            )
        },
    )
}
