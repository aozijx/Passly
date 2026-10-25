package com.aozijx.passly.feature.backup.internal.archive.format.bitwarden

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BitwardenOtpImportTest {
    private val adapter = BitwardenJsonImportAdapter()

    @Test
    fun `URI label keeps literal plus and decodes escaped content once`() {
        val fields = importOtp("otpauth://totp/Bank+Lab:alice+tag%2540example?secret=$SECRET&issuer=Bank+Lab")
            .fields.associate { it.key to it.value }

        assertEquals("Bank+Lab", fields.getValue("otp_issuer").text)
        assertEquals("alice+tag%40example", fields.getValue("otp_account_name").text)
    }

    @Test
    fun `unprefixed label is account name rather than issuer`() {
        val fields = importOtp("otpauth://totp/alice%40example?secret=$SECRET")
            .fields.associate { it.key to it.value }

        assertEquals("alice@example", fields.getValue("otp_account_name").text)
        assertEquals(null, fields["otp_issuer"])
    }

    @Test
    fun `HOTP preserves explicit long counter without adding a period`() {
        val fields = importOtp("otpauth://hotp/Bank:alice?secret=$SECRET&counter=${Long.MAX_VALUE}&digits=8&algorithm=SHA512")
            .fields.associate { it.key to it.value }

        assertEquals("hotp", fields.getValue("otp_type").text)
        assertEquals(Long.MAX_VALUE, fields.getValue("otp_counter").longValue)
        assertEquals(8, fields.getValue("otp_digits").integerValue)
        assertEquals("sha512", fields.getValue("otp_algorithm").text)
        assertEquals(null, fields["otp_period"])
    }

    @Test
    fun `Steam issuer URI uses Steam code parameters`() {
        val fields = importOtp("otpauth://totp/Steam:alice?secret=$SECRET&issuer=Steam&digits=5")
            .fields.associate { it.key to it.value }

        assertEquals("steam", fields.getValue("otp_type").text)
        assertEquals(5, fields.getValue("otp_digits").integerValue)
        assertEquals(30, fields.getValue("otp_period").integerValue)
    }

    @Test
    fun `bare secret and Steam prefix retain their source format defaults`() {
        val bare = importOtp(SECRET).fields.associate { it.key to it.value }
        val steam = importOtp("steam://$SECRET").fields.associate { it.key to it.value }

        assertEquals(SECRET, bare.getValue("otp_secret").text)
        assertEquals("totp", bare.getValue("otp_type").text)
        assertEquals(6, bare.getValue("otp_digits").integerValue)
        assertEquals(SECRET, steam.getValue("otp_secret").text)
        assertEquals("steam", steam.getValue("otp_type").text)
        assertEquals(5, steam.getValue("otp_digits").integerValue)
    }

    @Test
    fun `bare and case insensitive Steam secrets are validated and canonicalized before import`() {
        val bare = importOtp(" m y = = = = = = \n").fields.associate { it.key to it.value }
        val steam = importOtp("StEaM:// ${SECRET.lowercase()} ").fields.associate { it.key to it.value }

        assertEquals("MY", bare.getValue("otp_secret").text)
        assertEquals(SECRET, steam.getValue("otp_secret").text)
        assertEquals("steam", steam.getValue("otp_type").text)
        assertEquals("sha1", steam.getValue("otp_algorithm").text)
        assertEquals(5, steam.getValue("otp_digits").integerValue)
        assertEquals(30, steam.getValue("otp_period").integerValue)
    }

    @Test
    fun `invalid bare and Steam secrets cannot become unusable imported OTP entries`() {
        for (strategy in BackupImportStrategy.entries) {
            for (secret in listOf("INVALID!", "MZ======", "A", "steam://", "steam://INVALID!", "StEaM://INVALID!")) {
                assertThrows(BackupFailed::class.java) { importOtp(secret, strategy) }
            }
        }
    }

    @Test
    fun `malformed ambiguous and unsupported OTP URIs fail instead of importing guessed configuration`() {
        val invalid = listOf(
            "otpauth://hotp/alice?secret=$SECRET",
            "otpauth://hotp/alice?secret=$SECRET&counter=broken",
            "otpauth://totp/alice?secret=$SECRET&period=broken",
            "otpauth://totp/alice?secret=$SECRET&digits=broken",
            "otpauth://totp/alice?secret=$SECRET&secret=MY",
            "otpauth://totp/Bank:alice?secret=$SECRET&issuer=Other",
            "otpauth://totp/alice?secret=$SECRET&encoder=unknown",
            "otpauth://totp/alice?secret=INVALID!",
            "otpauth://totp/%C3%28?secret=$SECRET",
            "otpauth:totp/alice?secret=$SECRET",
        )
        for (strategy in BackupImportStrategy.entries) {
            for (uri in invalid) {
                assertThrows(BackupFailed::class.java) { importOtp(uri, strategy) }
            }
        }
    }

    private fun importOtp(
        value: String,
        strategy: BackupImportStrategy = BackupImportStrategy.COMPATIBLE,
    ): BackupEntryRecord {
        val payload = """{"items":[{"id":"login-1","type":1,"name":"Bank","login":{"username":"alice","password":"fake-password","totp":${JsonPrimitive(value)}}}]}"""
            .toByteArray(Charsets.UTF_8)
        return adapter.decode(payload, password = null, strategy = strategy)
            .document.entries.single { it.type == "otp" }
    }

    private companion object {
        const val SECRET = "JBSWY3DPEHPK3PXP"
    }
}
