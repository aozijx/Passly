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
# 直接保留整个 Logcat 类及其所有成员，不加 static 限制
-keep class com.example.poop.util.Logcat { *; }

# 丢弃第三方库可能使用的标准 Log 调用
# 注意：Log 内部方法返回的是 int 而非 void
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    # 如果你想保留自己的 Info/Warn/Error 日志在 Logcat 窗口显示，
    # 这里的 i, w, e 规则可以去掉。
    # 如果你也想去掉控制台的 i, w, e（只看文件日志），则保留它们。
    # public static int i(...);
    # public static int w(...);
    # public static int e(...);
}

# 丢弃 System.out.println (很多老旧库喜欢用这个)
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
