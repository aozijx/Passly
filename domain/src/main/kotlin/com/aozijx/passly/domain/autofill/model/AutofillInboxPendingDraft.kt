package com.aozijx.passly.domain.autofill.model

object AutofillInboxLimits {
    const val MAX_RECORD_BYTES = 64 * 1024
    const val MAX_PENDING_RECORDS = 8
    const val MAX_ACTIVE_BYTES = 256 * 1024
    const val MAX_QUARANTINED_RECORDS = 8
    const val MAX_QUARANTINE_BYTES = 256 * 1024
    const val MAX_ROOT_BYTES = 512 * 1024
    const val PENDING_RETENTION_MS = 24 * 60 * 60 * 1000L
    const val QUARANTINE_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L
}

@JvmInline
value class AutofillInboxDraftId(val value: String) {
    init {
        require(value.length == ID_HEX_LENGTH && value.all(::isLowerHex)) {
            "Inbox draft ID must be 16-byte lowercase hex"
        }
    }

    fun toByteArray(): ByteArray = ByteArray(ID_BYTES) { index ->
        val offset = index * 2
        ((value[offset].digitToInt(16) shl 4) or value[offset + 1].digitToInt(16)).toByte()
    }

    companion object {
        private const val ID_BYTES = 16
        private const val ID_HEX_LENGTH = ID_BYTES * 2

        fun fromBytes(bytes: ByteArray): AutofillInboxDraftId {
            require(bytes.size == ID_BYTES) { "Inbox draft ID must contain 16 bytes" }
            val value = buildString(ID_HEX_LENGTH) {
                bytes.forEach { byte ->
                    val value = byte.toInt() and 0xff
                    append(HEX[value ushr 4])
                    append(HEX[value and 0x0f])
                }
            }
            return AutofillInboxDraftId(value)
        }

        private fun isLowerHex(char: Char): Boolean = char in '0'..'9' || char in 'a'..'f'
        private const val HEX = "0123456789abcdef"
    }
}

enum class AutofillInboxSource(val wireKey: Int) {
    LEGACY_SERVICE(1),
    CREDENTIAL_MANAGER(2);

    companion object {
        fun fromWireKey(value: Int): AutofillInboxSource? =
            entries.firstOrNull { it.wireKey == value }
    }
}

data class AutofillInboxPendingDraft(
    val draftId: AutofillInboxDraftId,
    val source: AutofillInboxSource,
    val queuedAtMs: Long,
    val capturedAtMs: Long,
    val draft: AutofillSaveDraft,
) {
    init {
        require(queuedAtMs >= 0) { "Inbox queue time cannot be negative" }
        require(capturedAtMs >= 0) { "Inbox capture time cannot be negative" }
    }
}
