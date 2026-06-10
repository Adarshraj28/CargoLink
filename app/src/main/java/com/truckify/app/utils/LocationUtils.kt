package com.truckify.app.utils

import android.content.Context
import android.location.Geocoder
import java.util.Locale
import kotlin.math.*

private val addressCache = mutableMapOf<String, String>()

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius of the earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun getFriendlyAddress(context: Context, lat: Double, lng: Double): String {
    if (lat == 0.0 && lng == 0.0) return "Unknown Location"
    
    val cacheKey = String.format(Locale.getDefault(), "%.4f,%.4f", lat, lng)
    addressCache[cacheKey]?.let { return it }

    val geocoder = Geocoder(context, Locale.getDefault())
    val result = try {
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            
            // Extract components for maximum precision
            val subThoroughfare = address.subThoroughfare
            val thoroughfare = address.thoroughfare
            val subLocality = address.subLocality
            val locality = address.locality
            
            val components = mutableListOf<String>()
            
            val streetInfo = when {
                !subThoroughfare.isNullOrEmpty() && !thoroughfare.isNullOrEmpty() && thoroughfare != "Unnamed Road" -> 
                    "$subThoroughfare, $thoroughfare"
                !thoroughfare.isNullOrEmpty() && thoroughfare != "Unnamed Road" -> 
                    thoroughfare
                !subThoroughfare.isNullOrEmpty() -> 
                    subThoroughfare
                else -> null
            }
            
            streetInfo?.let { components.add(it) }
            if (!subLocality.isNullOrEmpty()) components.add(subLocality)
            if (!locality.isNullOrEmpty()) components.add(locality)

            if (components.isNotEmpty()) {
                components.joinToString(", ")
            } else {
                address.getAddressLine(0)?.split(",")?.take(3)?.joinToString(", ") ?: "Unknown Location"
            }
        } else {
            cacheKey
        }
    } catch (e: Exception) {
        cacheKey
    }
    
    addressCache[cacheKey] = result
    return result
}
