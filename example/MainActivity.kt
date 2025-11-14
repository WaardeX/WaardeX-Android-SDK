package com.example.myapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.waardex.adsdk.ads.InterstitialAd
import com.waardex.adsdk.ads.RewardedVideoAd
import com.waardex.adsdk.ads.RewardedVideoAdListener
import com.waardex.adsdk.core.AdError
import com.waardex.adsdk.views.BannerAdView

class MainActivity : AppCompatActivity() {

    private lateinit var bannerAd: BannerAdView
    private lateinit var interstitialAd: InterstitialAd
    private lateinit var rewardedVideoAd: RewardedVideoAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load banner ad
        bannerAd = findViewById(R.id.bannerAd)
        bannerAd.loadAd()

        // Load interstitial ad
        interstitialAd = InterstitialAd(this)
        interstitialAd.loadAd()

        // Load rewarded video ad
        rewardedVideoAd = RewardedVideoAd(this)
        rewardedVideoAd.setAdListener(object : RewardedVideoAdListener {
            override fun onAdLoaded() {
                Log.d("MainActivity", "Rewarded video loaded")
                Toast.makeText(this@MainActivity, "Rewarded video ready!", Toast.LENGTH_SHORT).show()
            }

            override fun onAdFailedToLoad(error: AdError) {
                when (error.code) {
                    com.waardex.adsdk.core.AdErrorCode.NO_FILL -> {
                        Log.d("MainActivity", "No ads available right now")
                        // This is normal - no ads to show, try again later
                    }
                    com.waardex.adsdk.core.AdErrorCode.NETWORK_ERROR -> {
                        Log.e("MainActivity", "Network error: ${error.message}")
                        // Check internet connection
                    }
                    com.waardex.adsdk.core.AdErrorCode.TIMEOUT -> {
                        Log.e("MainActivity", "Request timeout: ${error.message}")
                        // Retry or check server status
                    }
                    else -> {
                        Log.e("MainActivity", "Ad failed to load: ${error.message} (code: ${error.code})")
                    }
                }
            }

            override fun onUserEarnedReward() {
                Log.d("MainActivity", "User earned reward!")
                Toast.makeText(this@MainActivity, "You earned a reward!", Toast.LENGTH_SHORT).show()
                // Give reward to user here
            }

            override fun onAdDismissed() {
                Log.d("MainActivity", "Rewarded video dismissed")
                // Load next ad
                rewardedVideoAd.loadAd()
            }
        })
        rewardedVideoAd.loadAd()

        // Interstitial button
        findViewById<Button>(R.id.showInterstitialButton).setOnClickListener {
            if (interstitialAd.isReady()) {
                interstitialAd.show()
            } else {
                Toast.makeText(this, "Interstitial not ready", Toast.LENGTH_SHORT).show()
            }
        }

        // Rewarded video button
        findViewById<Button>(R.id.showRewardedVideoButton).setOnClickListener {
            if (rewardedVideoAd.isReady()) {
                rewardedVideoAd.show()
            } else {
                Toast.makeText(this, "Rewarded video not ready", Toast.LENGTH_SHORT).show()
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
        rewardedVideoAd.destroy()
    }
}
