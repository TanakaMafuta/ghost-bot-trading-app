# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# Keep all Compose classes
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-keep class com.squareup.retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class kotlin.Result { *; }
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep Moshi classes
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-if class *
-keepclassmembers class <1> {
    <init>(...);
}
-keep class kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoaderImpl
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep WebSocket classes
-keep class com.tinder.scarlet.** { *; }

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }

# Keep Room classes
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.TypeConverter

# Keep Security classes
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }

# Keep data classes and API models
-keep class com.ghostbot.trading.data.** { *; }
-keep class com.ghostbot.trading.domain.model.** { *; }

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}