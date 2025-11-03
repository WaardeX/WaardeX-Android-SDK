-keep class com.waardex.adsdk.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.waardex.adsdk.models.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}
