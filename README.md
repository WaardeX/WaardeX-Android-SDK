# WaardeX Ad SDK for Android

Android SDK for displaying ads via OpenRTB protocol.

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
- **Rich data enrichment** for better bid rates:
  - Google Advertising ID (GAID) with limit tracking support
  - IP address detection for geo-targeting
  - **IP-based geo location** with MaxMind GeoLite2-City database
    - Automatic download on first initialization (~70 MB)
    - Country + City detection from IP address
    - Auto-updates every 30 days
    - Works without location permissions
  - Detailed connection type (WiFi, 2G, 3G, 4G, 5G)
  - High-quality User Agent from WebView
  - GPS/WiFi location (if permissions granted)
  - Play Store URL for app verification
  - Device metrics, manufacturer, OS version

## GeoIP Database

The SDK automatically downloads **MaxMind GeoLite2-City** database (~70 MB) on first initialization. This enables accurate country and city detection from IP addresses without requiring location permissions.

**Features:**
- Automatic download in background (non-blocking)
- Auto-updates every 30 days
- Fallback to SIM-based country if database unavailable
- No API keys required (free public database)

**First launch behavior:**
- SDK initializes immediately
- GeoIP database downloads in background (~70 MB)
- Ads work during download using SIM-based country
- Once downloaded, IP-based city/country available for all subsequent requests

**Data priority:**
1. **GPS/WiFi location** (if permissions granted) - most accurate
2. **IP-based location** (from GeoLite2 database) - good accuracy, no permissions
3. **SIM-based country** (fallback) - country only

## Supported Video Formats

### Rewarded Video
- **VAST XML** - Automatically parsed, MediaFile URL extracted
- **HTML5 video tag** - Rendered in WebView with JavaScript event tracking
- **Video formats**: MP4, WebM, 3GPP

## Error Handling

The SDK provides error codes to differentiate between "no fill" (no ads available) and actual errors:

```kotlin
rewardedVideoAd.setAdListener(object : RewardedVideoAdListener {
    override fun onAdFailedToLoad(error: AdError) {
        when (error.code) {
            AdErrorCode.NO_FILL -> {
                // Normal situation - no ads available right now
                // This is NOT an error, just retry later
                Log.d(TAG, "No ads available")
            }
            AdErrorCode.NETWORK_ERROR -> {
                // Network connectivity issues
                Log.e(TAG, "Network error: ${error.message}")
            }
            AdErrorCode.TIMEOUT -> {
                // Request timeout - server took too long
                Log.e(TAG, "Request timeout: ${error.message}")
                // Retry with exponential backoff or check server status
            }
            AdErrorCode.INVALID_REQUEST -> {
                // Invalid ad request or configuration
                Log.e(TAG, "Invalid request: ${error.message}")
            }
            AdErrorCode.INTERNAL_ERROR -> {
                // SDK internal error
                Log.e(TAG, "Internal error: ${error.message}")
            }
            AdErrorCode.SDK_NOT_INITIALIZED -> {
                // SDK not initialized
                Log.e(TAG, "SDK not initialized")
            }
            else -> {
                Log.e(TAG, "Unknown error: ${error.message}")
            }
        }
    }
})
```

### Error Codes

- **`NO_FILL` (0)** - No ads available (not an error, normal business situation)
- **`NETWORK_ERROR` (1)** - Network connectivity issues
- **`TIMEOUT` (2)** - Request timeout (server took too long to respond)
- **`INVALID_REQUEST` (3)** - Invalid ad request or configuration
- **`INTERNAL_ERROR` (4)** - SDK internal error
- **`SDK_NOT_INITIALIZED` (5)** - SDK not initialized
- **`UNKNOWN` (99)** - Unknown error

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

## Permissions

### Required Permissions

Add these to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Optional Permissions (for better bid rates)

These permissions improve ad targeting and revenue but are not required:

```xml
<!-- For geo-targeting (improves CPM by ~15-30%) -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

**Note**: The SDK automatically detects available permissions and collects only the data it has access to. If location permissions are not granted, geo data will not be included in bid requests.

## Support

Website: https://waardex.com
