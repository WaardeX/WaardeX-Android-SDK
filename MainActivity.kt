package com.example.myapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.waardex.adsdk.ads.InterstitialAd
import com.waardex.adsdk.views.BannerAdView

class MainActivity : AppCompatActivity() {
    
    private lateinit var bannerAd: BannerAdView
    private lateinit var interstitialAd: InterstitialAd
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        bannerAd = findViewById(R.id.bannerAd)
        bannerAd.loadAd()
        
        interstitialAd = InterstitialAd(this)
        interstitialAd.loadAd()
        
        findViewById<Button>(R.id.showInterstitialButton).setOnClickListener {
            if (interstitialAd.isReady()) {
                interstitialAd.show()
            }
        }
    }
    
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
    }
}
