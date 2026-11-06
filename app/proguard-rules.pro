# ProGuard & R8 Optimization Rules for Follow AI Auto Automation App

# 1. Preserve Core Android Accessibility & Services
-keep public class * extends android.accessibilityservice.AccessibilityService {
    public <init>();
    public *;
}

-keep public class * extends android.app.Service {
    public <init>();
    public *;
}

-keep class com.example.service.FollowAIEngineService { *; }
-keep class com.example.service.FloatingService { *; }
-keep class com.example.service.OverlayLifecycleOwner { *; }
-keep class com.example.controller.AutomationEngine { *; }

# 2. Preserve Data Models & JSON Serialization
-keep class com.example.model.** { *; }
-keepclassmembers class * implements java.io.Serializable { *; }
-keepclassmembers enum com.example.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public *;
}

# 3. Jetpack Compose & WindowManager ViewTree Owners
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.ViewTreeLifecycleOwner { *; }
-keep class androidx.lifecycle.ViewTreeViewModelStoreOwner { *; }
-keep class androidx.savedstate.ViewTreeSavedStateRegistryOwner { *; }
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,SourceFile,LineNumberTable

# 4. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# 5. Moshi & JSON Parsing
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
