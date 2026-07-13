package com.cargolink.app.models

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
    val status: String = "Available", // Available, In Transit, Delivered, Searching for Driver
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
    val otpTimestamp: Long = 0,
    val paymentMode: String = "Escrow", // Escrow, COD
    val paymentStatus: String = "Pending", // Pending, Escrowed, Success, Refunded
    val escrowTxnId: String = "",
    
    // Step 1: Pickup Details
    val pickupContactName: String = "",
    val pickupContactNumber: String = "",
    val scheduledPickupDate: String = "",
    val scheduledPickupTime: String = "",

    // Step 2: Drop Details
    val receiverName: String = "",
    val receiverPhone: String = "",
    val scheduledDropDate: String = "",
    val scheduledDropTime: String = "",

    // Step 3: Load Information
    val dimensions: String = "",
    val numPackages: Int = 1,
    val loadValue: Double = 0.0,
    val isFragile: Boolean = false,
    val isHazardous: Boolean = false,
    val images: List<String> = emptyList(),

    // Step 4: Truck Requirements
    val numTrucks: Int = 1,
    val isFullTruck: Boolean = true,
    val isTemperatureControlled: Boolean = false,

    // Step 5: Pricing
    val isPriceNegotiable: Boolean = false,
    
    // Physical Proofs
    val pickupTime: Long = 0,
    val deliveryTime: Long = 0,
    val pickupActualLat: Double = 0.0,
    val pickupActualLng: Double = 0.0,
    val deliveryActualLat: Double = 0.0,
    val deliveryActualLng: Double = 0.0,
    val commission: Double = 0.0,
    val driverPayout: Double = 0.0,
    
    // Rating Flags
    val vendorRatedDriver: Boolean = false,
    val driverRatedVendor: Boolean = false,

    // Insurance
    val isInsured: Boolean = false,
    val insurancePremium: Double = 0.0,
    val declaredValue: Double = 0.0
)
