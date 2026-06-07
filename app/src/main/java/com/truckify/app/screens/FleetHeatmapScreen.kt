package com.truckify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
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
import com.google.maps.android.compose.*
import com.truckify.app.components.TruckifyTopAppBar
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Driver
import com.truckify.app.ui.theme.DarkBlue
import com.truckify.app.ui.theme.LightBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetHeatmapScreen(onBack: () -> Unit) {
    var drivers by remember { mutableStateOf<List<Driver>>(emptyList()) }
    val delhi = LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(delhi, 10f)
    }
    var isTrafficEnabled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val registration = FirestoreManager.getAllActiveDrivers { 
            drivers = it 
        }
        onDispose { registration.remove() }
    }

    Scaffold(
        topBar = {
            TruckifyTopAppBar(
                title = "Live Fleet Heatmap",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { isTrafficEnabled = !isTrafficEnabled }) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = "Traffic", 
                            tint = if (isTrafficEnabled) LightBlue else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isTrafficEnabled = isTrafficEnabled)
            ) {
                drivers.forEach { driver ->
                    if (driver.currentLat != 0.0) {
                        Marker(
                            state = MarkerState(position = LatLng(driver.currentLat, driver.currentLng)),
                            title = driver.name,
                            snippet = "${driver.truckType} • ${driver.capacity}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (driver.isAvailable) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE
                            )
                        )
                    }
                }

                // Simulated Delivery Zone (NCR Hub)
                Circle(
                    center = delhi,
                    radius = 5000.0,
                    fillColor = LightBlue.copy(alpha = 0.2f),
                    strokeColor = LightBlue,
                    strokeWidth = 2f
                )
            }

            // Legend / Status Overlay
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(Color.Green, "Available")
                    StatusIndicator(Color.Yellow, "In Transit")
                    StatusIndicator(Color.Red, "Delayed")
                }
            }
            
            // Delayed Alert (Simulated)
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("2 Shipments Delayed in Okhla Zone", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
