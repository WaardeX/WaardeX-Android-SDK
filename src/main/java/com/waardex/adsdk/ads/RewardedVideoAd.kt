package com.waardex.adsdk.ads

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.VideoView
import com.waardex.adsdk.core.AdError
import com.waardex.adsdk.core.AdLoadListener
import com.waardex.adsdk.core.AdManager
import com.waardex.adsdk.core.LoadedAd
import com.waardex.adsdk.utils.VastParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RewardedVideoAd(private val activity: Activity) {

    private val TAG = "RewardedVideoAd"
    private val adManager = AdManager()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadedAd: LoadedAd? = null
    private var vastData: VastParser.VastData? = null
    private var listener: RewardedVideoAdListener? = null
    private var dialog: Dialog? = null
    private var isLoading = false
    private var videoCompleted = false

    fun loadAd() {
        if (isLoading) {
            Log.w(TAG, "Ad is already loading")
            return
        }

        isLoading = true
        loadedAd = null
        vastData = null

        adManager.loadRewardedVideoAd(activity, object : AdLoadListener {
            override fun onAdLoaded(ad: LoadedAd) {
                isLoading = false
                loadedAd = ad

                // Перевіряємо чи це VAST XML
                if (VastParser.isVastXml(ad.adMarkup)) {
                    vastData = VastParser.parse(ad.adMarkup)
                    if (vastData != null) {
                        listener?.onAdLoaded()
                    } else {
                        listener?.onAdFailedToLoad(AdError("Failed to parse VAST XML"))
                    }
                } else {
                    // HTML5 video
                    listener?.onAdLoaded()
                }
            }

            override fun onAdFailedToLoad(error: AdError) {
                isLoading = false
                listener?.onAdFailedToLoad(error)
            }
        })
    }

    fun isReady(): Boolean = loadedAd != null

    fun show() {
        val ad = loadedAd

        if (ad == null) {
            listener?.onAdFailedToShow(AdError("Ad not loaded"))
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            listener?.onAdFailedToShow(AdError("Activity not available"))
            return
        }

        activity.runOnUiThread {
            try {
                videoCompleted = false
                if (vastData != null) {
                    showVastVideo(ad, vastData!!)
                } else {
                    showHtmlVideo(ad)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing ad", e)
                listener?.onAdFailedToShow(AdError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun showVastVideo(ad: LoadedAd, vast: VastParser.VastData) {
        dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setCancelable(false)

            val container = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
            }

            val progressBar = ProgressBar(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }
            container.addView(progressBar)

            val videoView = VideoView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                setOnPreparedListener { mp ->
                    progressBar.visibility = android.view.View.GONE
                    mp.setOnVideoSizeChangedListener { _, _, _ ->
                        // Центруємо відео
                        val videoWidth = mp.videoWidth
                        val videoHeight = mp.videoHeight
                        val screenWidth = activity.resources.displayMetrics.widthPixels
                        val screenHeight = activity.resources.displayMetrics.heightPixels

                        val scale = minOf(
                            screenWidth.toFloat() / videoWidth,
                            screenHeight.toFloat() / videoHeight
                        )

                        layoutParams = FrameLayout.LayoutParams(
                            (videoWidth * scale).toInt(),
                            (videoHeight * scale).toInt()
                        ).apply {
                            gravity = Gravity.CENTER
                        }
                    }
                }

                setOnCompletionListener {
                    videoCompleted = true
                    listener?.onUserEarnedReward()
                    fireTrackingEvent(vast, "complete")
                    dismiss()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Video error: what=$what, extra=$extra")
                    listener?.onAdFailedToShow(AdError("Video playback error"))
                    dismiss()
                    true
                }

                setVideoPath(vast.mediaFileUrl)
                start()
            }

            container.addView(videoView)
            setContentView(container)

            setOnDismissListener {
                listener?.onAdDismissed()
                loadedAd = null
                vastData = null
            }

            show()
            listener?.onAdShown()

            // Fire impression tracking
            adManager.fireImpression(ad)
            vast.impressionUrls.forEach { url ->
                scope.launch(Dispatchers.IO) {
                    adManager.fireTrackingPixel(url)
                }
            }
            fireTrackingEvent(vast, "start")
        }
    }

    private fun showHtmlVideo(ad: LoadedAd) {
        dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setCancelable(false)

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
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        listener?.onAdImpression()
                        adManager.fireImpression(ad)
                    }
                }

                // Inject HTML5 video event listeners
                val htmlWithListeners = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body { margin: 0; padding: 0; background: #000; }
                            video { width: 100%; height: 100vh; object-fit: contain; }
                        </style>
                    </head>
                    <body>
                        ${ad.adMarkup}
                        <script>
                            var video = document.querySelector('video');
                            if (video) {
                                video.addEventListener('ended', function() {
                                    Android.onVideoCompleted();
                                });
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onVideoCompleted() {
                        activity.runOnUiThread {
                            videoCompleted = true
                            listener?.onUserEarnedReward()
                            dismiss()
                        }
                    }
                }, "Android")

                loadDataWithBaseURL(null, htmlWithListeners, "text/html", "UTF-8", null)
            }

            container.addView(webView)
            setContentView(container)

            setOnDismissListener {
                listener?.onAdDismissed()
                loadedAd = null
            }

            show()
            listener?.onAdShown()
        }
    }

    private fun fireTrackingEvent(vast: VastParser.VastData, eventName: String) {
        vast.trackingEvents[eventName]?.forEach { url ->
            scope.launch(Dispatchers.IO) {
                adManager.fireTrackingPixel(url)
            }
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun setAdListener(listener: RewardedVideoAdListener) {
        this.listener = listener
    }

    fun destroy() {
        dismiss()
        adManager.destroy()
        scope.cancel()
    }
}

interface RewardedVideoAdListener {
    fun onAdLoaded() {}
    fun onAdFailedToLoad(error: AdError) {}
    fun onAdShown() {}
    fun onAdFailedToShow(error: AdError) {}
    fun onAdImpression() {}
    fun onUserEarnedReward() {}
    fun onAdDismissed() {}
}
