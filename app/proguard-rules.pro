# ========== 基础属性（保留，用于崩溃日志和注解） ==========
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable

# ========== Hilt（只保留模块方法，组件由插件自带） ==========
-keep @dagger.Module class *
-keepclassmembers @dagger.Module class * {
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# ========== Room（必须） ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * { @androidx.room.* <fields>; }

# ========== Kotlinx Serialization（必须） ==========
-keep,includedescriptorclasses class com.aozijx.passly.**$$serializer { *; }
-keepclassmembers class com.aozijx.passly.** { *** Companion; }
-keepclasseswithmembers class com.aozijx.passly.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ========== Protobuf Lite（必须，防止字段名混淆） ==========
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

# ========== Firebase/ML Kit（必须） ==========
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }

# ========== 日志优化（可选，建议保留） ==========
-assumenosideeffects class android.util.Log { public static int v(...); public static int d(...); }