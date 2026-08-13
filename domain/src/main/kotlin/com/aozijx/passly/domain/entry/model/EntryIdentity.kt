package com.aozijx.passly.domain.entry.model

@JvmInline
value class EntryId(val value: String) : Comparable<EntryId> {
    init {
        require(value.isNotBlank()) { "Entry ID cannot be blank" }
    }

    override fun compareTo(other: EntryId): Int = value.compareTo(other.value)
}

@JvmInline
value class EntryVersion(val value: Int) {
    init {
        require(value >= INITIAL_VALUE) { "Entry version must be positive" }
    }

    fun next(): EntryVersion = EntryVersion(value + 1)

    companion object {
        private const val INITIAL_VALUE = 1
        val INITIAL = EntryVersion(INITIAL_VALUE)
    }
}

data class EntryTimestamps(
    val createdAtMs: Long,
    val updatedAtMs: Long = createdAtMs,
    val deletedAtMs: Long? = null,
) {
    init {
        require(createdAtMs >= 0L) { "Creation time cannot be negative" }
        require(updatedAtMs >= createdAtMs) { "Update time cannot precede creation" }
        require(deletedAtMs == null || deletedAtMs >= createdAtMs) {
            "Deletion time cannot precede creation"
        }
    }

    val isDeleted: Boolean get() = deletedAtMs != null
}

data class EntryIdentity(
    val id: EntryId,
    val type: EntryType,
    val version: EntryVersion = EntryVersion.INITIAL,
    val timestamps: EntryTimestamps,
)
