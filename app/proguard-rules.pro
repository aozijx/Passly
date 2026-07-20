# ============================================
# 通用属性保留
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# ============================================
# Dagger Hilt
# ============================================
# Hilt 注解和生成代码由 consumer-rules.pro 自动处理
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class *_HiltComponents { *; }
-keep class *_HiltModules { *; }
-keep class *_Factory { *; }
-keep class *_MembersInjector { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }

# ============================================
# Hilt DI 模块 - 基于注解保留
# ============================================
-keep @dagger.Module class *
-keepclassmembers @dagger.Module class * {
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# ============================================
# Room Database
# ============================================
-keep class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
# Room 实体中的枚举和序列化字段
-keepclassmembers class com.aozijx.passly.data.model.entity.** {
    <fields>;
    <init>(...);
}

# ============================================
# SQLCipher — JNI 方法由 AGP 默认规则保护，类由应用代码直接引用

# ============================================
# Kotlin Serialization
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aozijx.passly.**$$serializer { *; }
-keepclassmembers class com.aozijx.passly.** {
    *** Companion;
}
-keepclasseswithmembers class com.aozijx.passly.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================
# Protobuf Lite - 防止 R8 混淆 proto 字段名
# Proto 运行时通过反射按字段名访问，混淆后会导致
# "Field version_ for Xxx not found" 崩溃
# ============================================
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============================================
# Coroutines
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================
# Domain Models - 保留所有数据类及其字段
# ============================================
-keep class com.aozijx.passly.domain.model.** {
    <fields>;
    <init>(...);
}
-keep class com.aozijx.passly.data.model.** {
    <fields>;
    <init>(...);
}

# ============================================
# Enums - 保留 fromValue / valueOf
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** fromValue(int);
    public static ** fromValue(java.lang.String);
    <fields>;
}

# ============================================
# Navigation Compose
# ============================================
# Navigation 和 Hilt Navigation 的 consumer-rules.pro 已自动包含

# ============================================
# CameraX
# ============================================
-dontwarn androidx.camera.**

# ============================================
# Coil
# ============================================
-dontwarn coil.**

# ============================================
# ZXing（仅 BarcodeFormat + QRCodeWriter，应用代码直接引用，R8 自动保留）

# ============================================
# Argon2 — JNI 方法由 AGP 默认规则保护，Argon2Kt 等由应用代码直接引用

# ============================================
# ML Kit / Firebase 组件系统
# ============================================
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
-keep class com.google.mlkit.common.internal.MlKitInitProvider
-keep class com.google.mlkit.common.sdkinternal.MlKitContext { *; }

# ============================================
# Application 类
# ============================================
-keep class com.aozijx.passly.app.PasslyApplication { *; }
-keep class com.aozijx.passly.MainActivity { *; }

# ============================================
# Serializable / Parcelable
# ============================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# 日志优化 (release)
# ============================================
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(java.lang.String);
    public void print(java.lang.String);
}
