package com.truckify.app.models

data class Shipment(
    val id: String = "",
    val vendorEmail: String = "",
    val driverEmail: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val price: String = "₹0",
    val status: String = "Available", // Available, In Transit, Delivered
    val weight: String = "",
    val loadType: String = "General",
    val truckType: String = "Mini Truck",
    val bodyType: String = "Open Body",
    val additionalInfo: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val aiRecommendationScore: Double = 0.0,
    val podToken: String = "",
    val deliveryOtp: String = "",
    val paymentMode: String = "Escrow", // Escrow, COD
    val paymentStatus: String = "Pending", // Pending, Escrowed, Success, Refunded
    val escrowTxnId: String = "",
    
    // Physical Proofs
    val pickupTime: Long = 0,
    val deliveryTime: Long = 0,
    val pickupActualLat: Double = 0.0,
    val pickupActualLng: Double = 0.0,
    val deliveryActualLat: Double = 0.0,
    val deliveryActualLng: Double = 0.0
)
