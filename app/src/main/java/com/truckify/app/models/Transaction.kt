package com.truckify.app.models

data class Transaction(
    val id: String = "",
    val userEmail: String = "",
    val shipmentId: String = "",
    val amount: Double = 0.0,
    val type: String = "Credit", // Payment, Payout, Refund
    val status: String = "Success", // Pending, Success, Escrow
    val gateway: String = "Razorpay", // Razorpay, Stripe
    val timestamp: Long = System.currentTimeMillis()
)
