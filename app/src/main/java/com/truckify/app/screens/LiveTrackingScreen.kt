package com.truckify.app.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import com.truckify.app.ui.theme.*
import com.truckify.app.utils.calculateDistance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(shipmentId: String, onBack: () -> Unit, userRole: String, onChatClick: () -> Unit = {}, onVerifyClick: () -> Unit = {}) {
    val context = LocalContext.current
    var shipment by remember { mutableStateOf<Shipment?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(shipmentId) {
        if (shipmentId.isEmpty()) return@DisposableEffect onDispose {}
        val registration = FirestoreManager.listenToShipment(shipmentId) { updatedShipment ->
            shipment = updatedShipment
            val target = if (updatedShipment.currentLat != 0.0) LatLng(updatedShipment.currentLat, updatedShipment.currentLng) else LatLng(updatedShipment.pickupLat, updatedShipment.pickupLng)
            coroutineScope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 12f))
            }
        }
        onDispose { registration?.remove() }
    }

    if (userRole == "Driver" && shipmentId.isNotEmpty()) {
        DisposableEffect(shipmentId) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(res: LocationResult) {
                    res.lastLocation?.let { 
                        FirestoreManager.updateDriverLocation(shipmentId, it.latitude, it.longitude) 
                    }
                }
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, callback, context.mainLooper)
            }
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Live Tracking", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                shipment?.let { s ->
                    val pickup = LatLng(s.pickupLat, s.pickupLng)
                    val destination = LatLng(s.destLat, s.destLng)
                    val driver = if (s.currentLat != 0.0) LatLng(s.currentLat, s.currentLng) else null

                    Marker(state = MarkerState(position = pickup), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    Marker(state = MarkerState(position = destination), title = "Destination")
                    
                    if (driver != null) {
                        Marker(state = MarkerState(position = driver), title = "Truck", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        Polyline(
                            points = listOf(pickup, driver, destination),
                            color = PrimaryBlue,
                            width = 12f,
                            startCap = RoundCap(),
                            endCap = RoundCap(),
                            jointType = JointType.ROUND
                        )
                    }
                }
            }

            // Driver Card at bottom
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var driverData by remember { mutableStateOf<Map<String, Any>?>(null) }
                LaunchedEffect(shipment?.driverEmail) {
                    shipment?.driverEmail?.let { email ->
                        if (email.isNotEmpty()) {
                            FirestoreManager.getUserData(email) { data -> driverData = data }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(BackgroundDark), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = driverData?.get("name") as? String ?: "Assigning Driver...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = String.format("%.1f", (driverData?.get("rating") as? Number)?.toDouble() ?: 0.0), color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = " • ${driverData?.get("truckNumber") as? String ?: "..."}", color = TextGray, fontSize = 12.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {}, modifier = Modifier.size(44.dp).clip(CircleShape).background(AccentGreen)) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                                }
                                IconButton(onClick = onChatClick, modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryBlue)) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TrackingStat("ETA", "2h 45m")
                            TrackingStat("Distance Left", "120 km")
                            TrackingStat("Next Stop", "Behror")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress Timeline
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TimelineStep("Confirmed", true, true)
                            TimelineStep("Picked Up", true, true)
                            TimelineStep("In Transit", true, false)
                            TimelineStep("Delivered", false, false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingStat(label: String, value: String) {
    Column {
        Text(text = label, color = TextGray, fontSize = 10.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun RowScope.TimelineStep(label: String, active: Boolean, completed: Boolean) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(12.dp).clip(CircleShape).background(if (completed) AccentGreen else if (active) PrimaryBlue else Color.Gray)
            )
            // Connector (this is simplified)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = if (active) Color.White else TextGray, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}
