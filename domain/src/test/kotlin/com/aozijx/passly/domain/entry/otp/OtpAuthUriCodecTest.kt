package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpAuthUriCodecTest {
    @Test
    fun `unicode label and issuer use utf8 percent encoding`() {
        val config = parsed(
            "otpauth://totp/%E9%82%AE%E4%BB%B6%3A%E5%B0%8F%E6%98%8E?secret=$SECRET&issuer=%E9%82%AE%E4%BB%B6",
        )
        assertEquals("邮件", config.issuer)
        assertEquals("小明", config.accountName)
        assertEquals(config, parsed(formatted(config, "显示标题")))
    }

    @Test
    fun `unsupported encoding extensions cannot silently change the secret interpretation`() {
        listOf("encoder=steam", "encoding=BASE64").forEach { extension ->
            assertEquals(
                OtpUriParseResult.Unsupported,
                OtpAuthUriCodec.parse("otpauth://totp/alice?secret=$SECRET&$extension"),
            )
        }
        assertEquals("alice", parsed("otpauth://totp/alice?secret=$SECRET&image=icon.png").accountName)
    }

    @Test
    fun `labels are decoded once and literal plus signs survive`() {
        val config = parsed(
            "otpauth://totp/ACME%20Co%3Aalice+tag%2540example.com?secret=$SECRET&issuer=ACME%20Co",
        )

        assertEquals("ACME Co", config.issuer)
        assertEquals("alice+tag%40example.com", config.accountName)
    }

    @Test
    fun `format encodes each component once and prefers account metadata over display title`() {
        val config = OtpConfig(
            secret = SECRET, algorithm = OtpHashAlgorithm.SHA256, digits = 8, periodSeconds = 45,
            issuer = "ACME Co+100%", accountName = "alice+tag%40@example.com",
        )
        val uri = formatted(config, "Unrelated display title")

        assertEquals(
            "otpauth://totp/ACME%20Co%2B100%25%3Aalice%2Btag%2540%40example.com" +
                "?secret=$SECRET&algorithm=SHA256&digits=8&period=45&issuer=ACME%20Co%2B100%25",
            uri,
        )
        assertEquals(config, parsed(uri))
    }

    @Test
    fun `base64 export becomes base32 without changing the RFC totp code`() {
        val config = OtpConfig(
            secret = Base64.getEncoder().encodeToString("12345678901234567890".toByteArray()),
            encoding = OtpSecretEncoding.BASE64, digits = 8, issuer = "Example", accountName = "alice",
        )

        val restored = parsed(formatted(config, "Example"))

        assertEquals(OtpSecretEncoding.BASE32, restored.encoding)
        assertEquals(SECRET, restored.secret)
        assertEquals(OtpResult.Success("94287082"), OtpGenerator.generate(restored, timestamp = 59L))
    }

    @Test
    fun `hotp requires an explicit nonnegative counter and preserves long precision`() {
        val counter = 9_007_199_254_740_993L
        val config = parsed("otpauth://hotp/alice?secret=$SECRET&counter=$counter")
        assertEquals(OtpType.HOTP, config.type)
        assertEquals(counter, config.counter)
        assertEquals(null, config.periodSeconds)
        assertEquals(config, parsed(formatted(config, "alice")))

        listOf("", "&counter=", "&counter=-1", "&counter=invalid", "&counter=9223372036854775808")
            .forEach { suffix ->
                assertEquals(OtpUriParseResult.Malformed, OtpAuthUriCodec.parse("otpauth://hotp/alice?secret=$SECRET$suffix"))
            }
    }

    @Test
    fun `invalid supplied parameters never silently acquire defaults`() {
        listOf("digits=", "digits=no", "period=", "period=no", "period=0", "period=-1")
            .forEach { parameter ->
                assertEquals(
                    OtpUriParseResult.Malformed,
                    OtpAuthUriCodec.parse("otpauth://totp/alice?secret=$SECRET&$parameter"),
                )
            }
        listOf("algorithm=MD5", "algorithm=", "digits=5", "digits=9").forEach { parameter ->
            assertEquals(
                OtpUriParseResult.Unsupported,
                OtpAuthUriCodec.parse("otpauth://totp/alice?secret=$SECRET&$parameter"),
            )
        }
    }

    @Test
    fun `duplicate recognized parameters and inconsistent issuer are rejected`() {
        listOf(
            "secret=$SECRET&secret=$SECRET",
            "secret=$SECRET&%73ecret=$SECRET",
            "secret=$SECRET&issuer=Example&issuer=Example",
            "secret=$SECRET&algorithm=SHA1&algorithm=SHA256",
            "secret=$SECRET&digits=6&digits=8",
            "secret=$SECRET&period=30&period=60",
            "secret=$SECRET&counter=0&counter=1",
            "secret=$SECRET&issuer=Other",
        ).forEach { query ->
            assertEquals(OtpUriParseResult.Malformed, OtpAuthUriCodec.parse("otpauth://totp/Example:alice?$query"))
        }
    }

    @Test
    fun `invalid secrets do not enter a parsed config`() {
        listOf("", "A", "MZ", "MY=====", "ABC1", "====").forEach { secret ->
            assertEquals(OtpUriParseResult.Malformed, OtpAuthUriCodec.parse("otpauth://totp/alice?secret=$secret"))
        }
        assertEquals("MY", parsed("otpauth://totp/alice?secret=my%3D%3D%3D%3D%3D%3D").secret)
    }

    @Test
    fun `defaults apply only to absent values and seven digit otp remains supported`() {
        val config = parsed("OTPAUTH://TOTP/alice?secret=$SECRET")
        assertEquals(OtpHashAlgorithm.SHA1, config.algorithm)
        assertEquals(6, config.digits)
        assertEquals(30, config.periodSeconds)
        val sevenDigits = config.copy(digits = 7)
        assertEquals(sevenDigits, parsed(formatted(sevenDigits, "alice")))
    }

    @Test
    fun `steam marker round trips with explicit algorithm digits and period`() {
        val config = OtpConfig.steamDefaults(SECRET, "Steam").copy(accountName = "alice")
        assertEquals(config, parsed(formatted(config, "Display title")))
        assertEquals(OtpType.STEAM, parsed("otpauth://totp/Steam:alice?secret=$SECRET").type)
        listOf("algorithm=SHA256", "period=60", "digits=6").forEach { parameter ->
            assertEquals(
                OtpUriParseResult.Unsupported,
                OtpAuthUriCodec.parse("otpauth://totp/Steam:alice?secret=$SECRET&$parameter"),
            )
        }
    }

    @Test
    fun `invalid export configuration cannot emit a uri`() {
        listOf(
            OtpConfig(secret = null),
            OtpConfig(secret = "MZ"),
            OtpConfig(secret = "!", encoding = OtpSecretEncoding.BASE64),
            OtpConfig(secret = SECRET, issuer = "Example:Other"),
            OtpConfig(secret = SECRET, accountName = "alice:other"),
            OtpConfig(secret = SECRET, issuer = "Steam"),
            OtpConfig.steamDefaults(SECRET).copy(algorithm = OtpHashAlgorithm.SHA256),
        ).forEach { config ->
            assertEquals(OtpUriFormatResult.InvalidConfiguration, OtpAuthUriCodec.format(config, "alice"))
        }
        assertEquals(OtpUriFormatResult.InvalidConfiguration, OtpAuthUriCodec.format(OtpConfig(secret = SECRET), ""))
    }

    @Test
    fun `display title fallback removes the matching issuer prefix only once`() {
        val config = parsed(formatted(OtpConfig(secret = SECRET, issuer = "Example"), "Example: alice+tag"))
        assertEquals("Example", config.issuer)
        assertEquals("alice+tag", config.accountName)
    }

    @Test
    fun `non otp unsupported and malformed inputs have distinct results`() {
        assertEquals(OtpUriParseResult.NotOtp, OtpAuthUriCodec.parse("https://example.com"))
        assertEquals(OtpUriParseResult.Unsupported, OtpAuthUriCodec.parse("otpauth://unknown/alice?secret=$SECRET"))
        listOf(
            "otpauth://totp/%GG?secret=$SECRET",
            "otpauth://user@totp/alice?secret=$SECRET",
            "otpauth://totp:80/alice?secret=$SECRET",
            "otpauth://totp/alice?secret=$SECRET#fragment",
            "otpauth://totp/?secret=$SECRET",
            "otpauth://totp/alice",
            "otpauth:totp/alice?secret=$SECRET",
            "otpauth:///alice?secret=$SECRET",
            "otpauth://totp/%FF?secret=$SECRET",
            "otpauth://totp/%E9%82?secret=$SECRET",
        ).forEach { value -> assertEquals(OtpUriParseResult.Malformed, OtpAuthUriCodec.parse(value)) }
    }

    private fun parsed(value: String): OtpConfig {
        val result = OtpAuthUriCodec.parse(value)
        assertTrue(result is OtpUriParseResult.Parsed)
        return (result as OtpUriParseResult.Parsed).config
    }

    private fun formatted(config: OtpConfig, title: String): String {
        val result = OtpAuthUriCodec.format(config, title)
        assertTrue(result is OtpUriFormatResult.Formatted)
        return (result as OtpUriFormatResult.Formatted).content
    }

    companion object {
        private const val SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    }
}
