package com.aozijx.passly.data.local.database.callback

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** SQLite is the final guard for the PENDING/COMMITTED ownership invariant. */
internal object AttachmentReferenceConstraintCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        createTriggers(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        createTriggers(db)
    }

    fun createTriggers(db: SupportSQLiteDatabase) {
        db.execSQL(INSERT_TRIGGER)
        db.execSQL(UPDATE_TRIGGER)
    }

    private const val VALID_STATE = """
        ((NEW.status = 'PENDING' AND NEW.entryId IS NULL AND NEW.stagingOwnerId IS NOT NULL)
        OR (NEW.status = 'COMMITTED' AND NEW.entryId IS NOT NULL AND NEW.stagingOwnerId IS NULL))
    """
    private const val ACTIVE_RESOURCE = """
        EXISTS (SELECT 1 FROM attachment_resources
                WHERE resourceId = NEW.resourceId AND lifecycleState = 'ACTIVE')
    """
    private const val INSERT_TRIGGER = """
        CREATE TRIGGER IF NOT EXISTS attachment_refs_validate_insert
        BEFORE INSERT ON attachment_refs
        WHEN NOT ($VALID_STATE AND $ACTIVE_RESOURCE)
        BEGIN
            SELECT RAISE(ABORT, 'invalid attachment ref state');
        END
    """
    private const val UPDATE_TRIGGER = """
        CREATE TRIGGER IF NOT EXISTS attachment_refs_validate_update
        BEFORE UPDATE ON attachment_refs
        WHEN NOT ($VALID_STATE AND $ACTIVE_RESOURCE)
        BEGIN
            SELECT RAISE(ABORT, 'invalid attachment ref state');
        END
    """
}
