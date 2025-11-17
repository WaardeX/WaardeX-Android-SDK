package com.waardex.adsdk.utils

import android.content.Context
import android.util.Log
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.exception.AddressNotFoundException
import com.maxmind.geoip2.model.CityResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Manages GeoLite2-City database download and IP geolocation lookups
 */
internal object GeoIPManager {
    private const val TAG = "GeoIPManager"
    private const val DATABASE_FILENAME = "GeoLite2-City.mmdb"
    private const val DATABASE_URL = "https://github.com/P3TERX/GeoLite.mmdb/raw/download/GeoLite2-City.mmdb"
    private const val UPDATE_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

    private var databaseReader: DatabaseReader? = null
    private var isDownloading = false

    /**
     * Initialize GeoIP database - downloads if needed
     */
    suspend fun initialize(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dbFile = getDatabaseFile(context)

                // Check if database exists and is not too old
                if (dbFile.exists() && !isDatabaseOutdated(dbFile)) {
                    Log.d(TAG, "GeoIP database is up to date")
                    loadDatabase(dbFile)
                    return@withContext true
                }

                // Download database
                if (isDownloading) {
                    Log.w(TAG, "Database download already in progress")
                    return@withContext false
                }

                downloadDatabase(context, dbFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GeoIP database", e)
                false
            }
        }
    }

    /**
     * Lookup city and country by IP address
     */
    fun lookupCity(ipAddress: String?): CityData? {
        if (ipAddress == null) return null

        return try {
            val reader = databaseReader ?: return null
            val address = InetAddress.getByName(ipAddress)
            val response = reader.city(address)

            CityData(
                country = response.country?.isoCode,
                city = response.city?.name,
                latitude = response.location?.latitude,
                longitude = response.location?.longitude
            )
        } catch (e: AddressNotFoundException) {
            Log.d(TAG, "Address not found in database: $ipAddress")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lookup IP: $ipAddress", e)
            null
        }
    }

    private suspend fun downloadDatabase(context: Context, dbFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            isDownloading = true
            try {
                Log.d(TAG, "Downloading GeoIP database from $DATABASE_URL")

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(DATABASE_URL)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download database: HTTP ${response.code}")
                    return@withContext false
                }

                val body = response.body ?: run {
                    Log.e(TAG, "Empty response body")
                    return@withContext false
                }

                // Write to file
                FileOutputStream(dbFile).use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "GeoIP database downloaded successfully (${dbFile.length() / 1024 / 1024} MB)")

                // Load the database
                loadDatabase(dbFile)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download GeoIP database", e)

                // Delete partial file
                if (dbFile.exists()) {
                    dbFile.delete()
                }
                false
            } finally {
                isDownloading = false
            }
        }
    }

    private fun loadDatabase(dbFile: File) {
        try {
            databaseReader?.close()
            databaseReader = DatabaseReader.Builder(dbFile).build()
            Log.d(TAG, "GeoIP database loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load GeoIP database", e)
            databaseReader = null
        }
    }

    private fun getDatabaseFile(context: Context): File {
        return File(context.filesDir, DATABASE_FILENAME)
    }

    private fun isDatabaseOutdated(dbFile: File): Boolean {
        val age = System.currentTimeMillis() - dbFile.lastModified()
        return age > UPDATE_INTERVAL_MS
    }

    /**
     * Check if database is ready
     */
    fun isDatabaseReady(): Boolean {
        return databaseReader != null
    }

    /**
     * Close database reader
     */
    fun close() {
        databaseReader?.close()
        databaseReader = null
    }
}

data class CityData(
    val country: String?,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?
)
