package com.waardex.adsdk.core

import android.content.Context
import android.util.Log
import com.waardex.adsdk.WaardeXAdSDK
import com.waardex.adsdk.models.*
import com.waardex.adsdk.network.NoBidException
import com.waardex.adsdk.network.OpenRTBClient
import com.waardex.adsdk.utils.DeviceInfoCollector
import kotlinx.coroutines.*
import java.util.UUID

internal class AdManager {
    private val TAG = "AdManager"
    private val client = OpenRTBClient()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun loadBannerAd(context: Context, width: Int, height: Int, listener: AdLoadListener) {
        if (!WaardeXAdSDK.isInitialized()) {
            listener.onAdFailedToLoad(AdError("SDK not initialized"))
            return
        }
        
        scope.launch {
            try {
                val bidRequest = createBannerBidRequest(context, width, height)
                val result = withContext(Dispatchers.IO) { client.sendBidRequest(bidRequest) }
                
                result.fold(
                    onSuccess = { bidResponse ->
                        val ad = parseAdFromResponse(bidResponse, AdType.BANNER)
                        if (ad != null) listener.onAdLoaded(ad)
                        else listener.onAdFailedToLoad(AdError("Failed to parse ad"))
                    },
                    onFailure = { exception ->
                        if (exception is NoBidException) listener.onAdFailedToLoad(AdError("No fill"))
                        else listener.onAdFailedToLoad(AdError(exception.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading banner ad", e)
                listener.onAdFailedToLoad(AdError(e.message ?: "Unknown error"))
            }
        }
    }
    
    fun loadInterstitialAd(context: Context, listener: AdLoadListener) {
        if (!WaardeXAdSDK.isInitialized()) {
            listener.onAdFailedToLoad(AdError("SDK not initialized"))
            return
        }
        
        scope.launch {
            try {
                val bidRequest = createInterstitialBidRequest(context)
                val result = withContext(Dispatchers.IO) { client.sendBidRequest(bidRequest) }
                
                result.fold(
                    onSuccess = { bidResponse ->
                        val ad = parseAdFromResponse(bidResponse, AdType.INTERSTITIAL)
                        if (ad != null) listener.onAdLoaded(ad)
                        else listener.onAdFailedToLoad(AdError("Failed to parse ad"))
                    },
                    onFailure = { exception ->
                        if (exception is NoBidException) listener.onAdFailedToLoad(AdError("No fill"))
                        else listener.onAdFailedToLoad(AdError(exception.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading interstitial ad", e)
                listener.onAdFailedToLoad(AdError(e.message ?: "Unknown error"))
            }
        }
    }
    
    private suspend fun createBannerBidRequest(context: Context, width: Int, height: Int): BidRequest {
        val device = DeviceInfoCollector.collectDeviceInfo(context)
        val app = DeviceInfoCollector.collectAppInfo(context)
        val user = User(id = DeviceInfoCollector.generateUserId(context))
        
        return BidRequest(
            id = UUID.randomUUID().toString(),
            impressions = listOf(
                Impression(
                    id = UUID.randomUUID().toString(),
                    banner = Banner(width = width, height = height, format = listOf(Format(width, height))),
                    instl = 0,
                    bidFloor = 0.01,
                    secure = 1
                )
            ),
            app = app,
            device = device,
            user = user,
            test = if (WaardeXAdSDK.isDebugMode) 1 else 0
        )
    }
    
    private suspend fun createInterstitialBidRequest(context: Context): BidRequest {
        val device = DeviceInfoCollector.collectDeviceInfo(context)
        val app = DeviceInfoCollector.collectAppInfo(context)
        val user = User(id = DeviceInfoCollector.generateUserId(context))
        
        return BidRequest(
            id = UUID.randomUUID().toString(),
            impressions = listOf(
                Impression(
                    id = UUID.randomUUID().toString(),
                    banner = Banner(width = device.width, height = device.height, position = 7),
                    instl = 1,
                    bidFloor = 0.05,
                    secure = 1
                )
            ),
            app = app,
            device = device,
            user = user,
            test = if (WaardeXAdSDK.isDebugMode) 1 else 0
        )
    }
    
    private fun parseAdFromResponse(bidResponse: BidResponse, adType: AdType): LoadedAd? {
        val seatBid = bidResponse.seatBids?.firstOrNull() ?: return null
        val bid = seatBid.bids.firstOrNull() ?: return null
        
        return LoadedAd(
            adId = bid.id,
            impressionId = bid.impressionId,
            price = bid.price,
            adMarkup = bid.adMarkup ?: "",
            noticeUrl = bid.noticeUrl,
            width = bid.width ?: 0,
            height = bid.height ?: 0,
            adType = adType,
            creativeId = bid.creativeId
        )
    }
    
    fun fireImpression(ad: LoadedAd) {
        if (ad.noticeUrl != null) {
            scope.launch(Dispatchers.IO) {
                client.fireTrackingPixel(ad.noticeUrl)
            }
        }
    }
    
    fun destroy() {
        scope.cancel()
    }
}

interface AdLoadListener {
    fun onAdLoaded(ad: LoadedAd)
    fun onAdFailedToLoad(error: AdError)
}

data class LoadedAd(
    val adId: String,
    val impressionId: String,
    val price: Double,
    val adMarkup: String,
    val noticeUrl: String?,
    val width: Int,
    val height: Int,
    val adType: AdType,
    val creativeId: String?
)

enum class AdType {
    BANNER,
    INTERSTITIAL,
    REWARDED,
    NATIVE
}

data class AdError(
    val message: String,
    val code: Int = 0
)
