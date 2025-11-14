package com.waardex.adsdk.utils

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

internal object VastParser {

    private const val TAG = "VastParser"

    data class VastData(
        val mediaFileUrl: String,
        val duration: Int = 0,
        val clickThroughUrl: String? = null,
        val impressionUrls: List<String> = emptyList(),
        val trackingEvents: Map<String, List<String>> = emptyMap()
    )

    fun parse(vastXml: String): VastData? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(vastXml))

            var mediaFileUrl: String? = null
            var duration = 0
            var clickThroughUrl: String? = null
            val impressionUrls = mutableListOf<String>()
            val trackingEvents = mutableMapOf<String, MutableList<String>>()

            var eventType = parser.eventType
            var currentTag: String? = null
            var currentTrackingEvent: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            "MediaFile" -> {
                                // Витягуємо URL з тексту MediaFile тегу
                            }
                            "Tracking" -> {
                                currentTrackingEvent = parser.getAttributeValue(null, "event")
                            }
                            "ClickThrough" -> {
                                // Витягуємо URL
                            }
                            "Impression" -> {
                                // Витягуємо impression URL
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "MediaFile" -> {
                                    if (mediaFileUrl == null && text.startsWith("http")) {
                                        mediaFileUrl = text
                                    }
                                }
                                "Duration" -> {
                                    duration = parseDuration(text)
                                }
                                "ClickThrough" -> {
                                    if (text.startsWith("http")) {
                                        clickThroughUrl = text
                                    }
                                }
                                "Impression" -> {
                                    if (text.startsWith("http")) {
                                        impressionUrls.add(text)
                                    }
                                }
                                "Tracking" -> {
                                    if (text.startsWith("http") && currentTrackingEvent != null) {
                                        trackingEvents.getOrPut(currentTrackingEvent) { mutableListOf() }
                                            .add(text)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "Tracking") {
                            currentTrackingEvent = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }

            if (mediaFileUrl != null) {
                VastData(
                    mediaFileUrl = mediaFileUrl,
                    duration = duration,
                    clickThroughUrl = clickThroughUrl,
                    impressionUrls = impressionUrls,
                    trackingEvents = trackingEvents
                )
            } else {
                Log.w(TAG, "No MediaFile found in VAST XML")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VAST XML", e)
            null
        }
    }

    private fun parseDuration(duration: String): Int {
        return try {
            // VAST format: HH:MM:SS or HH:MM:SS.mmm
            val parts = duration.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                val seconds = parts[2].split(".")[0].toInt()
                hours * 3600 + minutes * 60 + seconds
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun isVastXml(content: String): Boolean {
        return content.trim().startsWith("<?xml") ||
               content.trim().startsWith("<VAST") ||
               content.contains("<VAST")
    }
}
