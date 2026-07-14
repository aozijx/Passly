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
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class *_HiltComponents { *; }
-keep class *_HiltModules { *; }
-keep class *_Factory { *; }
-keep class *_MembersInjector { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }

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
-keep class androidx.room.** { *; }
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
# SQLCipher / SQLite
# ============================================
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class androidx.sqlite.db.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

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
# DataStore Preferences
# ============================================
-keep class androidx.datastore.preferences.** { *; }
-keep class androidx.datastore.core.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
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
-keep class androidx.navigation.** { *; }

# ============================================
# Hilt Navigation Compose
# ============================================
-keep class androidx.hilt.navigation.compose.** { *; }

# ============================================
# CameraX
# ============================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ============================================
# Coil
# ============================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================
# ZXing / Barcode
# ============================================
-keep class com.google.zxing.** { *; }
-keep class com.google.mlkit.vision.barcode.** { *; }

# ============================================
# Argon2
# ============================================
-keep class com.lambdapioneer.argon2kt.** { *; }

# ============================================
# Credentials / Autofill
# ============================================
-keep class androidx.credentials.** { *; }
-keep class androidx.autofill.** { *; }

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
# DI 模块
# ============================================
-keep class com.aozijx.passly.di.** { *; }
-keep class com.aozijx.passly.data.di.** { *; }
-keep class com.aozijx.passly.core.di.** { *; }

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
