package com.truckify.app.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.truckify.app.models.Shipment
import com.truckify.app.models.Driver
import com.truckify.app.models.Transaction
import com.truckify.app.models.ChatMessage
import com.truckify.app.models.Expense

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
            "verificationStatus" to "Not Started"
        )
        db.collection("users").document(cleanEmail).set(user)
            .addOnFailureListener { android.util.Log.e("FirestoreManager", "saveUser Error: ${it.message}") }
    }

    fun postShipment(shipment: Shipment, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val docRef = db.collection("shipments").document()
        val finalShipment = shipment.copy(
            id = docRef.id,
            podToken = java.util.UUID.randomUUID().toString()
        )
        docRef.set(finalShipment)
            .addOnSuccessListener {
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
            }
            .addOnFailureListener { onError(it.message ?: "Failed to post") }
    }

    fun getAvailableLoads(onResult: (List<Shipment>) -> Unit): ListenerRegistration {
        return db.collection("shipments")
            .whereEqualTo("status", "Available")
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
            .whereEqualTo("vendorEmail", email)
            .whereIn("status", listOf("In Transit", "Available"))
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getActiveShipments Error: ${error.message}")
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

    fun getShipmentHistory(email: String, role: String, onResult: (List<Shipment>) -> Unit): ListenerRegistration {
        val field = if (role == "Vendor") "vendorEmail" else "driverEmail"
        return db.collection("shipments")
            .whereEqualTo(field, email)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "getShipmentHistory Error: ${error.message}")
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreManager", "listenForNewLoads Error: ${error.message}")
                    return@addSnapshotListener
                }
                val docs = value?.documents ?: return@addSnapshotListener
                for (doc in docs) {
                    val shipment = doc.toObject(Shipment::class.java)?.copy(id = doc.id)
                    if (shipment != null) {
                        onNewLoad(shipment)
                        break // We only want to handle the newest one for the popup
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
            val vStatus = userDoc.getString("verificationStatus") ?: "Not Started"
            if (vStatus != "Verified") {
                onError("Please verify your account to accept loads.")
                return@addOnSuccessListener
            }

            db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
                val shipment = doc.toObject(Shipment::class.java) ?: return@addOnSuccessListener onError("Shipment not found")
                
                if (shipment.status != "Available") {
                    onError("Shipment no longer available")
                    return@addOnSuccessListener
                }

                val vendorEmail = shipment.vendorEmail
                val cleanPrice = shipment.price.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0

                db.runTransaction { transaction ->
                    val shipmentRef = db.collection("shipments").document(shipmentId)
                    val currentShipment = transaction.get(shipmentRef).toObject(Shipment::class.java)
                    
                    if (currentShipment?.status != "Available") {
                        throw Exception("Shipment already taken")
                    }

                    transaction.update(shipmentRef, mapOf(
                        "status" to "In Transit",
                        "driverEmail" to driverEmail
                    ))

                    if (shipment.paymentMode != "COD" && cleanPrice > 0) {
                        val vendorWalletRef = db.collection("wallets").document(vendorEmail)
                        val vendorWalletDoc = transaction.get(vendorWalletRef)
                        val currentBalance = vendorWalletDoc.getDouble("balance") ?: 0.0
                        
                        if (currentBalance >= cleanPrice) {
                            transaction.update(vendorWalletRef, "balance", currentBalance - cleanPrice)
                            
                            val txnId = db.collection("transactions").document().id
                            val escrowTxn = mapOf(
                                "id" to txnId,
                                "userEmail" to vendorEmail,
                                "shipmentId" to shipmentId,
                                "amount" to cleanPrice,
                                "type" to "Escrow Payment",
                                "status" to "Escrow",
                                "timestamp" to System.currentTimeMillis()
                            )
                            transaction.set(db.collection("transactions").document(txnId), escrowTxn)
                            transaction.update(shipmentRef, "paymentStatus", "Escrowed")
                        } else {
                            throw Exception("Insufficient Vendor Balance")
                        }
                    }
                }.addOnSuccessListener {
                    sendInternalNotification(vendorEmail, "Load Accepted", "Driver $driverEmail has accepted your shipment #$shipmentId.")
                    onSuccess()
                }.addOnFailureListener { onError(it.message ?: "Failed to accept") }
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

    fun completeShipment(shipmentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Now handled by verifyPodWithToken
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
                db.collection("shipments").document(shipmentId)
                    .update(mapOf(
                        "status" to "In Transit",
                        "deliveryOtp" to otp,
                        "pickupTime" to System.currentTimeMillis(),
                        "pickupActualLat" to lat,
                        "pickupActualLng" to lng
                    ))
                    .addOnSuccessListener {
                        sendInternalNotification(shipment.vendorEmail, "Load Picked Up", "Shipment #${shipment.id.takeLast(4)} is now In Transit. Delivery OTP: $otp")
                        onSuccess()
                    }
            } else {
                onError("Invalid QR Code")
            }
        }
    }

    fun verifyDeliveryOtp(shipmentId: String, otp: String, lat: Double, lng: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (shipmentId.isBlank()) {
            onError("Invalid shipment ID")
            return
        }
        db.collection("shipments").document(shipmentId).get().addOnSuccessListener { doc ->
            val shipment = doc.toObject(Shipment::class.java) ?: return@addOnSuccessListener
            if (shipment.deliveryOtp == otp) {
                db.collection("shipments").document(shipmentId)
                    .update(mapOf(
                        "status" to "Delivered",
                        "deliveryTime" to System.currentTimeMillis(),
                        "deliveryActualLat" to lat,
                        "deliveryActualLng" to lng
                    ))
                    .addOnSuccessListener {
                        sendInternalNotification(shipment.vendorEmail, "Shipment Delivered", "Your shipment #${shipment.id.takeLast(4)} has been delivered successfully.")
                        sendInternalNotification(shipment.driverEmail, "Payment Received", "POD Verified! Payment for shipment #${shipment.id.takeLast(4)} has been added to your wallet.")
                        
                        if (shipment.paymentMode == "COD") {
                            val cleanPrice = shipment.price.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0
                            handleCodSettlement(shipmentId, shipment.driverEmail, cleanPrice)
                        } else {
                            releaseEscrow(shipmentId)
                        }
                        updateDriverStats(shipment.driverEmail, isOnTime = true, isSuccessfulScan = true)
                        onSuccess()
                    }
            } else {
                onError("Invalid OTP")
            }
        }
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

    private fun sendInternalNotification(recipientEmail: String, title: String, message: String) {
        val notification = hashMapOf(
            "recipientEmail" to recipientEmail,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("notifications").add(notification)
    }

    fun getUserData(email: String, onResult: (Map<String, Any>?) -> Unit) {
        val cleanEmail = email.lowercase().trim()
        db.collection("users").document(cleanEmail).get()
            .addOnSuccessListener { document -> onResult(document.data) }
            .addOnFailureListener { onResult(null) }
    }

    fun submitDriverRating(driverEmail: String, rating: Double) {
        db.collection("users").document(driverEmail).get().addOnSuccessListener { doc ->
            val driver = doc.toObject(Driver::class.java) ?: return@addOnSuccessListener
            val currentRating = driver.rating
            val totalTrips = driver.totalTrips
            val newRating = ((currentRating * totalTrips) + rating) / (totalTrips + 1)
            
            db.collection("users").document(driverEmail).update("rating", newRating).addOnSuccessListener {
                updateDriverStats(driverEmail) // Triggers trust score recalculation with new rating
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
                            
                            transaction.set(driverWalletRef, mapOf("balance" to currentBalance + txn.amount), com.google.firebase.firestore.SetOptions.merge())
                            transaction.update(db.collection("transactions").document(txn.id), "status", "Success")
                            
                            val payoutId = db.collection("transactions").document().id
                            val payoutTxn = txn.copy(
                                id = payoutId,
                                userEmail = driverEmail,
                                type = "Payout",
                                status = "Success",
                                timestamp = System.currentTimeMillis()
                            )
                            transaction.set(db.collection("transactions").document(payoutId), payoutTxn)
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

    fun markArrived(shipmentId: String, vendorEmail: String) {
        sendInternalNotification(vendorEmail, "Shipment Arrived", "Your driver has arrived at the destination for shipment #$shipmentId")
    }

    fun listenToShipment(shipmentId: String, onUpdate: (Shipment) -> Unit): ListenerRegistration? {
        if (shipmentId.isBlank()) return null
        return db.collection("shipments").document(shipmentId)
            .addSnapshotListener { value, _ ->
                value?.toObject(Shipment::class.java)?.copy(id = value.id)?.let { onUpdate(it) }
            }
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
