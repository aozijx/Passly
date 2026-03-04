# Room 基础规则
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class * extends androidx.room.RoomDatabase

# SQLCipher / SQLite 相关规则
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class androidx.sqlite.db.SupportSQLite* { *; }

# 核心数据模型防止被混淆导致 Room 无法映射
-keepclassmembers class com.example.poop.data.VaultEntry { *; }
-keep class com.example.poop.data.VaultEntry { *; }
-keepclassmembers class com.example.poop.data.VaultHistory { *; }
-keep class com.example.poop.data.VaultHistory { *; }

# DAO 接口防止被优化
-keep interface com.example.poop.data.VaultDao { *; }

# 处理反射相关的异常堆栈保留
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# 记录日志相关的类
-keep class com.example.poop.util.Logcat { *; }
