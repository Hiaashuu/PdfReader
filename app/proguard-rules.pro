# Set optimization passes to 5 for more scrambling
-optimizationpasses 5

# Move all classes to the root package to destroy structure
-repackageclasses ''

# Rename class names found inside strings
-adaptclassstrings

# Allow R8 to change method visibility (e.g., public to private)
-allowaccessmodification

# Rename methods to the same name (confuses decompilers)
-overloadaggressively

# Keep only essential attributes for reflection, remove debug info
-keepattributes Annotation, InnerClasses, EnclosingMethod, Signature, Exceptions

# Rename the original source file to a generic name
-renamesourcefileattribute SourceFile

# Keep native (JNI) methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Kotlin metadata (critical for Kotlin apps)
-keepclassmembers class kotlin.Metadata { *; }

# Keep ViewBinding and DataBinding classes
-keep class Binding { *; }
-keepclassmembers class * extends androidx.databinding.ViewDataBinding {
    public static  inflate(...);
    public static  bind(...);
}

# Keep Android component entry points (Application, Activity, etc.)
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.core.app.CoreComponentFactory

# Keep Activity constructors
-keepclassmembers class * extends android.app.Activity {
    public <init>(...);
}

# Keep Custom View constructors used in XML layouts
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Base64 decoder for string deobfuscation
-keep class android.util.Base64 {
    public static byte[] decode(java.lang.String, int);
}