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

### 5. Rewarded Video Ad

```kotlin
val rewardedVideoAd = RewardedVideoAd(this)
rewardedVideoAd.setAdListener(object : RewardedVideoAdListener {
    override fun onAdLoaded() {
        // Ad is ready to show
    }

    override fun onUserEarnedReward() {
        // Give reward to user
        giveRewardToUser()
    }

    override fun onAdDismissed() {
        // Load next ad
        rewardedVideoAd.loadAd()
    }
})
rewardedVideoAd.loadAd()

// Show when ready
if (rewardedVideoAd.isReady()) {
    rewardedVideoAd.show()
}
```

## Features

- **Banner ads** (320x50, 300x250, 728x90)
- **Interstitial ads** (full-screen)
- **Rewarded video ads** (full-screen with reward callback)
  - VAST XML support with automatic parsing
  - HTML5 video support via WebView
  - Native VideoView playback (no external dependencies)
  - Automatic VAST tracking events (impression, start, complete)
- **OpenRTB 2.5 protocol** compliant
- **Automatic tracking** for impressions and clicks
- **Debug mode** for development

## Supported Video Formats

### Rewarded Video
- **VAST XML** - Automatically parsed, MediaFile URL extracted
- **HTML5 video tag** - Rendered in WebView with JavaScript event tracking
- **Video formats**: MP4, WebM, 3GPP

## Lifecycle Management

```kotlin
override fun onPause() {
    super.onPause()
    bannerAd.pause()
}

override fun onResume() {
    super.onResume()
    bannerAd.resume()
}

override fun onDestroy() {
    super.onDestroy()
    bannerAd.destroy()
    interstitialAd.destroy()
    rewardedVideoAd.destroy()
}
```

## Support

Website: https://waardex.com
