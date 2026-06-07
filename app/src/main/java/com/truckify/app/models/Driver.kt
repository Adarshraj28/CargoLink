package com.truckify.app.models

data class Driver(
    val email: String = "",
    val name: String = "",
    val truckType: String = "Open Body",
    val capacity: String = "5 Tons",
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val isAvailable: Boolean = true,
    val rating: Double = 4.5,
    val totalTrips: Int = 0,
    val fuelEfficiency: Double = 15.0, // km per liter
    val experienceYears: Int = 2,
    val trustScore: Double = 90.0, // 0 to 100
    val onTimeDeliveries: Int = 0,
    val successfulScans: Int = 0,
    val accidentCount: Int = 0,
    val cancellationsCount: Int = 0,
    val verificationStatus: String = "Not Started"
)
