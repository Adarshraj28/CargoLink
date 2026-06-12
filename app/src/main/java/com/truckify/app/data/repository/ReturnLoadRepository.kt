package com.truckify.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.truckify.app.models.Driver
import com.truckify.app.models.Shipment
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class ReturnLoadRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getDriverProfile(email: String): Driver? {
        return try {
            firestore.collection("users").document(email).get().await().toObject(Driver::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateDriverStatus(email: String, status: String) {
        firestore.collection("users").document(email).update("status", status).await()
    }

    suspend fun getPotentialReturnLoads(driver: Driver): List<Shipment> {
        return try {
            val result = firestore.collection("shipments")
                .whereEqualTo("status", "Available")
                .whereEqualTo("truckType", driver.truckType)
                .get()
                .await()

            val allLoads = result.toObjects(Shipment::class.java)
            
            // Apply filtering and scoring
            allLoads.filter { shipment ->
                val distFromCurrent = calculateDistance(driver.currentLat, driver.currentLng, shipment.pickupLat, shipment.pickupLng)
                distFromCurrent <= 30.0 // Within 30km
            }.map { shipment ->
                val score = calculateScore(driver, shipment)
                shipment.copy(aiRecommendationScore = score.toDouble())
            }.sortedByDescending { it.aiRecommendationScore }
            
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateScore(driver: Driver, shipment: Shipment): Int {
        var score = 0
        
        // Destination matches home city (approx check via distance for production robustness)
        val distToHome = calculateDistance(shipment.destLat, shipment.destLng, driver.homeLat, driver.homeLng)
        if (distToHome < 20.0) score += 50
        
        // Pickup within 10km
        val distFromCurrent = calculateDistance(driver.currentLat, driver.currentLng, shipment.pickupLat, shipment.pickupLng)
        if (distFromCurrent < 10.0) score += 30
        
        // Higher budget (Simplified: price as string "₹5000" -> 5000)
        val priceValue = shipment.price.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (priceValue > 5000) score += 20
        
        // Preferred vendor (Logic placeholder - e.g. based on vendor rating)
        // score += 10
        
        return score
    }

    suspend fun acceptReturnLoad(driverEmail: String, shipmentId: String, pickupAddress: String, destAddress: String): Result<Unit> {
        return try {
            val driverDoc = firestore.collection("users").document(driverEmail).get().await()
            val driver = driverDoc.toObject(Driver::class.java) ?: return Result.failure(Exception("Driver not found"))
            
            val shipmentRef = firestore.collection("shipments").document(shipmentId)
            
            firestore.runTransaction { transaction ->
                val shipment = transaction.get(shipmentRef).toObject(Shipment::class.java)
                if (shipment?.status != "Available") throw Exception("Load no longer available")
                
                // 1. Assign Load
                transaction.update(shipmentRef, mapOf(
                    "status" to "In Transit",
                    "driverEmail" to driverEmail
                ))
                
                // 2. Update Driver Analytics
                val distance = calculateDistance(shipment.pickupLat, shipment.pickupLng, shipment.destLat, shipment.destLng)
                val co2Saved = distance * 0.12 // Approx 120g per km for a truck
                
                transaction.update(firestore.collection("users").document(driverEmail), mapOf(
                    "status" to "On Trip",
                    "emptyKmSaved" to (driver.emptyKmSaved + distance),
                    "additionalEarnings" to (driver.additionalEarnings + (shipment.price.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0)),
                    "returnMatchesCount" to (driver.returnMatchesCount + 1),
                    "co2Reduced" to (driver.co2Reduced + co2Saved)
                ))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
