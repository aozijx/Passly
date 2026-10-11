package com.aozijx.passly.data.local.database.converter

import androidx.room.TypeConverter
import com.aozijx.passly.data.codec.json.AppJson
import com.aozijx.passly.data.codec.activityTypeFromStorageKey
import com.aozijx.passly.data.codec.entryRelationTypeFromStorageKey
import com.aozijx.passly.data.codec.entryTypeFromStorageKey
import com.aozijx.passly.data.codec.otpTypeFromStorageKey
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType

object EntryTypeConverter {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.toStorageKey()

    @TypeConverter
    fun toEntryType(value: String): EntryType = entryTypeFromStorageKey(value)
}

object ActivityTypeConverter {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.toStorageKey()

    @TypeConverter
    fun toActivityType(value: String): ActivityType = activityTypeFromStorageKey(value)
}

object OtpTypeConverter {
    @TypeConverter
    fun fromOtpType(value: OtpType): String = value.toStorageKey()

    @TypeConverter
    fun toOtpType(value: String): OtpType = otpTypeFromStorageKey(value)
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
    fun fromEntryRelationType(value: EntryRelationType): String = value.toStorageKey()

    @TypeConverter
    fun toEntryRelationType(value: String): EntryRelationType =
        entryRelationTypeFromStorageKey(value)
}
