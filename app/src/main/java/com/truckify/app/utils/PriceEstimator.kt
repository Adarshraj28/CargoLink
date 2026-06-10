package com.truckify.app.utils

import kotlin.math.*

object PriceEstimator {
    private const val BASE_FARE = 500.0
    private const val RATE_PER_KM = 45.0
    private const val RATE_PER_TON = 200.0
    private const val COMMISSION_PERCENT = 0.10

    fun estimatePrice(distanceKm: Double, weightTons: Double, truckType: String): EstimationResult {
        val truckMultiplier = when (truckType) {
            "Mini Truck" -> 1.0
            "Pick-up" -> 1.2
            "Mid-size" -> 1.5
            "Heavy Duty" -> 2.0
            else -> 1.0
        }

        val totalFare = (BASE_FARE + (distanceKm * RATE_PER_KM) + (weightTons * RATE_PER_TON)) * truckMultiplier
        val commission = totalFare * COMMISSION_PERCENT
        val driverPayout = totalFare - commission

        return EstimationResult(
            totalPrice = totalFare,
            commission = commission,
            driverPayout = driverPayout,
            distance = distanceKm
        )
    }

    data class EstimationResult(
        val totalPrice: Double,
        val commission: Double,
        val driverPayout: Double,
        val distance: Double
    )
}
