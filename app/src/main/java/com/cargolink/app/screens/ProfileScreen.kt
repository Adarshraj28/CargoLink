package com.cargolink.app.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.components.CargoLinkTopAppBar
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onPersonalInfoClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onPayoutsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onReferClick: () -> Unit = {},
    onChatbotClick: () -> Unit = {}
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
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            CargoLinkTopAppBar(
                title = "My Account",
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
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Beige.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Beige.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        ProfileMenuItemNew(title = "Personal Information", icon = Icons.Default.Person, onClick = onPersonalInfoClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Beige.copy(alpha = 0.2f))
                        ProfileMenuItemNew(title = "Documents & Identity", icon = Icons.Default.Description, onClick = onDocumentsClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Beige.copy(alpha = 0.2f))
                        ProfileMenuItemNew(title = "Payments & Wallet", icon = Icons.Default.AccountBalance, onClick = onPayoutsClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Beige.copy(alpha = 0.2f))
                        ProfileMenuItemNew(title = "Refer friends, earn ₹2000", icon = Icons.Default.CardGiftcard, onClick = onReferClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Beige.copy(alpha = 0.2f))
                        ProfileMenuItemNew(title = "Get Support", icon = Icons.Default.HeadsetMic, onClick = onSupportClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Beige.copy(alpha = 0.2f))
                        ProfileMenuItemNew(title = "AI Assistant", icon = Icons.Default.AutoAwesome, onClick = onChatbotClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Beige.copy(alpha = 0.2f))
                        ProfileMenuItemNew(title = "Account Settings", icon = Icons.Default.Settings, onClick = onSettingsClick)
                    }
                }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PremiumGradient)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(4.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = DarkBlue
            )
            Text(
                text = email, 
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = WarningOrange.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "$rating Top Rated", color = WarningOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun VehicleDetailsCardNew(vehicleNo: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = PrimaryBlue.copy(alpha = 0.15f)).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.background(Brush.horizontalGradient(listOf(Color.White, PrimaryBlue.copy(alpha = 0.02f)))).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "PRIMARY VEHICLE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = vehicleNo, fontWeight = FontWeight.Black, fontSize = 22.sp, color = DarkBlue)
                Text(text = "Heavy Duty • 14 Wheeler", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = PrimaryBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                }
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
