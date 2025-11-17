package com.waardex.adsdk.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.waardex.adsdk.core.AdError
import com.waardex.adsdk.core.AdLoadListener
import com.waardex.adsdk.core.AdManager
import com.waardex.adsdk.core.LoadedAd

class InterstitialAd(private val activity: Activity) {

    private val TAG = "InterstitialAd"
    private val adManager = AdManager()
    private var loadedAd: LoadedAd? = null
    private var loadedTime: Long = 0
    private var impressionFired: Boolean = false
    private var listener: InterstitialAdListener? = null
    private var dialog: Dialog? = null
    private var isLoading = false

    companion object {
        private const val AD_TTL_MS = 3600_000L // 1 hour
    }
    
    fun loadAd() {
        if (isLoading) {
            Log.w(TAG, "Ad is already loading")
            return
        }

        isLoading = true
        loadedAd = null
        loadedTime = 0
        impressionFired = false

        adManager.loadInterstitialAd(activity, object : AdLoadListener {
            override fun onAdLoaded(ad: LoadedAd) {
                isLoading = false
                loadedAd = ad
                loadedTime = System.currentTimeMillis()
                impressionFired = false
                listener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: AdError) {
                isLoading = false
                listener?.onAdFailedToLoad(error)
            }
        })
    }
    
    fun isReady(): Boolean {
        val ad = loadedAd ?: return false

        // Check TTL
        if (System.currentTimeMillis() - loadedTime > AD_TTL_MS) {
            Log.w(TAG, "Ad expired (TTL exceeded)")
            loadedAd = null
            loadedTime = 0
            impressionFired = false
            return false
        }

        return true
    }

    fun show() {
        val ad = loadedAd

        if (ad == null) {
            listener?.onAdFailedToShow(AdError("Ad not loaded", com.waardex.adsdk.core.AdErrorCode.INVALID_REQUEST))
            return
        }

        // Check TTL
        if (System.currentTimeMillis() - loadedTime > AD_TTL_MS) {
            loadedAd = null
            loadedTime = 0
            impressionFired = false
            listener?.onAdFailedToShow(AdError("Ad expired", com.waardex.adsdk.core.AdErrorCode.INVALID_REQUEST))
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            listener?.onAdFailedToShow(AdError("Activity not available", com.waardex.adsdk.core.AdErrorCode.INTERNAL_ERROR))
            return
        }

        // Mark ad as used immediately to prevent double-show
        loadedAd = null
        loadedTime = 0

        activity.runOnUiThread {
            try {
                showAdDialog(ad)
            } catch (e: Exception) {
                listener?.onAdFailedToShow(AdError(e.message ?: "Unknown error", com.waardex.adsdk.core.AdErrorCode.INTERNAL_ERROR))
            }
        }
    }
    
    private fun showAdDialog(ad: LoadedAd) {
        dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            
            val container = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            
            val webView = WebView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
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

                        // Fire impression only once to prevent fraud
                        if (!impressionFired) {
                            listener?.onAdImpression()
                            adManager.fireImpression(ad)
                            impressionFired = true
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        url?.let {
                            listener?.onAdClicked()
                            dismiss()
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(it)
                                )
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to open URL: $it", e)
                            }
                        }
                        return true
                    }
                }
                
                loadDataWithBaseURL(null, ad.adMarkup, "text/html", "UTF-8", null)
            }
            
            container.addView(webView)
            
            val closeButton = android.widget.ImageButton(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(40)
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    setMargins(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
                }
                
                setBackgroundColor(Color.argb(128, 0, 0, 0))
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                setOnClickListener { dismiss() }
            }
            
            container.addView(closeButton)
            setContentView(container)

            setOnDismissListener {
                listener?.onAdDismissed()
                // loadedAd is already cleared in show() to prevent double-show
            }

            setCancelable(true)
            show()
            listener?.onAdShown()
        }
    }
    
    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
    
    fun setAdListener(listener: InterstitialAdListener) {
        this.listener = listener
    }
    
    fun destroy() {
        dismiss()
        adManager.destroy()
    }
    
    private fun dpToPx(dp: Int): Int {
        val density = activity.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}

interface InterstitialAdListener {
    fun onAdLoaded() {}
    fun onAdFailedToLoad(error: AdError) {}
    fun onAdShown() {}
    fun onAdFailedToShow(error: AdError) {}
    fun onAdImpression() {}
    fun onAdClicked() {}
    fun onAdDismissed() {}
}
