# Room 基础规则
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class * extends androidx.room.RoomDatabase

# SQLCipher / SQLite 相关规则
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class androidx.sqlite.db.SupportSQLite* { *; }

# 核心数据模型防止被混淆
-keep class com.example.poop.data.VaultEntry { *; }

# 核心工具类防止被混淆
-keep class com.example.poop.util.Logcat { *; }

# 丢弃第三方库可能使用的标准 Log 调用
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# 丢弃 System.out.println
-assumenosideeffects class java.io.PrintStream {
    public void println(java.lang.String);
    public void print(java.lang.String);
}

# 保留混淆后的堆栈信息以便 Retrace
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
