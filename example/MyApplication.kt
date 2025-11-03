package com.example.myapp

import android.app.Application
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
