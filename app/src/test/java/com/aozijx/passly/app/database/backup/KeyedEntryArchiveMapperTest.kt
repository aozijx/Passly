package com.aozijx.passly.app.database.backup

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyedEntryArchiveMapperTest {
    @Test
    fun `wifi export keeps string fields and canonicalizes known security`() {
        val record = KeyedEntryArchiveMapper.toRecord(
            wifiEntry(WifiSecurityType.WpaPersonal),
            attachmentIds = emptyList(),
        )

        assertEquals("wifi", record.type)
        assertEquals("Office", record.text("wifi_ssid"))
        assertEquals("WPA", record.text("wifi_security"))
        assertEquals("password", record.text("password"))
    }

    @Test
    fun `legacy archive without security keeps wpa personal semantics`() {
        val restored = KeyedEntryArchiveMapper.toEntry(
            wifiRecord(security = null),
        )

        assertEquals(WifiSecurityType.WpaPersonal, restored.secret.wifi?.securityType)
    }

    @Test
    fun `unknown archive security normalizes before the next export`() {
        val restored = KeyedEntryArchiveMapper.toEntry(
            wifiRecord(security = "vendor-private"),
        )
        val exported = KeyedEntryArchiveMapper.toRecord(restored, attachmentIds = emptyList())

        assertEquals(
            WifiSecurityType.WpaPersonal,
            restored.secret.wifi?.securityType,
        )
        assertEquals("WPA", exported.text("wifi_security"))
    }

    private fun wifiEntry(securityType: WifiSecurityType) = Entry(
        identity = EntryIdentity(
            id = EntryId("wifi-1"),
            type = EntryType.WIFI,
            timestamps = EntryTimestamps(1L),
        ),
        profile = EntryProfile(title = "Office"),
        secret = EntrySecret(
            WifiCredential(
                ssid = "Office",
                password = "password",
                securityType = securityType,
            ),
        ),
    )

    private fun wifiRecord(security: String?): BackupEntryRecord = BackupEntryRecord(
        id = "wifi-legacy",
        type = "wifi",
        revision = 1,
        createdAt = 1L,
        updatedAt = 1L,
        fields = buildList {
            add(BackupFieldRecord("title", BackupFieldValue.text("Legacy")))
            add(BackupFieldRecord("wifi_ssid", BackupFieldValue.text("Legacy")))
            add(BackupFieldRecord("password", BackupFieldValue.text("password")))
            security?.let {
                add(BackupFieldRecord("wifi_security", BackupFieldValue.text(it)))
            }
        },
    )

    private fun BackupEntryRecord.text(key: String): String? =
        fields.singleOrNull { it.key == key }?.value?.text
}
