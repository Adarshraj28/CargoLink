package com.cargolink.app.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.ui.theme.BackgroundDark
import com.cargolink.app.ui.theme.DarkBlue
import com.cargolink.app.ui.theme.PrimaryBlue
import com.cargolink.app.ui.theme.TextGray

import com.cargolink.app.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Vendor") }
    var loading by remember { mutableStateOf(false) }
    var isNewUser by remember { mutableStateOf(false) }
    
    val userEmail = AuthManager.getCurrentUserEmail() ?: ""

    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            FirestoreManager.getUserData(userEmail) { data ->
                if (data == null) {
                    isNewUser = true
                    // Prefill phone if available
                    if (userEmail.startsWith("+")) phone = userEmail
                } else {
                    name = data["name"] as? String ?: ""
                    phone = data["phone"] as? String ?: (data["email"] as? String ?: "")
                    vehicleNo = data["vehicleNo"] as? String ?: ""
                    selectedRole = data["role"] as? String ?: "Vendor"
                }
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text(if (isNewUser) "Complete Profile" else "Edit Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isNewUser) {
                Text("Select your role:", color = Color.White, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val roles = listOf("Vendor", "Driver")
                    roles.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.White,
                                selectedContainerColor = PrimaryBlue
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number / Email", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Gray
                )
            )

            if (selectedRole == "Driver") {
                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it },
                    label = { Text("Vehicle Number", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    loading = true
                    val updates = mutableMapOf<String, Any>(
                        "name" to name,
                        "phone" to phone,
                        "role" to selectedRole,
                        "verificationStatus" to if (selectedRole == "Vendor") "Verified" else "Not Started"
                    )
                    if (selectedRole == "Driver") {
                        updates["vehicleNo"] = vehicleNo
                    }
                    
                    val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(userEmail)
                        
                    val task = if (isNewUser) {
                        // For new users, set additional fields
                        updates["trustScore"] = 90.0
                        updates["totalTrips"] = 0
                        updates["rating"] = 4.5
                        updates["isPhoneVerified"] = userEmail.startsWith("+")
                        docRef.set(updates)
                    } else {
                        docRef.update(updates as Map<String, Any>)
                    }
                    
                    task.addOnSuccessListener {
                        loading = false
                        Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                        authViewModel.checkAuthStatus()
                        onBack()
                    }
                    .addOnFailureListener {
                        loading = false
                        Toast.makeText(context, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(if (isNewUser) "Complete Profile" else "Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
