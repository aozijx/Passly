package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryListItemMapperTest {
    @Test
    fun `typed otp metadata reaches list item without string parsing`() {
        val entity = EntryEntity(
            entryId = "entry-1",
            entryType = EntryType.OTP,
            otpType = OtpType.STEAM,
            title = "Steam",
        )

        val item = EntryListItemMapper.assemble(
            entity = entity,
            summary = EntryProfile(title = "Steam"),
        )

        assertEquals(OtpType.STEAM, item.otpType)
    }
}
