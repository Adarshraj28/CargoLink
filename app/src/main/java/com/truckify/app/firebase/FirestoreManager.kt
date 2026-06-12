package com.truckify.app.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.truckify.app.models.Shipment
import com.truckify.app.models.Driver
import com.truckify.app.models.Transaction
import com.truckify.app.models.ChatMessage
import com.truckify.app.models.Expense
import com.truckify.app.models.Review

object FirestoreManager {

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun saveUser(name: String, email: String, role: String) {
        val cleanEmail = email.lowercase().trim()
        val user = hashMapOf(
            "name" to name,
            "email" to cleanEmail,
            "role" to role,
            "trustScore" to 90.0,
            "totalTrips" to 0,
            "onTimeDeliveries" to 0,
            "successfulScans" to 0,
            "accidentCount" to 0,
            "cancellationsCount" to 0,
            "isPhoneVerified" to false,
            "verificationStatus" to "Not Started"
        )
        db.collection("users").document(cleanEmail).set(user)
            .addOnFailureListener { android.util.Log.e("FirestoreManager", "saveUser Error: ${it.message}") }
    }

    fun postShipment(shipment: Shipment, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val docRef = db.collection("shipments").document()
        val otpDocRef = db.collection("shipmentOtps").document(docRef.id)
        
        val otp = (100000..999999).random().toString()
        val finalShipment = shipment.copy(
            id = docRef.id,
            podToken = java.util.UUID.randomUUID().toString(),
            otpTimestamp = System.currentTimeMillis()
        )
        
        db.runBatch { batch ->
            batch.set(docRef, finalShipment)
            batch.set(otpDocRef, hashMapOf(
                "shipmentId" to docRef.id,
                "vendorEmail" to shipment.vendorEmail,
                "otp" to otp
            ))
        }.addOnSuccessListener {
            // Background notification for drivers
            db.collection("users").whereEqualTo("role", "Driver").get().addOnSuccessListener { drivers ->
                db.runBatch { batch ->
                    for (driver in drivers) {
                        val email = driver.getString("email") ?: continue
                        val notification = hashMapOf(
                            "recipientEmail" to email,
                            "title" to "New Load Available",
                            "message" to "A new shipment is available from ${shipment.pickupAddress.split(",").first()}",
                            "timestamp" to System.currentTimeMillis()
                        )
                        batch.set(db.collection("notifications").document(), notification)
                    }
                }
            }
            onSuccess(docRef.id)
            
            // Notify Vendor with OTP
            sendInternalNotification(
                shipment.vendorEmail,
                "Shipment Posted",
                "Your shipment #${docRef.id.takeLast(4).uppercase()} has been posted. Delivery OTP: $otp"
            )
        }.addOnFailureListener { onError(it.message ?: "Failed to post") }
    }

    fun getAvailableLoads(onResult: (List<Shipment>) -> Unit): ListenerRegistration {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        return db.collection("shipments")
            .whereEqualTo("status", "Available")
            .whereGreaterThan("timestamp", twentyFourHoursAgo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getAvailableLoads Error: ${error.message}")
                    return@addSnapshotListener
                }
                if (value != null) {
                    val shipments = value.documents.mapNotNull { doc ->
                        doc.toObject(Shipment::class.java)?.copy(id = doc.id)
                    }
                    onResult(shipments)
                }
            }
    }

    fun getActiveShipments(email: String, onResult: (List<Shipment>) -> Unit): ListenerRegistration {
        return db.collection("shipments")
            .whereIn("status", listOf("Accepted", "Arrived", "In Transit"))
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getActiveShipments Error: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                if (value != null) {
                    val shipments = value.documents.mapNotNull { doc ->
                        doc.toObject(Shipment::class.java)?.copy(id = doc.id)
                    }.filter { 
                        it.vendorEmail == email || it.driverEmail == email
                    }.sortedByDescending { it.timestamp }
                    onResult(shipments)
                }
            }
    }

    fun listenToActiveShipments(email: String, role: String, onResult: (List<Shipment>) -> Unit): ListenerRegistration {
        val field = if (role == "Vendor") "vendorEmail" else "driverEmail"
        return db.collection("shipments")
            .whereEqualTo(field, email)
            .whereIn("status", listOf("Accepted", "Arrived", "In Transit"))
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "listenToActiveShipments Error: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                if (value != null) {
                    val shipments = value.documents.mapNotNull { doc ->
                        doc.toObject(Shipment::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    onResult(shipments)
                }
            }
    }

    fun getShipmentHistory(email: String, role: String, onResult: (List<Shipment>) -> Unit): ListenerRegistration {
        val field = if (role == "Vendor") "vendorEmail" else "driverEmail"
        return db.collection("shipments")
            .whereEqualTo(field, email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getShipmentHistory Error: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                if (value != null) {
                    val shipments = value.documents.mapNotNull { doc ->
                        doc.toObject(Shipment::class.java)?.copy(id = doc.id)
                    }
                    onResult(shipments)
                }
            }
    }

    fun listenForNewLoads(startTime: Long = System.currentTimeMillis(), onNewLoad: (Shipment) -> Unit): ListenerRegistration {
        return db.collection("shipments")
            .whereEqualTo("status", "Available")
            .whereGreaterThan("timestamp", startTime)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "listenForNewLoads Error: ${error.message}")
                    return@addSnapshotListener
                }
                val docs = value?.documentChanges ?: return@addSnapshotListener
                for (change in docs) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val shipment = change.document.toObject(Shipment::class.java).copy(id = change.document.id)
                        onNewLoad(shipment)
                    }
                }
            }
    }

    fun acceptShipment(shipmentId: String, driverEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (shipmentId.isBlank()) {
            onError("Invalid Load ID")
            return
        }

        db.collection("users").document(driverEmail).get().addOnSuccessListener { userDoc ->
            if (!userDoc.exists()) {
                onError("Driver profile not found. Please complete your profile.")
                return@addOnSuccessListener
            }
            val driver = userDoc.toObject(Driver::class.java) ?: return@addOnSuccessListener onError("Failed to parse driver profile")
            
            if (driver.verificationStatus != "Verified") {
                onError("Please verify your account to accept loads.")
                return@addOnSuccessListener
            }

            val driverCapacity = driver.capacity.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

            // Check for existing active shipments first
            db.collection("shipments").whereEqualTo("driverEmail", driverEmail)
                .whereIn("status", listOf("In Transit", "Accepted")).get().addOnSuccessListener { activeTrips ->
                    if (!activeTrips.isEmpty) {
                        onError("You already have an active shipment. Complete it first.")
                        return@addOnSuccessListener
                    }

                    // Then check the target shipment
                    db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
                        val shipment = doc.toObject(Shipment::class.java) ?: return@addOnSuccessListener onError("Shipment not found")
                        
                        if (shipment.status != "Available") {
                            onError("Shipment no longer available")
                            return@addOnSuccessListener
                        }

                        val shipmentWeight = shipment.weight.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                        if (shipmentWeight > driverCapacity) {
                            onError("This load exceeds your truck's capacity (${driver.capacity}).")
                            return@addOnSuccessListener
                        }

                        db.runTransaction { transaction ->
                            val shipmentRef = db.collection("shipments").document(shipmentId)
                            // READ: Shipment details
                            val currentShipment = transaction.get(shipmentRef).toObject(Shipment::class.java)
                                ?: throw Exception("Shipment not found")
                            
                            if (currentShipment.status != "Available") {
                                throw Exception("Shipment already taken")
                            }

                            // --- START WRITES ---
                            transaction.update(shipmentRef, mapOf(
                                "status" to "Accepted",
                                "driverEmail" to driverEmail
                            ))

                            // If it's an Escrow payment, we just mark it as such without strictly deducting from a wallet balance
                            if (currentShipment.paymentMode != "COD") {
                                transaction.update(shipmentRef, "paymentStatus", "Escrowed")
                            }
                        }.addOnSuccessListener {
                            sendInternalNotification(shipment.vendorEmail, "Load Accepted", "Driver ${driver.name.ifBlank { driverEmail }} has accepted your shipment #$shipmentId.")
                            sendInternalNotification(shipment.vendorEmail, "Load Accepted", "Driver ${driver.name.ifBlank { driverEmail }} has accepted your shipment #$shipmentId.")
                            
                            sendInternalNotification(
                                driverEmail,
                                "Load Accepted",
                                "You've accepted the load to ${shipment.destinationAddress.split(",").first()}."
                            )

                            onSuccess()
                        }.addOnFailureListener { 
                            android.util.Log.e("FirestoreManager", "Transaction Error: ${it.message}")
                            onError(it.message ?: "Failed to accept") 
                        }
                    }.addOnFailureListener { onError(it.message ?: "Could not fetch shipment details") }
                }.addOnFailureListener { 
                    android.util.Log.e("FirestoreManager", "Active Trip Check Error: ${it.message}")
                    onError(it.message ?: "Could not verify active trips") 
                }
        }.addOnFailureListener { onError(it.message ?: "Connection error") }
    }


    fun cancelShipmentByDriver(shipmentId: String, driverEmail: String, onSuccess: () -> Unit) {
        db.collection("shipments").document(shipmentId).update(mapOf(
            "status" to "Available",
            "driverEmail" to ""
        )).addOnSuccessListener {
            updateDriverStats(driverEmail, cancellation = true)
            onSuccess()
        }
    }

    fun cancelShipment(shipmentId: String, onSuccess: () -> Unit) {
        if (shipmentId.isBlank()) return
        db.collection("shipments").document(shipmentId).delete().addOnSuccessListener { onSuccess() }
    }

    fun completeShipment(shipmentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Now handled by verifyPodWithToken
    }

    fun completeShipmentDirectly(shipmentId: String, onSuccess: () -> Unit) {
        if (shipmentId.isBlank()) return
        db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
            val shipment = doc.toObject(Shipment::class.java) ?: return@addOnSuccessListener
            
            db.collection("shipments").document(shipmentId)
                .update(mapOf(
                    "status" to "Delivered",
                    "deliveryTime" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    sendInternalNotification(shipment.vendorEmail, "Shipment Delivered", "Your shipment #${shipment.id.takeLast(4)} has been delivered.")
                    if (shipment.paymentMode != "COD") {
                        releaseEscrow(shipmentId)
                    }
                    updateDriverStats(shipment.driverEmail, isOnTime = true, isSuccessfulScan = true)
                    onSuccess()
                }
        }
    }

    fun markArrivedAtPickup(shipmentId: String, vendorEmail: String, onSuccess: () -> Unit) {
        db.collection("shipments").document(shipmentId).update("status", "Arrived")
            .addOnSuccessListener {
                sendInternalNotification(vendorEmail, "Driver Arrived", "Your driver has arrived at the pickup location for shipment #$shipmentId")
                onSuccess()
            }
    }

    fun verifyPickupQR(shipmentId: String, token: String, lat: Double, lng: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (shipmentId.isBlank()) {
            onError("Invalid shipment ID")
            return
        }
        db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
            val shipment = doc.toObject(Shipment::class.java) ?: return@addOnSuccessListener
            if (shipment.podToken == token) {
                val otp = (100000..999999).random().toString()
                
                db.runBatch { batch ->
                    batch.update(db.collection("shipments").document(shipmentId), mapOf(
                        "status" to "In Transit",
                        "pickupTime" to System.currentTimeMillis(),
                        "pickupActualLat" to lat,
                        "pickupActualLng" to lng,
                        "otpTimestamp" to System.currentTimeMillis()
                    ))
                    batch.set(db.collection("shipmentOtps").document(shipmentId), hashMapOf(
                        "shipmentId" to shipmentId,
                        "vendorEmail" to shipment.vendorEmail,
                        "otp" to otp
                    ))
                }.addOnSuccessListener {
                    sendInternalNotification(shipment.vendorEmail, "Load Picked Up", "Shipment #${shipment.id.takeLast(4)} is now In Transit. Delivery OTP: $otp")
                    onSuccess()
                }
            } else {
                onError("Invalid QR Code")
            }
        }
    }

    fun verifyDeliveryOtp(shipmentId: String, enteredOtp: String, lat: Double, lng: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (shipmentId.isBlank()) {
            onError("Invalid shipment ID")
            return
        }
        
        db.collection("shipmentOtps").document(shipmentId).get().addOnSuccessListener { otpDoc ->
            val storedOtp = otpDoc.getString("otp") ?: ""
            
            db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
                val shipment = doc.toObject(Shipment::class.java) ?: return@addOnSuccessListener
                
                val isExpired = System.currentTimeMillis() - shipment.otpTimestamp > 30 * 60 * 1000
                if (isExpired) {
                    onError("OTP has expired. Please ask the sender to refresh the code.")
                    return@addOnSuccessListener
                }

                if (storedOtp == enteredOtp && shipment.status == "In Transit") {
                    db.runBatch { batch ->
                        batch.update(db.collection("shipments").document(shipmentId), mapOf(
                            "status" to "Delivered",
                            "deliveryTime" to System.currentTimeMillis(),
                            "deliveryActualLat" to lat,
                            "deliveryActualLng" to lng
                        ))
                        batch.delete(db.collection("shipmentOtps").document(shipmentId))
                    }.addOnSuccessListener {
                        sendInternalNotification(shipment.vendorEmail, "Shipment Delivered", "Your shipment #${shipment.id.takeLast(4)} has been delivered successfully via OTP.")
                        sendInternalNotification(shipment.driverEmail, "Earnings Released", "Delivery Verified! Payment for shipment #${shipment.id.takeLast(4)} has been added to your wallet.")
                        
                        if (shipment.paymentMode == "COD") {
                            val cleanPrice = shipment.price.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            handleCodSettlement(shipmentId, shipment.driverEmail, cleanPrice)
                        } else {
                            releaseEscrow(shipmentId)
                        }
                        updateDriverStats(shipment.driverEmail, isOnTime = true, isSuccessfulScan = true)
                        onSuccess()
                    }
                } else if (shipment.status == "Delivered") {
                    onError("Shipment already delivered")
                } else {
                    onError("Invalid OTP")
                }
            }
        }.addOnFailureListener { onError("Failed to verify OTP") }
    }

    private fun updateDriverStats(email: String, isOnTime: Boolean = false, isSuccessfulScan: Boolean = false, accident: Boolean = false, cancellation: Boolean = false) {
        db.collection("users").document(email).get().addOnSuccessListener { doc ->
            val driver = doc.toObject(Driver::class.java) ?: return@addOnSuccessListener
            
            val newOnTime = if (isOnTime) driver.onTimeDeliveries + 1 else driver.onTimeDeliveries
            val newScans = if (isSuccessfulScan) driver.successfulScans + 1 else driver.successfulScans
            val newAccidents = if (accident) driver.accidentCount + 1 else driver.accidentCount
            val newCancellations = if (cancellation) driver.cancellationsCount + 1 else driver.cancellationsCount
            val newTotalTrips = if (isOnTime || isSuccessfulScan) driver.totalTrips + 1 else driver.totalTrips

            // Simple Trust Score Calculation
            var score = 70.0 // Base
            score += (newOnTime * 2.0)
            score += (newScans * 1.0)
            score += (driver.rating - 3.0) * 5.0
            score -= (newAccidents * 15.0)
            score -= (newCancellations * 5.0)
            
            val finalScore = score.coerceIn(0.0, 100.0)

            db.collection("users").document(email).update(mapOf(
                "onTimeDeliveries" to newOnTime,
                "successfulScans" to newScans,
                "accidentCount" to newAccidents,
                "cancellationsCount" to newCancellations,
                "totalTrips" to newTotalTrips,
                "trustScore" to finalScore
            ))
        }
    }

    fun wipeAllData(onSuccess: () -> Unit) {
        val collections = listOf("shipments", "transactions", "notifications", "expenses", "shipmentOtps", "platform_earnings")
        
        collections.forEach { coll ->
            db.collection(coll).get().addOnSuccessListener { docs ->
                db.runBatch { batch ->
                    for (doc in docs) {
                        batch.delete(db.collection(coll).document(doc.id))
                    }
                }
            }
        }
        
        db.collection("users").get().addOnSuccessListener { docs ->
            db.runBatch { batch ->
                for (doc in docs) {
                    val updates = mapOf(
                        "totalTrips" to 0,
                        "onTimeDeliveries" to 0,
                        "successfulScans" to 0,
                        "accidentCount" to 0,
                        "cancellationsCount" to 0,
                        "trustScore" to 90.0,
                        "rating" to 4.5,
                        "emptyKmSaved" to 0.0,
                        "additionalEarnings" to 0.0,
                        "returnMatchesCount" to 0,
                        "co2Reduced" to 0.0,
                        "status" to "Available",
                        "isAvailable" to true
                    )
                    batch.update(db.collection("users").document(doc.id), updates)
                }
            }
        }
        
        db.collection("wallets").get().addOnSuccessListener { docs ->
            db.runBatch { batch ->
                for (doc in docs) {
                    batch.update(db.collection("wallets").document(doc.id), "balance", 0.0)
                }
            }.addOnSuccessListener { onSuccess() }
        }
    }

    private fun sendInternalNotification(recipientEmail: String, title: String, message: String) {
        val notification = hashMapOf(
            "recipientEmail" to recipientEmail,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("notifications").add(notification)
    }

    fun resendDeliveryOtp(shipmentId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val newOtp = (100000..999999).random().toString()
        val timestamp = System.currentTimeMillis()
        db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
            val shipment = doc.toObject(Shipment::class.java)
            
            db.runBatch { batch ->
                batch.update(db.collection("shipments").document(shipmentId), "otpTimestamp", timestamp)
                batch.update(db.collection("shipmentOtps").document(shipmentId), "otp", newOtp)
            }.addOnSuccessListener {
                if (shipment != null) {
                    sendInternalNotification(
                        shipment.vendorEmail,
                        "New Delivery OTP",
                        "The OTP for shipment #${shipment.id.takeLast(4)} has been refreshed: $newOtp"
                    )
                }
                onSuccess(newOtp)
            }.addOnFailureListener { onError(it.message ?: "Failed to resend") }
        }
    }

    fun getShipmentOtp(shipmentId: String, onResult: (String?) -> Unit): ListenerRegistration? {
        if (shipmentId.isBlank()) return null
        return db.collection("shipmentOtps").document(shipmentId).addSnapshotListener { value, _ ->
            onResult(value?.getString("otp"))
        }
    }

    fun getUserData(email: String, onResult: (Map<String, Any>?) -> Unit) {
        val cleanEmail = email.lowercase().trim()
        db.collection("users").document(cleanEmail).get()
            .addOnSuccessListener { document -> onResult(document.data) }
            .addOnFailureListener { onResult(null) }
    }

    fun submitDriverReview(review: Review, onSuccess: () -> Unit) {
        val reviewId = db.collection("reviews").document().id
        val finalReview = review.copy(id = reviewId)
        
        db.runBatch { batch ->
            batch.set(db.collection("reviews").document(reviewId), finalReview)
            batch.update(db.collection("shipments").document(review.shipmentId), "vendorRatedDriver", true)
        }.addOnSuccessListener {
            // Update driver average rating
            updateUserAverageRating(review.targetEmail, review.rating)
            onSuccess()
        }
    }

    fun submitVendorReview(review: Review, onSuccess: () -> Unit) {
        val reviewId = db.collection("reviews").document().id
        val finalReview = review.copy(id = reviewId)
        
        db.runBatch { batch ->
            batch.set(db.collection("reviews").document(reviewId), finalReview)
            batch.update(db.collection("shipments").document(review.shipmentId), "driverRatedVendor", true)
        }.addOnSuccessListener {
            // Update vendor average rating
            updateUserAverageRating(review.targetEmail, review.rating)
            onSuccess()
        }
    }

    private fun updateUserAverageRating(email: String, newRating: Double) {
        db.collection("users").document(email).get().addOnSuccessListener { doc ->
            if (!doc.exists()) return@addOnSuccessListener
            
            val currentRating = doc.getDouble("rating") ?: 4.5
            val totalTrips = doc.getLong("totalTrips")?.toInt() ?: 0
            
            // weighted average
            val updatedRating = ((currentRating * totalTrips) + newRating) / (totalTrips + 1)
            
            db.collection("users").document(email).update(mapOf(
                "rating" to updatedRating
            )).addOnSuccessListener {
                if (doc.getString("role") == "Driver") {
                    updateDriverStats(email)
                }
            }
        }
    }

    fun getDrivers(onResult: (List<Map<String, Any>>) -> Unit) {
        db.collection("users")
            .whereEqualTo("role", "Driver")
            .get()
            .addOnSuccessListener { value -> onResult(value.documents.mapNotNull { it.data }) }
    }

    fun getAllActiveDrivers(onResult: (List<Driver>) -> Unit): ListenerRegistration {
        return db.collection("users")
            .whereEqualTo("role", "Driver")
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    onResult(value.toObjects(Driver::class.java))
                }
            }
    }

    fun getNotifications(email: String, onResult: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        return db.collection("notifications")
            .whereEqualTo("recipientEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getNotifications Error: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                if (value != null) {
                    onResult(value.documents.mapNotNull { it.data })
                }
            }
    }

    fun getWalletBalance(email: String, onResult: (Double) -> Unit): ListenerRegistration {
        return db.collection("wallets").document(email)
            .addSnapshotListener { doc, _ -> 
                onResult(doc?.getDouble("balance") ?: 0.0) 
            }
    }

    fun topupWallet(email: String, amount: Double, gateway: String, onSuccess: () -> Unit) {
        val txnId = db.collection("transactions").document().id
        val transaction = Transaction(
            id = txnId,
            userEmail = email,
            amount = amount,
            type = "Topup",
            status = "Success",
            gateway = gateway,
            timestamp = System.currentTimeMillis()
        )

        db.collection("wallets").document(email).get().addOnSuccessListener { doc ->
            val currentBalance = if (doc.exists()) doc.getDouble("balance") ?: 0.0 else 0.0
            val newBalance = currentBalance + amount
            
            db.runBatch { batch ->
                batch.set(db.collection("transactions").document(txnId), transaction)
                batch.set(db.collection("wallets").document(email), mapOf("balance" to newBalance), com.google.firebase.firestore.SetOptions.merge())
            }
            .addOnSuccessListener { onSuccess() }
        }
    }

    fun initiateEscrow(userEmail: String, shipmentId: String, amount: Double, gateway: String, onSuccess: () -> Unit) {
        val txnId = db.collection("transactions").document().id
        val transaction = Transaction(
            id = txnId,
            userEmail = userEmail,
            shipmentId = shipmentId,
            amount = amount,
            type = "Escrow Payment",
            status = "Escrow",
            gateway = gateway
        )
        
        db.collection("transactions").document(txnId).set(transaction)
            .addOnSuccessListener {
                db.collection("shipments").document(shipmentId).update(mapOf(
                    "paymentStatus" to "Escrowed",
                    "escrowTxnId" to txnId
                ))
                onSuccess()
            }
    }

    fun refundEscrow(shipmentId: String, onSuccess: () -> Unit = {}) {
        db.collection("transactions")
            .whereEqualTo("shipmentId", shipmentId)
            .whereEqualTo("status", "Escrow")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val txn = doc.toObject(Transaction::class.java)
                    db.collection("wallets").document(txn.userEmail).get().addOnSuccessListener { wDoc ->
                        val current = wDoc.getDouble("balance") ?: 0.0
                        db.runBatch { batch ->
                            batch.update(db.collection("wallets").document(txn.userEmail), "balance", current + txn.amount)
                            batch.update(db.collection("transactions").document(txn.id), "status", "Refunded")
                            batch.update(db.collection("shipments").document(shipmentId), "paymentStatus", "Refunded")
                        }.addOnSuccessListener {
                            sendInternalNotification(txn.userEmail, "Refund Processed", "₹${txn.amount} has been refunded to your wallet for shipment cancellation.")
                            onSuccess()
                        }
                    }
                }
            }
    }

    fun initiateDispute(shipmentId: String, reason: String) {
        db.collection("shipments").document(shipmentId).update(mapOf(
            "status" to "Disputed",
            "disputeReason" to reason
        )).addOnSuccessListener {
            db.collection("transactions")
                .whereEqualTo("shipmentId", shipmentId)
                .whereEqualTo("status", "Escrow")
                .get()
                .addOnSuccessListener { docs ->
                    for (doc in docs) {
                        db.collection("transactions").document(doc.id).update("status", "Hold")
                    }
                }
        }
    }

    fun releaseEscrow(shipmentId: String) {
        if (shipmentId.isBlank()) return
        db.collection("transactions")
            .whereEqualTo("shipmentId", shipmentId)
            .whereEqualTo("status", "Escrow")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val txn = doc.toObject(Transaction::class.java)?.copy(id = doc.id) ?: continue
                    db.collection("shipments").document(shipmentId).get().addOnSuccessListener { sDoc ->
                        val driverEmail = sDoc.getString("driverEmail") ?: return@addOnSuccessListener
                        if (driverEmail.isBlank()) return@addOnSuccessListener
                        
                        db.runTransaction { transaction ->
                            val driverWalletRef = db.collection("wallets").document(driverEmail)
                            val driverWalletDoc = transaction.get(driverWalletRef)
                            val currentBalance = if (driverWalletDoc.exists()) driverWalletDoc.getDouble("balance") ?: 0.0 else 0.0
                            
                            val payoutAmount = sDoc.getDouble("driverPayout") ?: (txn.amount * 0.9) // Fallback to 90% if field missing
                            
                            transaction.set(driverWalletRef, mapOf("balance" to currentBalance + payoutAmount), com.google.firebase.firestore.SetOptions.merge())
                            transaction.update(db.collection("transactions").document(txn.id), "status", "Success")
                            
                            val payoutId = db.collection("transactions").document().id
                            val payoutTxn = txn.copy(
                                id = payoutId,
                                userEmail = driverEmail,
                                amount = payoutAmount,
                                type = "Payout",
                                status = "Success",
                                timestamp = System.currentTimeMillis()
                            )
                            transaction.set(db.collection("transactions").document(payoutId), payoutTxn)
                            
                            // Log commission
                            val commissionAmount = sDoc.getDouble("commission") ?: (txn.amount * 0.1)
                            val commissionId = db.collection("transactions").document().id
                            val commissionTxn = hashMapOf(
                                "id" to commissionId,
                                "type" to "Platform Commission",
                                "shipmentId" to shipmentId,
                                "amount" to commissionAmount,
                                "timestamp" to System.currentTimeMillis()
                            )
                            transaction.set(db.collection("platform_earnings").document(commissionId), commissionTxn)
                        }
                    }
                }
            }
    }

    fun handleCodSettlement(shipmentId: String, driverEmail: String, amount: Double) {
        val txnId = db.collection("transactions").document().id
        val transaction = Transaction(
            id = txnId,
            userEmail = driverEmail,
            shipmentId = shipmentId,
            amount = amount,
            type = "COD Collection",
            status = "Success"
        )
        db.collection("transactions").document(txnId).set(transaction)
    }

    fun getTransactionHistory(email: String, onResult: (List<Transaction>) -> Unit): ListenerRegistration {
        return db.collection("transactions")
            .whereEqualTo("userEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "Error fetching transaction history: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                if (value != null) {
                    val txns = value.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
                    onResult(txns)
                }
            }
    }

    fun submitDriverDocs(email: String, license: String, rc: String, idType: String, idNumber: String, truckPhoto: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val docs = hashMapOf(
            "email" to email,
            "licenseUrl" to license,
            "rcUrl" to rc,
            "idType" to idType,
            "idNumber" to idNumber,
            "truckPhotoUrl" to truckPhoto,
            "status" to "Pending",
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("driverDocuments").document(email).set(docs)
            .addOnSuccessListener {
                db.collection("users").document(email).update("verificationStatus", "Pending")
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Failed to update user status") }
            }
            .addOnFailureListener { onError(it.message ?: "Failed to submit documents") }
    }

    fun getDriverDocStatus(email: String, onResult: (String?) -> Unit) {
        db.collection("users").document(email).get()
            .addOnSuccessListener { onResult(it.getString("verificationStatus")) }
    }

    fun updateUserToken(email: String, token: String) {
        db.collection("users").document(email).update("fcmToken", token)
    }

    fun fetchAndSaveFcmToken(email: String) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUserToken(email, task.result)
            }
        }
    }

    fun updateDriverOnlineStatus(email: String, isOnline: Boolean, onSuccess: () -> Unit = {}) {
        db.collection("users").document(email).update("isAvailable", isOnline)
            .addOnSuccessListener { onSuccess() }
    }

    fun updateDriverLocation(shipmentId: String, lat: Double, lng: Double) {
        if (shipmentId.isBlank()) return
        db.collection("shipments").document(shipmentId).update(mapOf("currentLat" to lat, "currentLng" to lng))
    }

    // --- Chat System ---
    fun sendMessage(shipmentId: String, message: String) {
        val email = AuthManager.getCurrentUserEmail() ?: return
        val chatRef = db.collection("shipments").document(shipmentId).collection("messages").document()
        val chatMessage = ChatMessage(
            id = chatRef.id,
            shipmentId = shipmentId,
            senderEmail = email,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        chatRef.set(chatMessage)
    }

    fun listenToChat(shipmentId: String, onMessages: (List<ChatMessage>) -> Unit): ListenerRegistration? {
        if (shipmentId.isBlank()) return null
        return db.collection("shipments").document(shipmentId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    onMessages(value.toObjects(ChatMessage::class.java))
                }
            }
    }

    // --- Expense Tracker ---
    fun addExpense(expense: Expense, onSuccess: () -> Unit) {
        val docRef = db.collection("expenses").document()
        docRef.set(expense.copy(id = docRef.id)).addOnSuccessListener { onSuccess() }
    }

    fun getExpenseHistory(email: String, onResult: (List<Expense>) -> Unit): ListenerRegistration {
        return db.collection("expenses")
            .whereEqualTo("driverEmail", email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getExpenseHistory Error: ${error.message}")
                    return@addSnapshotListener
                }
                if (value != null) {
                    onResult(value.toObjects(Expense::class.java))
                }
            }
    }

    fun markArrivedAtDestination(shipmentId: String, vendorEmail: String, onSuccess: () -> Unit) {
        db.collection("shipments").document(shipmentId).update("status", "Arrived at Destination")
            .addOnSuccessListener {
                sendInternalNotification(vendorEmail, "Driver Arrived", "Your driver has arrived at the destination for shipment #$shipmentId")
                onSuccess()
            }
    }

    fun sendSOSAlert(shipmentId: String, driverEmail: String) {
        val alertId = db.collection("sos_alerts").document().id
        val alert = hashMapOf(
            "id" to alertId,
            "shipmentId" to shipmentId,
            "driverEmail" to driverEmail,
            "timestamp" to System.currentTimeMillis(),
            "status" to "Active"
        )
        db.collection("sos_alerts").document(alertId).set(alert)
        
        // Notify Team
        sendInternalNotification("support@truckify.app", "SOS ALERT", "Emergency alert from driver $driverEmail for shipment $shipmentId")
    }

    fun listenToShipment(shipmentId: String, onUpdate: (Shipment) -> Unit): ListenerRegistration? {
        if (shipmentId.isBlank()) return null
        return db.collection("shipments").document(shipmentId)
            .addSnapshotListener { value, _ ->
                value?.toObject(Shipment::class.java)?.copy(id = value.id)?.let { onUpdate(it) }
            }
    }

    fun assignDriverToShipment(shipmentId: String, driverEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("shipments").document(shipmentId)
            .update(mapOf(
                "driverEmail" to driverEmail
            ))
            .addOnSuccessListener {
                sendInternalNotification(driverEmail, "Load Assigned", "A vendor has assigned you to shipment #$shipmentId")
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Unknown error") }
    }

    fun getSmartMatches(shipmentId: String, onResult: (List<Driver>) -> Unit) {
        if (shipmentId.isBlank()) {
            onResult(emptyList())
            return
        }
        db.collection("shipments").document(shipmentId).get().addOnSuccessListener { shipDoc ->
            val shipment = shipDoc.toObject(Shipment::class.java)
            if (shipment == null) {
                onResult(emptyList())
                return@addOnSuccessListener
            }

            db.collection("users").whereEqualTo("role", "Driver").get().addOnSuccessListener { driverDocs ->
                val drivers = driverDocs.toObjects(Driver::class.java)
                
                // Logic Improvement: filter by availability AND truck compatibility
                val filteredDrivers = drivers.filter { driver ->
                    driver.isAvailable && 
                    driver.verificationStatus == "Verified" &&
                    isTruckCompatible(driver.truckType, shipment.weight)
                }

                val scoredDrivers = filteredDrivers.map { driver ->
                    val distance = com.truckify.app.utils.calculateDistance(
                        shipment.pickupLat, shipment.pickupLng, 
                        driver.currentLat, driver.currentLng
                    )
                    // Score = 40% Trust Score + 30% Rating + 30% Proximity
                    val proximityScore = (100 - (distance / 2)).coerceAtLeast(0.0)
                    val combinedScore = (driver.trustScore * 0.4) + (driver.rating * 20 * 0.3) + (proximityScore * 0.3)
                    driver to combinedScore
                }.sortedByDescending { it.second }.map { it.first }.take(5)

                onResult(scoredDrivers)
            }.addOnFailureListener { onResult(emptyList()) }
        }.addOnFailureListener { onResult(emptyList()) }
    }

    private fun isTruckCompatible(truckType: String, loadWeight: String): Boolean {
        val weightValue = loadWeight.split(" ").firstOrNull()?.toDoubleOrNull() ?: 0.0
        return when (truckType) {
            "Mini Truck" -> weightValue <= 1.5
            "Pick-up" -> weightValue <= 3.0
            "Mid-size" -> weightValue <= 7.0
            "Heavy Duty" -> weightValue > 7.0
            else -> true
        }
    }

    // --- Admin Functions ---
    fun getPendingVerifications(onResult: (List<Map<String, Any>>) -> Unit) {
        db.collection("driverDocuments")
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { value -> onResult(value.documents.mapNotNull { it.data }) }
    }

    fun verifyDriver(email: String, approved: Boolean, reason: String = "") {
        val status = if (approved) "Verified" else "Rejected"
        db.runBatch { batch ->
            batch.update(db.collection("users").document(email), "verificationStatus", status)
            batch.update(db.collection("driverDocuments").document(email), "status", status)
            if (!approved) {
                batch.update(db.collection("driverDocuments").document(email), "rejectionReason", reason)
            }
        }.addOnSuccessListener {
            sendInternalNotification(email, "Verification Update", "Your profile verification was $status. $reason")
        }
    }
}
