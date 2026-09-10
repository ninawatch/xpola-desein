# ------------------------------------
#   GENERAL
# ------------------------------------
-keepattributes SourceFile,LineNumberTable
-allowaccessmodification
-repackageclasses 'com.xpola.player.internal'

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the security config classes but allow renaming of members not accessed by native
-keep class com.xpola.player.Sec.ConfigLoader {
    native <methods>;
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

-keep class com.android.vending.licensing.ILicensingService

# ------------------------------------
#   GSON
# ------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ------------------------------------
#   PICASSO
# ------------------------------------
-dontwarn com.squareup.picasso.**

# ------------------------------------
#   FIREBASE (FULL)
# ------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Installations
-keep class com.google.firebase.installations.** { *; }

# Messaging
-keep class com.google.firebase.messaging.** { *; }

# Analytics
-keep class com.google.firebase.analytics.** { *; }

# Auth
-keep class com.google.firebase.auth.** { *; }

# ------------------------------------
#   GOOGLE PLAY SERVICES (FULL)
# ------------------------------------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.common.internal.safeparcel.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.base.** { *; }
-keep class com.google.android.gms.dynamic.** { *; }

# ------------------------------------
#   ADMOB ADS
# ------------------------------------
-keep public class com.google.android.gms.ads.** { *; }
-keep public class com.google.android.ads.** { *; }

# ------------------------------------
#   JAVASCRIPT INTERFACE
# ------------------------------------
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ------------------------------------
#   PARCELABLE
# ------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ------------------------------------
#   FACEBOOK ANNOTATIONS
# ------------------------------------
-dontwarn com.facebook.infer.annotation.Nullsafe*

# ------------------------------------
#   EXTRA KEEP RULES FROM missing_rules.txt
# ------------------------------------
-dontwarn com.arthenica.smartexception.java.Exceptions
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# ------------------------------------
#   VLC LIBRARY
# ------------------------------------
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.**
