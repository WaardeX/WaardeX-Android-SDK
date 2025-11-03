# WaardeX-Android-SDK

# WaardeX Ad SDK - Complete Package

##Download Complete SDK

**Package Name:** `waardex-ad-sdk-android`

## What's Included

### Core SDK Files :
- `WaardeXAdSDK.kt` - Main SDK class
- `BidRequestModels.kt` - OpenRTB 2.5 request models  
- `BidResponseModels.kt` - OpenRTB 2.5 response models
- `OpenRTBClient.kt` - HTTP network client
- All other implementation files

### Configuration:
- `build.gradle` with package `com.waardex.adsdk`
- `proguard-rules.pro`
- `AndroidManifest.xml`

## Quick Start

```kotlin
import com.waardex.adsdk.WaardeXAdSDK

WaardeXAdSDK.initialize(
    context = this,
    name = "YOUR_NAME",
    password = "YOUR_PASSWORD", 
    debug = true
)
```

## Banner Ad

```xml
<com.waardex.adsdk.views.BannerAdView
    android:layout_width="320dp"
    android:layout_height="50dp" />
```

## Interstitial Ad

```kotlin
val interstitial = InterstitialAd(this)
interstitial.loadAd()
if (interstitial.isReady()) {
    interstitial.show()
}
```

## Key Features

✅ OpenRTB 2.5 Protocol
✅ Banner & Interstitial Ads  
✅ Automatic Tracking
✅ Debug Mode
✅ Production Ready

## Support

Website: https://waardex.com
Email: support@waardex.com

---

Made with ❤️ by WaardeX
