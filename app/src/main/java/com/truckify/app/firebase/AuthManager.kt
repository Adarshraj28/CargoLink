package com.truckify.app.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthManager {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    suspend fun signupUser(
        name: String,
        email: String,
        password: String,
        role: String
    ): Result<Unit> {
        return try {
            val cleanEmail = email.lowercase().trim()
            auth.createUserWithEmailAndPassword(cleanEmail, password).await()
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
            db.collection("users").document(cleanEmail).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            val cleanEmail = email.lowercase().trim()
            auth.signInWithEmailAndPassword(cleanEmail, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return try {
            auth.currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserRole(): String? {
        val currentUser = auth.currentUser ?: return "NoCurrentUser"
        val email = currentUser.email?.lowercase()?.trim() ?: return "NoEmail"

        return try {
            val document = db.collection("users").document(email).get().await()
            if (document.exists()) {
                val role = document.getString("role")
                android.util.Log.d("Truckify", "Fetched role for $email: $role")
                role ?: "NoRoleField"
            } else {
                android.util.Log.w("Truckify", "User document missing for $email")
                "ProfileMissing"
            }
        } catch (e: Exception) {
            android.util.Log.e("Truckify", "Error fetching user role: ${e.message}")
            "Error: ${e.message}"
        }
    }

    fun getCurrentUserEmail(): String? {
        return try {
            auth.currentUser?.email
        } catch (e: Exception) {
            null
        }
    }
}
