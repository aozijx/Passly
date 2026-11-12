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
                errors =
                    """
                    {"symbol":"One","code":"DUPLICATE","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"one","group_path":["General"]},
                    {"symbol":"Two","code":"DUPLICATE","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"two","group_path":["General"]}
                    """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors =
                    """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"MISSING","category":"APPLICATION","exposure":"INTERNAL","description":"one","group_path":["General"]}
                    """.trimIndent(),
            )
        }
    }

    @Test
    fun `hierarchy leaf cannot also be a parent object`() {
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors =
                    """
                    {"symbol":"Child","code":"ONE_ERROR","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"parent leaf","group_path":["Application"]},
                    {"symbol":"Leaf","code":"TWO_ERROR","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"nested leaf","group_path":["Application","Child"]}
                    """.trimIndent(),
            )
        }
    }

    @Test
    fun `message mapping exists only for user actionable errors`() {
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors =
                    """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"KNOWN","category":"APPLICATION","exposure":"USER_ACTIONABLE","description":"one","group_path":["General"]}
                    """.trimIndent(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            parse(
                errors =
                    """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"KNOWN","category":"APPLICATION","exposure":"INTERNAL","description":"one","message_key":"auth_error_failed","group_path":["General"]}
                    """.trimIndent(),
            )
        }
    }

    @Test
    fun `rendered kotlin and markdown are deterministic`() {
        val catalog =
            parse(
                errors =
                    """
                    {"symbol":"One","code":"ONE_ERROR","default_reason":"KNOWN","category":"SECURITY","exposure":"USER_ACTIONABLE","description":"Visible failure","message_key":"auth_error_failed","group_path":["Authentication"]}
                    """.trimIndent(),
            )

        val first = ErrorCatalogRenderer.render(catalog)
        val second = ErrorCatalogRenderer.render(catalog)

        assertEquals(first, second)
        assertTrue(first.coreKotlin.contains("object Security"))
        assertTrue(first.coreKotlin.contains("val One = ErrorCode(\"ONE_ERROR\")"))
        assertTrue(first.appKotlin.contains("ErrorCodes.Security.Authentication.One -> R.string.auth_error_failed"))
        assertTrue(first.appKotlin.contains("else -> null"))
        assertTrue(!first.appKotlin.contains("errorCode.value"))
        assertTrue(first.markdown.contains("### Security"))
        assertTrue(first.markdown.contains("| `Security.Authentication.One` (`ONE_ERROR`) | `Application.Fallback.Known` (`KNOWN`) | USER_ACTIONABLE | Visible failure |"))
    }

    private fun parse(errors: String): ErrorCatalog =
        ErrorCatalogParser.parse(
            directory.resolve("catalog.json").apply {
                writeText(
                    """
                    {
                      "schema_version": 1,
                      "reasons": [
                        {"symbol":"Known","code":"KNOWN","description":"Known reason","group_path":["Application","Fallback"]}
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
