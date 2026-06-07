package com.truckify.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.TruckifyTopAppBar
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onPersonalInfoClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onPayoutsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    val userEmail = AuthManager.getCurrentUserEmail() ?: ""

    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            FirestoreManager.getUserData(userEmail) { data ->
                userData = data
            }
        }
    }

    val name = userData?.get("name") as? String ?: "Ashok Kumar"
    val role = userData?.get("role") as? String ?: "Driver"
    val rating = (userData?.get("rating") as? Double) ?: 4.8
    val vehicleNo = userData?.get("vehicleNo") as? String ?: "RJ14GA2345"

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TruckifyTopAppBar(
                title = "Profile",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                ProfileHeaderNew(name, userEmail, rating)
            }

            if (role == "Driver") {
                item {
                    VehicleDetailsCardNew(vehicleNo) { onPersonalInfoClick() }
                }
            }

            item {
                ProfileMenuItemNew(title = "Personal Information", icon = Icons.Default.Person, onClick = onPersonalInfoClick)
                ProfileMenuItemNew(title = "Documents", icon = Icons.Default.Description, onClick = onDocumentsClick)
                ProfileMenuItemNew(title = "Payout & Bank Details", icon = Icons.Default.AccountBalance, onClick = onPayoutsClick)
                ProfileMenuItemNew(title = "Support", icon = Icons.Default.HeadsetMic, onClick = onSupportClick)
                ProfileMenuItemNew(title = "Settings", icon = Icons.Default.Settings, onClick = onSettingsClick)
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ProfileHeaderNew(name: String, email: String, rating: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(CardDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            )
            Text(
                text = "+91 98765 43210", // Placeholder for phone
                fontSize = 14.sp,
                color = TextGray
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$rating", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun VehicleDetailsCardNew(vehicleNo: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Vehicle Details", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = vehicleNo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text(text = "14 Wheels • Open Body", color = TextGray, fontSize = 14.sp)
                }
                // Truck image placeholder
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(60.dp))
            }
        }
    }
}

@Composable
fun ProfileMenuItemNew(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color.White
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGray)
    }
}
