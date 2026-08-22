package com.aozijx.passly.data.local.database.converter

import androidx.room.TypeConverter
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.domain.entry.model.query.LookupField

object EntryTypeConverter {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.name

    @TypeConverter
    fun toEntryType(value: String): EntryType = EntryType.valueOf(value)
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

object StringSetConverter {
    @TypeConverter
    fun fromStringSet(value: Set<String>): String = AppJson.encodeToString(value.sorted())

    @TypeConverter
    fun toStringSet(value: String): Set<String> =
        AppJson.decodeFromString<List<String>>(value).toSet()
}

object EntryRelationTypeConverter {
    @TypeConverter
    fun fromEntryRelationType(value: EntryRelationType): String = value.name

    @TypeConverter
    fun toEntryRelationType(value: String): EntryRelationType =
        EntryRelationType.valueOf(value)
}
