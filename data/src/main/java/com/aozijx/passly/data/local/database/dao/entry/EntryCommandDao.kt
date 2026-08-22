package com.aozijx.passly.data.local.database.dao.entry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.EntryEntity

@Dao
interface EntryCommandDao {

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(entry: EntryEntity)

    // === Optimistic Lock Update ===

    @Query("""
        UPDATE entries SET
            title = :title,
            username = :username,
            primaryUrl = :primaryUrl,
            domains = :domains,
            applicationIds = :applicationIds,
            iconName = :iconName,
            iconCustomReference = :iconCustomReference,
            favorite = :favorite,
            tags = :tags,
            iconColor = :iconColor,
            expiresAt = :expiresAt,
            capabilityFlags = :capabilityFlags,
            otpType = :otpType,
            version = version + 1,
            updatedAt = :updatedAt
        WHERE entryId = :entryId AND version = :expectedVersion
    """)
    suspend fun optimisticUpdate(
        entryId: String,
        expectedVersion: Int,
        title: String,
        username: String,
        primaryUrl: String?,
        domains: Set<String>,
        applicationIds: Set<String>,
        iconName: String?,
        iconCustomReference: String?,
        favorite: Boolean,
        tags: Set<String>,
        iconColor: String?,
        expiresAt: Long?,
        capabilityFlags: Int,
        otpType: String?,
        updatedAt: Long
    ): Int

    @Query("UPDATE entries SET version = version + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND version = :expectedVersion")
    suspend fun bumpVersion(
        entryId: String,
        expectedVersion: Int,
        updatedAt: Long,
    ): Int

    // === Optimistic Lock Soft Delete ===

    @Query("UPDATE entries SET deletedAt = :deletedAt, version = version + 1, updatedAt = :updatedAt WHERE entryId = :entryId AND version = :expectedVersion")
    suspend fun optimisticSoftDelete(
        entryId: String,
        expectedVersion: Int,
        deletedAt: Long,
        updatedAt: Long
    ): Int

    // === Optimistic Lock Restore ===

    @Query("UPDATE entries SET deletedAt = NULL, version = version + 1, updatedAt = :now WHERE entryId = :entryId AND version = :expectedVersion AND deletedAt IS NOT NULL")
    suspend fun restoreOptimistic(entryId: String, expectedVersion: Int, now: Long): Int

    // === Hard Delete ===

    @Query("DELETE FROM entries WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String): Int

    @Query("DELETE FROM entries WHERE entryId = :entryId AND version = :expectedVersion AND deletedAt IS NOT NULL")
    suspend fun deleteDeletedOptimistic(entryId: String, expectedVersion: Int): Int

    @Query("DELETE FROM entries WHERE deletedAt IS NOT NULL")
    suspend fun deleteAllDeleted(): Int

    @Query("DELETE FROM entries WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long): Int

    @Query("UPDATE entries SET searchIndexVersion = :version WHERE entryId = :entryId")
    suspend fun updateSearchIndexVersion(entryId: String, version: Int): Int

    @Query("UPDATE entries SET searchIndexVersion = :version WHERE entryId IN (:entryIds)")
    suspend fun updateSearchIndexVersions(entryIds: List<String>, version: Int): Int

    @Query("UPDATE entries SET capabilityFlags = capabilityFlags | :capability WHERE entryId = :entryId")
    suspend fun addCapability(entryId: String, capability: Int): Int

    @Query("UPDATE entries SET capabilityFlags = capabilityFlags & :retainedMask WHERE entryId = :entryId")
    suspend fun retainCapabilities(entryId: String, retainedMask: Int): Int

}
