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

# --- ML Kit / Firebase 依赖注入系统修复 ---

# 1. 核心修复：保留所有组件注册器。
# ML Kit 使用 Firebase 组件系统，注册器通过反射加载。保留实现该接口的类及其构造函数。
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}

# 2. 保留 ML Kit 初始化的核心入口点
-keep class com.google.mlkit.common.internal.MlKitInitProvider
-keep class com.google.mlkit.common.sdkinternal.MlKitContext { *; }

# 3. 针对 Barcode Scanning GMS 版本的 API 保护
# 仅保留公共接口和方法，允许混淆内部实现
-keep class com.google.mlkit.vision.barcode.** {
    public <methods>;
    protected <methods>;
}

# 4. 关键：处理 Google Play Services 的 KeepName 注解
# GMS 库内部大量使用此注解来标记“严禁混淆/删除”的方法。这是解决 Unsatisfied dependency 的关键。
-keep class com.google.android.gms.common.annotation.KeepName
-keep @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# 5. 保留内部可能通过反射实例化的构造函数，但不阻止重命名类名
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** {
    <init>(...);
}

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
