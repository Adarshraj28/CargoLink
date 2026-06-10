package com.truckify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Driver
import com.truckify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetHeatmapScreen(onBack: () -> Unit) {
    var drivers by remember { mutableStateOf<List<Driver>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        FirestoreManager.getAllActiveDrivers { drivers = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fleet Real-time Map", fontWeight = FontWeight.Bold, color = DarkBlue) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DarkBlue) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
            ) {
                drivers.forEach { driver ->
                    // Show all drivers, even if lat/lng is 0.0 for testing, but ideally only if valid
                    val pos = if (driver.currentLat != 0.0) LatLng(driver.currentLat, driver.currentLng) else LatLng(28.6139, 77.2090)
                    Marker(
                        state = MarkerState(position = pos),
                        title = driver.name,
                        snippet = "${driver.truckType} | Status: ${if (driver.isAvailable) "Available" else "Busy"}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (driver.isAvailable) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }

            // Legend / Info Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Fleet Insight", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${drivers.size} Trucks Online", color = Color.Gray, fontSize = 12.sp)
                        }
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "LIVE", 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MapStatItem(label = "Available", value = drivers.count { it.isAvailable }.toString(), color = SuccessGreen)
                        MapStatItem(label = "In-Transit", value = drivers.count { !it.isAvailable }.toString(), color = WarningOrange)
                        MapStatItem(label = "Incidents", value = "0", color = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun MapStatItem(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$value $label", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
