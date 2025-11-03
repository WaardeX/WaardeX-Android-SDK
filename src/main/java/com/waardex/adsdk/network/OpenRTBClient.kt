package com.waardex.adsdk.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.waardex.adsdk.WaardeXAdSDK
import com.waardex.adsdk.models.BidRequest
import com.waardex.adsdk.models.BidResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class OpenRTBClient {
    private val TAG = "OpenRTBClient"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    private val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
        
        if (WaardeXAdSDK.isDebugMode) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        builder.build()
    }
    
    suspend fun sendBidRequest(bidRequest: BidRequest): Result<BidResponse> {
        return try {
            val url = "${WaardeXAdSDK.baseUrl.trimEnd('/')}/?name=${WaardeXAdSDK.appName}&pass=${WaardeXAdSDK.appPassword}"
            val json = gson.toJson(bidRequest)
            
            if (WaardeXAdSDK.isDebugMode) {
                Log.d(TAG, "Sending bid request to: $url")
            }
            
            val requestBody = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-OpenRTB-Version", "2.5")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}"))
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                return Result.failure(NoBidException("Empty response"))
            }
            
            val bidResponse = gson.fromJson(responseBody, BidResponse::class.java)
            
            if (bidResponse.seatBids.isNullOrEmpty() || bidResponse.seatBids[0].bids.isEmpty()) {
                return Result.failure(NoBidException("No bids"))
            }
            
            Result.success(bidResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun fireTrackingPixel(url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

class NoBidException(message: String) : Exception(message)
