package com.cargolink.app.models

data class Expense(
    val id: String = "",
    val driverEmail: String = "",
    val amount: Double = 0.0,
    val type: String = "", // Fuel, Toll, Maintenance, Food
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val receiptUrl: String = ""
)
