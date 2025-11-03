package com.waardex.adsdk.models

import com.google.gson.annotations.SerializedName

data class BidRequest(
    @SerializedName("id") val id: String,
    @SerializedName("imp") val impressions: List<Impression>,
    @SerializedName("app") val app: App? = null,
    @SerializedName("device") val device: Device? = null,
    @SerializedName("user") val user: User? = null,
    @SerializedName("test") val test: Int = 0,
    @SerializedName("tmax") val tmax: Int = 3000
)

data class Impression(
    @SerializedName("id") val id: String,
    @SerializedName("banner") val banner: Banner? = null,
    @SerializedName("video") val video: Video? = null,
    @SerializedName("native") val native: Native? = null,
    @SerializedName("instl") val instl: Int = 0,
    @SerializedName("bidfloor") val bidFloor: Double = 0.0,
    @SerializedName("secure") val secure: Int = 1
)

data class Banner(
    @SerializedName("w") val width: Int,
    @SerializedName("h") val height: Int,
    @SerializedName("format") val format: List<Format>? = null,
    @SerializedName("pos") val position: Int = 0,
    @SerializedName("api") val api: List<Int>? = null
)

data class Format(
    @SerializedName("w") val width: Int,
    @SerializedName("h") val height: Int
)

data class Video(
    @SerializedName("mimes") val mimes: List<String>,
    @SerializedName("minduration") val minDuration: Int,
    @SerializedName("maxduration") val maxDuration: Int,
    @SerializedName("protocols") val protocols: List<Int>,
    @SerializedName("w") val width: Int,
    @SerializedName("h") val height: Int,
    @SerializedName("startdelay") val startDelay: Int = 0,
    @SerializedName("linearity") val linearity: Int = 1,
    @SerializedName("skip") val skip: Int = 0,
    @SerializedName("pos") val position: Int = 7
)

data class Native(
    @SerializedName("request") val request: String,
    @SerializedName("ver") val version: String = "1.2"
)

data class App(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("bundle") val bundle: String,
    @SerializedName("storeurl") val storeUrl: String? = null,
    @SerializedName("ver") val version: String? = null,
    @SerializedName("publisher") val publisher: Publisher? = null
)

data class Publisher(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null
)

data class Device(
    @SerializedName("ua") val userAgent: String,
    @SerializedName("geo") val geo: Geo? = null,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("devicetype") val deviceType: Int = 4,
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
    @SerializedName("os") val os: String = "Android",
    @SerializedName("osv") val osVersion: String,
    @SerializedName("w") val width: Int,
    @SerializedName("h") val height: Int,
    @SerializedName("ppi") val ppi: Int,
    @SerializedName("pxratio") val pixelRatio: Float,
    @SerializedName("language") val language: String,
    @SerializedName("ifa") val advertisingId: String? = null,
    @SerializedName("lmt") val limitAdTracking: Int = 0,
    @SerializedName("connectiontype") val connectionType: Int = 0
)

data class Geo(
    @SerializedName("lat") val latitude: Double? = null,
    @SerializedName("lon") val longitude: Double? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("type") val type: Int = 2
)

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("yob") val yearOfBirth: Int? = null
)
