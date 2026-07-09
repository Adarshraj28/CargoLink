package com.cargolink.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import android.app.Activity
import android.content.SharedPreferences

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signup(name: String, email: String, password: String, role: String): Result<Unit>
    fun logout()
    fun isUserLoggedIn(): Boolean
    suspend fun getUserRole(): String?
    suspend fun isPhoneVerified(): Boolean
    fun getCurrentUserEmail(): String?
    
    // Phone Auth
    fun verifyPhoneNumber(
        phoneNumber: String, 
        activity: Activity, 
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )
    suspend fun signInWithPhone(verificationId: String, code: String): Result<Unit>
    suspend fun markPhoneAsVerified(): Result<Unit>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val prefs: SharedPreferences
) : AuthRepository {

    private fun getUserIdentifier(): String? {
        val user = auth.currentUser ?: return null
        return user.email?.lowercase()?.trim() ?: user.phoneNumber
    }

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
                "isPhoneVerified" to false,
                "verificationStatus" to if (role == "Vendor") "Verified" else "Not Started"
            )
            db.collection("users").document(cleanEmail).set(user).await()
            // Cache role locally
            prefs.edit().putString("user_role", role).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        auth.signOut()
        prefs.edit().remove("user_role").apply()
    }

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun getUserRole(): String? {
        // Try to return cached role first for instant login
        val cachedRole = prefs.getString("user_role", null)
        if (cachedRole != null) return cachedRole

        val identifier = getUserIdentifier() ?: return null
        return try {
            val doc = db.collection("users").document(identifier).get().await()
            val role = doc.getString("role")
            if (role != null) {
                prefs.edit().putString("user_role", role).apply()
                role
            } else if (doc.exists()) {
                "ProfileMissing"
            } else {
                "NewUser"
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun isPhoneVerified(): Boolean {
        val identifier = getUserIdentifier() ?: return false
        return try {
            val doc = db.collection("users").document(identifier).get().await()
            doc.getBoolean("isPhoneVerified") ?: (auth.currentUser?.phoneNumber != null)
        } catch (e: Exception) {
            auth.currentUser?.phoneNumber != null
        }
    }

    override fun getCurrentUserEmail(): String? = auth.currentUser?.email ?: auth.currentUser?.phoneNumber

    override fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun signInWithPhone(verificationId: String, code: String): Result<Unit> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markPhoneAsVerified(): Result<Unit> {
        val identifier = getUserIdentifier() ?: return Result.failure(Exception("No user logged in"))
        return try {
            val doc = db.collection("users").document(identifier).get().await()
            if (doc.exists()) {
                db.collection("users").document(identifier).update("isPhoneVerified", true).await()
            } else {
                // If doc doesn't exist, we'll create it later during profile completion
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
