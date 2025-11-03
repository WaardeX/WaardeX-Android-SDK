package com.waardex.adsdk.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.waardex.adsdk.core.AdError
import com.waardex.adsdk.core.AdLoadListener
import com.waardex.adsdk.core.AdManager
import com.waardex.adsdk.core.LoadedAd

class BannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val TAG = "BannerAdView"
    private val adManager = AdManager()
    private var webView: WebView? = null
    private var listener: BannerAdListener? = null
    private var loadedAd: LoadedAd? = null
    private var isAdLoaded = false
    
    object AdSize {
        val BANNER_320x50 = Pair(320, 50)
        val BANNER_300x250 = Pair(300, 250)
        val BANNER_728x90 = Pair(728, 90)
    }
    
    init {
        setupWebView()
    }
    
    private fun setupWebView() {
        webView = WebView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (isAdLoaded) {
                        listener?.onAdImpression()
                        loadedAd?.let { adManager.fireImpression(it) }
                    }
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        listener?.onAdClicked()
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(it)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open URL: $it", e)
                        }
                    }
                    return true
                }
            }
        }
        
        addView(webView)
    }
    
    fun loadAd() {
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "BannerAdView must have explicit width and height")
            listener?.onAdFailedToLoad(AdError("Invalid size"))
            return
        }
        
        val density = resources.displayMetrics.density
        val widthDp = (width / density).toInt()
        val heightDp = (height / density).toInt()
        
        isAdLoaded = false
        
        adManager.loadBannerAd(context, widthDp, heightDp, object : AdLoadListener {
            override fun onAdLoaded(ad: LoadedAd) {
                loadedAd = ad
                isAdLoaded = true
                displayAd(ad)
                listener?.onAdLoaded()
            }
            
            override fun onAdFailedToLoad(error: AdError) {
                Log.e(TAG, "Failed to load ad: ${error.message}")
                listener?.onAdFailedToLoad(error)
            }
        })
    }
    
    private fun displayAd(ad: LoadedAd) {
        post {
            try {
                webView?.loadDataWithBaseURL(null, ad.adMarkup, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to display ad", e)
                listener?.onAdFailedToLoad(AdError("Failed to display ad"))
            }
        }
    }
    
    fun setAdListener(listener: BannerAdListener) {
        this.listener = listener
    }
    
    fun pause() {
        webView?.onPause()
    }
    
    fun resume() {
        webView?.onResume()
    }
    
    fun destroy() {
        webView?.destroy()
        webView = null
        adManager.destroy()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }
}

interface BannerAdListener {
    fun onAdLoaded() {}
    fun onAdFailedToLoad(error: AdError) {}
    fun onAdImpression() {}
    fun onAdClicked() {}
}
