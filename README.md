
## Quick Start

### 1. Add to Project

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

```xml

```

```kotlin
bannerAd.loadAd()
```

### 4. Interstitial Ad

```kotlin
val interstitial = InterstitialAd(this)
interstitial.loadAd()

if (interstitial.isReady()) {
    interstitial.show()
}
```

## Features

- OpenRTB 2.5 protocol
- Banner ads (320x50, 300x250, 728x90)
- Interstitial ads
- Automatic tracking
- Debug mode
- ProGuard rules included

## Technical Details

- Min SDK: Android 21+
- Language: Kotlin
- Dependencies: OkHttp, Gson, Coroutines
- Package: com.waardex.adsdk

## Support

Website: https://waardex.com

