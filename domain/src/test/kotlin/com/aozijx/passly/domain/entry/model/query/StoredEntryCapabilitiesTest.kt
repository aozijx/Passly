package com.aozijx.passly.domain.entry.model.query

import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.credential.LoginCredential
import com.aozijx.passly.domain.entry.model.credential.SshCredential
import com.aozijx.passly.domain.entry.model.credential.CardCredential
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredEntryCapabilitiesTest {
    @Test
    fun passwordPresenceSurvivesRedactedBundleUsedForMetadataUpdate() {
        val bundle = EntrySecret(LoginCredential(password = null), notes = "Updated notes")
        val actual = EntryCapabilities.fromStoredSecret(bundle, setOf(SensitiveFieldKey.PASSWORD))
        assertTrue(EntryCapability.PASSWORD in actual)
    }

    @Test
    fun otpPresenceSurvivesRedactedBundleUsedForProfileUpdate() {
        val bundle = EntrySecret(OtpCredential(OtpConfig(secret = null)))
        val actual = EntryCapabilities.fromStoredSecret(bundle, setOf(SensitiveFieldKey.OTP_SECRET))
        assertTrue(EntryCapability.OTP in actual)
    }

    @Test
    fun clearedRowsRemoveCapabilitiesEvenIfAnOldPlaintextValueIsAvailable() {
        val oldPassword = EntrySecret(LoginCredential(password = "stale"))
        val oldOtp = EntrySecret(OtpCredential(OtpConfig(secret = "MY")))
        assertFalse(EntryCapability.PASSWORD in EntryCapabilities.fromStoredSecret(oldPassword, emptySet()))
        assertFalse(EntryCapability.OTP in EntryCapabilities.fromStoredSecret(oldOtp, emptySet()))
    }

    @Test
    fun sshPassphrasePresenceAndStructuralCapabilitiesAreIndependent() {
        val bundle = EntrySecret(SshCredential(privateKey = null, passphrase = null))
        val actual = EntryCapabilities.fromStoredSecret(bundle, setOf(SensitiveFieldKey.SSH_PASSPHRASE), hasAttachments = true)
        assertEquals(setOf(EntryCapability.PASSWORD, EntryCapability.SSH_KEY, EntryCapability.ATTACHMENTS), actual.values)
    }

    @Test
    fun unrelatedSensitiveRowsDoNotCreatePasswordOrOtpCapabilities() {
        val actual = EntryCapabilities.fromStoredSecret(EntrySecret(CardCredential()), setOf(SensitiveFieldKey.CARD_NUMBER))
        assertEquals(setOf(EntryCapability.CARD), actual.values)
    }

    @Test
    fun wifiPasswordInStoredBundleKeepsPasswordCapabilityWithoutSeparateRow() {
        val bundle = EntrySecret(WifiCredential(ssid = "Network", password = "network-password", securityType = WifiSecurityType.WpaPersonal))
        val actual = EntryCapabilities.fromStoredSecret(bundle, emptySet())
        assertEquals(setOf(EntryCapability.PASSWORD, EntryCapability.WIFI), actual.values)
    }

    @Test
    fun openWifiWithoutPasswordKeepsOnlyWifiCapability() {
        val bundle = EntrySecret(WifiCredential(ssid = "Open Network", securityType = WifiSecurityType.Open))
        val actual = EntryCapabilities.fromStoredSecret(bundle, emptySet())
        assertEquals(setOf(EntryCapability.WIFI), actual.values)
    }
}
