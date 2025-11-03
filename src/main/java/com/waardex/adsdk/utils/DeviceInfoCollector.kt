package com.waardex.adsdk.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.waardex.adsdk.models.App
import com.waardex.adsdk.models.Device
import com.waardex.adsdk.models.Publisher
import java.util.Locale

internal object DeviceInfoCollector {
    
    suspend fun collectDeviceInfo(context: Context): Device {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        
        return Device(
            userAgent = System.getProperty("http.agent") ?: "Android",
            deviceType = 4,
            make = Build.MANUFACTURER,
            model = Build.MODEL,
            os = "Android",
            osVersion = Build.VERSION.RELEASE,
            width = displayMetrics.widthPixels,
            height = displayMetrics.heightPixels,
            ppi = displayMetrics.densityDpi,
            pixelRatio = displayMetrics.density,
            language = Locale.getDefault().language,
            advertisingId = androidId,
            limitAdTracking = 0,
            connectionType = 0
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
        
        return App(
            id = packageName,
            name = appName,
            bundle = packageName,
            version = appVersion,
            publisher = Publisher(id = packageName, name = appName)
        )
    }
    
    fun generateUserId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
}
