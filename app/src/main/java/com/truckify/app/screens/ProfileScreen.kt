package com.truckify.app.screens

import androidx.compose.foundation.BorderStroke
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
        containerColor = MaterialTheme.colorScheme.background,
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
                .size(74.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(44.dp))
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = email, 
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = WarningOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$rating", color = WarningOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun VehicleDetailsCardNew(vehicleNo: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "VEHICLE DETAILS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = vehicleNo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "14 Wheels • Open Body", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(50.dp))
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
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
