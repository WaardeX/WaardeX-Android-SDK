package com.waardex.adsdk

import android.content.Context
import android.util.Log
import com.waardex.adsdk.utils.GeoIPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WaardeXAdSDK {

    private const val TAG = "WaardeXAdSDK"
    private const val SDK_VERSION = "1.0.0"

    private var isInitialized = false
    private var applicationContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    internal var baseUrl: String = "https://useast.justbidit2.xyz:8843/"
    internal var appName: String = ""
    internal var appPassword: String = ""
    internal var isDebugMode: Boolean = false

    fun initialize(
        context: Context,
        name: String,
        password: String,
        debug: Boolean = false
    ) {
        if (isInitialized) {
            Log.w(TAG, "SDK already initialized")
            return
        }

        applicationContext = context.applicationContext
        appName = name
        appPassword = password
        isDebugMode = debug
        isInitialized = true

        if (debug) {
            Log.d(TAG, "WaardeX Ad SDK v$SDK_VERSION initialized successfully")
        }

        // Initialize GeoIP database in background
        scope.launch {
            val success = GeoIPManager.initialize(context.applicationContext)
            if (debug) {
                if (success) {
                    Log.d(TAG, "GeoIP database initialized successfully")
                } else {
                    Log.w(TAG, "GeoIP database initialization failed (will use SIM-based country)")
                }
            }
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    internal fun getContext(): Context {
        return applicationContext 
            ?: throw IllegalStateException("SDK not initialized. Call initialize() first.")
    }
    
    fun getVersion(): String = SDK_VERSION
    
    fun setBaseUrl(url: String) {
        baseUrl = url
    }
}
