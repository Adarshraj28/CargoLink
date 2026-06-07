package com.truckify.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signup(name: String, email: String, password: String, role: String): Result<Unit>
    fun logout()
    fun isUserLoggedIn(): Boolean
    suspend fun getUserRole(): String?
    fun getCurrentUserEmail(): String?
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email.lowercase().trim(), password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signup(name: String, email: String, password: String, role: String): Result<Unit> {
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

    override fun logout() {
        auth.signOut()
    }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun getUserRole(): String? {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: return null
        return try {
            val doc = db.collection("users").document(email).get().await()
            doc.getString("role")
        } catch (e: Exception) {
            null
        }
    }

    override fun getCurrentUserEmail(): String? = auth.currentUser?.email
}
