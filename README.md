# WaardeX Ad SDK for Android

Android SDK for displaying ads.

## Quick Start

### 1. Add SDK

**settings.gradle:**
```gradle
include ':app', ':waardex-ad-sdk'
```

**app/build.gradle:**
```gradle
dependencies {
    implementation project(':waardex-ad-sdk')
}
```

### 2. Initialize

```kotlin
import com.waardex.adsdk.WaardeXAdSDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WaardeXAdSDK.initialize(
            context = this,
            name = "YOUR_NAME",
            password = "YOUR_PASSWORD",
            debug = true
        )
    }
}
```

### 3. Banner Ad

**XML:**
```xml
<com.waardex.adsdk.views.BannerAdView
    android:id="@+id/bannerAd"
    android:layout_width="320dp"
    android:layout_height="50dp" />
```

**Kotlin:**
```kotlin
bannerAd.loadAd()
```

### 4. Interstitial Ad

```kotlin
val interstitialAd = InterstitialAd(this)
interstitialAd.loadAd()

if (interstitialAd.isReady()) {
    interstitialAd.show()
}
```

## Features

- Banner ads (320x50, 300x250, 728x90)
- Interstitial ads (full-screen)
- OpenRTB 2.5 protocol
- Automatic tracking
- Debug mode

## Support

Website: https://waardex.com
