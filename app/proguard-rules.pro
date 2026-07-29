-optimizationpasses 5

-repackageclasses ''

-adaptclassstrings

-allowaccessmodification

-overloadaggressively

-keepattributes Annotation, InnerClasses, EnclosingMethod, Signature, Exceptions

-renamesourcefileattribute SourceFile

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class kotlin.Metadata { *; }

-keep class Binding { *; }
-keepclassmembers class * extends androidx.databinding.ViewDataBinding {
    public static  inflate(...);
    public static  bind(...);
}

-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.core.app.CoreComponentFactory

-keepclassmembers class * extends android.app.Activity {
    public <init>(...);
}

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep class android.util.Base64 {
    public static byte[] decode(java.lang.String, int);
}