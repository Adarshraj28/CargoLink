package com.truckify.app.models

data class Review(
    val id: String = "",
    val shipmentId: String = "",
    val reviewerEmail: String = "",
    val targetEmail: String = "",
    val rating: Double = 0.0,
    val feedback: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    
    // Vendor rating for Driver
    val drivingRating: Float = 0f,
    val communicationRating: Float = 0f,
    val onTimeRating: Float = 0f,
    
    // Driver rating for Vendor
    val behaviorRating: Float = 0f,
    val loadingExpRating: Float = 0f,
    val paymentExpRating: Float = 0f
)
