package com.waardex.adsdk.models

import com.google.gson.annotations.SerializedName

data class BidResponse(
    @SerializedName("id") val id: String,
    @SerializedName("seatbid") val seatBids: List<SeatBid>? = null,
    @SerializedName("bidid") val bidId: String? = null,
    @SerializedName("cur") val currency: String = "USD",
    @SerializedName("nbr") val noBidReason: Int? = null
)

data class SeatBid(
    @SerializedName("bid") val bids: List<Bid>,
    @SerializedName("seat") val seat: String? = null
)

data class Bid(
    @SerializedName("id") val id: String,
    @SerializedName("impid") val impressionId: String,
    @SerializedName("price") val price: Double,
    @SerializedName("adid") val adId: String? = null,
    @SerializedName("nurl") val noticeUrl: String? = null,
    @SerializedName("adm") val adMarkup: String? = null,
    @SerializedName("adomain") val advertiserDomains: List<String>? = null,
    @SerializedName("bundle") val bundle: String? = null,
    @SerializedName("iurl") val imageUrl: String? = null,
    @SerializedName("cid") val campaignId: String? = null,
    @SerializedName("crid") val creativeId: String? = null,
    @SerializedName("w") val width: Int? = null,
    @SerializedName("h") val height: Int? = null,
    @SerializedName("ext") val extensions: Map<String, Any>? = null
)
