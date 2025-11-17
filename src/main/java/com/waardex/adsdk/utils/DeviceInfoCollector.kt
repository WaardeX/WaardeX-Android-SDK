package com.waardex.adsdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.waardex.adsdk.models.App
import com.waardex.adsdk.models.Device
import com.waardex.adsdk.models.Geo
import com.waardex.adsdk.models.Publisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Locale

internal object DeviceInfoCollector {
    
    suspend fun collectDeviceInfo(context: Context): Device {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Get Google Advertising ID (GAID) - much better than ANDROID_ID
        val (gaid, limitTracking) = getAdvertisingId(context)

        // Get IP address
        val ipAddress = getDeviceIpAddress()

        // Get connection type
        val connectionType = getConnectionType(context)

        // Get better User Agent from WebView
        val userAgent = try {
            WebView.getDefaultUserAgent(context)
        } catch (e: Exception) {
            System.getProperty("http.agent") ?: "Android"
        }

        // Get Geo if permission available
        val geo = getGeoLocation(context)

        return Device(
            userAgent = userAgent,
            geo = geo,
            ip = ipAddress,
            deviceType = 4, // Mobile/Tablet
            make = Build.MANUFACTURER.capitalize(),
            model = Build.MODEL,
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            width = displayMetrics.widthPixels,
            height = displayMetrics.heightPixels,
            ppi = displayMetrics.densityDpi,
            pixelRatio = displayMetrics.density,
            language = Locale.getDefault().language,
            advertisingId = gaid,
            limitAdTracking = if (limitTracking) 1 else 0,
            connectionType = connectionType
        )
    }
    
    fun collectAppInfo(context: Context): App {
        val packageName = context.packageName
        val packageManager = context.packageManager

        val appName = try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val appVersion = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }

        // Generate Play Store URL
        val storeUrl = "https://play.google.com/store/apps/details?id=$packageName"

        return App(
            id = packageName,
            name = appName,
            bundle = packageName,
            storeUrl = storeUrl,
            version = appVersion,
            publisher = Publisher(id = packageName, name = appName)
        )
    }
    
    fun generateUserId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    /**
     * Get Google Advertising ID (GAID) - much better quality than ANDROID_ID
     * Returns pair of (advertisingId, limitAdTracking)
     */
    private suspend fun getAdvertisingId(context: Context): Pair<String?, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                Pair(adInfo.id, adInfo.isLimitAdTrackingEnabled)
            } catch (e: GooglePlayServicesNotAvailableException) {
                // Google Play Services not available - fallback to ANDROID_ID
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                Pair(androidId, false)
            } catch (e: GooglePlayServicesRepairableException) {
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                Pair(androidId, false)
            } catch (e: IOException) {
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                Pair(androidId, false)
            } catch (e: Exception) {
                Pair(null, false)
            }
        }
    }

    /**
     * Get device IP address - important for geo-targeting
     */
    private fun getDeviceIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress
                        // Return IPv4 address
                        if (hostAddress?.indexOf(':') == -1) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    /**
     * Get connection type: 0=unknown, 1=Ethernet, 2=WiFi, 3=Cell-Unknown, 4=Cell-2G, 5=Cell-3G, 6=Cell-4G, 7=Cell-5G
     */
    private fun getConnectionType(context: Context): Int {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return 0
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0

                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 2
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        // Try to determine cell generation
                        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                        when (telephonyManager?.networkType) {
                            TelephonyManager.NETWORK_TYPE_GPRS,
                            TelephonyManager.NETWORK_TYPE_EDGE,
                            TelephonyManager.NETWORK_TYPE_CDMA,
                            TelephonyManager.NETWORK_TYPE_1xRTT,
                            TelephonyManager.NETWORK_TYPE_IDEN -> 4 // 2G

                            TelephonyManager.NETWORK_TYPE_UMTS,
                            TelephonyManager.NETWORK_TYPE_EVDO_0,
                            TelephonyManager.NETWORK_TYPE_EVDO_A,
                            TelephonyManager.NETWORK_TYPE_HSDPA,
                            TelephonyManager.NETWORK_TYPE_HSUPA,
                            TelephonyManager.NETWORK_TYPE_HSPA,
                            TelephonyManager.NETWORK_TYPE_EVDO_B,
                            TelephonyManager.NETWORK_TYPE_EHRPD,
                            TelephonyManager.NETWORK_TYPE_HSPAP -> 5 // 3G

                            TelephonyManager.NETWORK_TYPE_LTE -> 6 // 4G

                            31 -> 7 // 5G NR (NETWORK_TYPE_NR = 20, but using constant 31 for compatibility)

                            else -> 3 // Cell-Unknown
                        }
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 1
                    else -> 0
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return 0
                return when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> 2
                    ConnectivityManager.TYPE_MOBILE -> 3 // Cell-Unknown (can't determine generation on old API)
                    ConnectivityManager.TYPE_ETHERNET -> 1
                    else -> 0
                }
            }
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Get geo location if permission is granted
     */
    private fun getGeoLocation(context: Context): Geo? {
        try {
            // Get country code (ISO 3166-1 alpha-3)
            val countryCode = getCountryCode(context)

            // Check if location permission is granted
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasCoarseLocation && !hasFineLocation) {
                // No location permission, but can still send country
                return if (countryCode != null) {
                    Geo(country = countryCode, type = 2)
                } else {
                    null
                }
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return if (countryCode != null) {
                    Geo(country = countryCode, type = 2)
                } else {
                    null
                }

            // Get last known location
            val providers = locationManager.getProviders(true)
            var lastKnownLocation: android.location.Location? = null

            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (lastKnownLocation == null || location.accuracy < lastKnownLocation.accuracy) {
                    lastKnownLocation = location
                }
            }

            return if (lastKnownLocation != null) {
                Geo(
                    latitude = lastKnownLocation.latitude,
                    longitude = lastKnownLocation.longitude,
                    country = countryCode,
                    type = if (hasFineLocation) 1 else 2 // 1=GPS, 2=IP/WiFi
                )
            } else if (countryCode != null) {
                // No location but have country
                Geo(country = countryCode, type = 2)
            } else {
                null
            }
        } catch (e: SecurityException) {
            // Permission not granted - try to get at least country
            val countryCode = getCountryCode(context)
            return if (countryCode != null) {
                Geo(country = countryCode, type = 2)
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get country code (ISO 3166-1 alpha-3) from SIM or system locale
     */
    private fun getCountryCode(context: Context): String? {
        try {
            // First try to get from SIM card (most accurate for mobile targeting)
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = telephonyManager?.simCountryIso?.uppercase()

            if (!simCountry.isNullOrEmpty() && simCountry.length == 2) {
                // Convert ISO 3166-1 alpha-2 to alpha-3
                return convertCountryCodeToAlpha3(simCountry)
            }

            // Fallback to network country (from cell tower)
            val networkCountry = telephonyManager?.networkCountryIso?.uppercase()
            if (!networkCountry.isNullOrEmpty() && networkCountry.length == 2) {
                return convertCountryCodeToAlpha3(networkCountry)
            }

            // Fallback to system locale
            val localeCountry = Locale.getDefault().country.uppercase()
            if (localeCountry.isNotEmpty() && localeCountry.length == 2) {
                return convertCountryCodeToAlpha3(localeCountry)
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    /**
     * Convert ISO 3166-1 alpha-2 to alpha-3 country code
     */
    private fun convertCountryCodeToAlpha3(alpha2: String): String? {
        return try {
            val locale = Locale("", alpha2)
            locale.isO3Country
        } catch (e: Exception) {
            null
        }
    }
}
