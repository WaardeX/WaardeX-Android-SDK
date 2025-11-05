package com.waardex.adsdk

import android.content.Context
import android.util.Log

object WaardeXAdSDK {
    
    private const val TAG = "WaardeXAdSDK"
    private const val SDK_VERSION = "1.0.0"
    
    private var isInitialized = false
    private var applicationContext: Context? = null
    
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
