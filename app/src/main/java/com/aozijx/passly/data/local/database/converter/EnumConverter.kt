package com.aozijx.passly.data.local.database.converter

import androidx.room.TypeConverter
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
import com.aozijx.passly.domain.entry.model.lookup.LookupField

object EntryTypeConverter {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.name

    @TypeConverter
    fun toEntryType(value: String): EntryType = EntryType.fromName(value)
}

object ActivityTypeConverter {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType =
        ActivityType.entries.find { it.name == value } ?: ActivityType.VIEW
}

object LookupFieldConverter {
    @TypeConverter
    fun fromLookupField(value: LookupField): String = value.name

    @TypeConverter
    fun toLookupField(value: String): LookupField =
        LookupField.entries.find { it.name == value } ?: LookupField.TITLE
}

object EntryRelationTypeConverter {
    @TypeConverter
    fun fromEntryRelationType(value: EntryRelationType): String = value.name

    @TypeConverter
    fun toEntryRelationType(value: String): EntryRelationType =
        EntryRelationType.valueOf(value)
}
