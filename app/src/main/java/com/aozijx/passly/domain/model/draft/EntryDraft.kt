package com.aozijx.passly.domain.model.draft

import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntrySummary
import com.aozijx.passly.domain.model.entry.secret.CustomField
import com.aozijx.passly.domain.model.otp.OtpConfig

data class EntryDraft(
    val draftId: String,
    val status: DraftStatus,
    val header: EntryHeader? = null,
    val summary: EntrySummary? = null,
    val password: String? = null,
    val otpConfig: OtpConfig? = null,
    val cardData: Map<String, String> = emptyMap(),
    val sshData: Map<String, String> = emptyMap(),
    val identityData: Map<String, String> = emptyMap(),
    val wifiData: Map<String, String> = emptyMap(),
    val passkeyData: Map<String, String> = emptyMap(),
    val customFields: List<CustomField> = emptyList(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
