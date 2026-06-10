package com.truckify.app.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Navigation
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
import android.content.Intent
import android.net.Uri
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
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
fun LiveTrackingScreen(
    shipmentId: String, 
    onBack: () -> Unit, 
    userRole: String, 
    onChatClick: () -> Unit = {}, 
    onVerifyClick: () -> Unit = {},
    onAvailableRedirect: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var shipment by remember { mutableStateOf<Shipment?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(shipmentId) {
        if (shipmentId.isEmpty()) return@DisposableEffect onDispose {}
        val registration = FirestoreManager.listenToShipment(shipmentId) { updatedShipment ->
            if (userRole == "Vendor" && updatedShipment.status == "Available") {
                onAvailableRedirect(shipmentId)
            }
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
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = userRole == "Driver",
                    mapStyleOptions = MapStyleOptions("""
                        [
                          {
                            "elementType": "geometry",
                            "stylers": [{ "color": "#242f3e" }]
                          },
                          {
                            "elementType": "labels.text.fill",
                            "stylers": [{ "color": "#746855" }]
                          },
                          {
                            "elementType": "labels.text.stroke",
                            "stylers": [{ "color": "#242f3e" }]
                          },
                          {
                            "featureType": "administrative.locality",
                            "elementType": "labels.text.fill",
                            "stylers": [{ "color": "#d59563" }]
                          },
                          {
                            "featureType": "road",
                            "elementType": "geometry",
                            "stylers": [{ "color": "#38414e" }]
                          },
                          {
                            "featureType": "road",
                            "elementType": "geometry.stroke",
                            "stylers": [{ "color": "#212a37" }]
                          },
                          {
                            "featureType": "water",
                            "elementType": "geometry",
                            "stylers": [{ "color": "#17263c" }]
                          }
                        ]
                    """.trimIndent())
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                shipment?.let { s ->
                    val pickup = LatLng(s.pickupLat, s.pickupLng)
                    val destination = LatLng(s.destLat, s.destLng)
                    val driver = if (s.currentLat != 0.0) LatLng(s.currentLat, s.currentLng) else null

                    Marker(state = MarkerState(position = pickup), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    Marker(state = MarkerState(position = destination), title = "Destination")
                    
                    if (driver != null) {
                        Marker(state = MarkerState(position = driver), title = "Truck", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        
                        // Route from Driver -> Pickup -> Destination
                        Polyline(
                            points = if (s.status == "In Transit") {
                                // Already picked up, show route to destination
                                listOf(driver, destination)
                            } else {
                                // Not picked up yet, show route to pickup then destination
                                listOf(driver, pickup, destination)
                            },
                            color = PrimaryBlue,
                            width = 10f,
                            startCap = RoundCap(),
                            endCap = RoundCap(),
                            jointType = JointType.ROUND
                        )
                    }
                }
            }

            // Bottom UI matching the image
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
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2632))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        if (userRole == "Vendor") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF0D1117)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = driverData?.get("name") as? String ?: "Assigning...", 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 18.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format(java.util.Locale.getDefault(), "%.1f", (driverData?.get("rating") as? Number)?.toDouble() ?: 4.8), 
                                            color = Color(0xFFFFB300), 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = " • ${driverData?.get("truckNumber") as? String ?: "RJ14GA2345"} • 14 Wheels", 
                                            color = TextGray, 
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val phone = driverData?.get("phone") as? String ?: "9988776655"
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        }, 
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SuccessGreen)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                                    }
                                    IconButton(
                                        onClick = onChatClick, 
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryBlue)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        } else {
                            // Driver View Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = PrimaryBlue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Ongoing Trip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Head towards destination", color = TextGray, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        shipment?.let { s ->
                            val distanceLeft = if (s.currentLat != 0.0) {
                                calculateDistance(s.currentLat, s.currentLng, s.destLat, s.destLng)
                            } else {
                                calculateDistance(s.pickupLat, s.pickupLng, s.destLat, s.destLng)
                            }
                            val etaHours = distanceLeft / 45.0
                            val etaString = if (etaHours < 1.0) "${(etaHours * 60).toInt()}m" else "${etaHours.toInt()}h ${(etaHours % 1 * 60).toInt()}m"

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TrackingStat("ETA", etaString)
                                TrackingStat("Distance Left", String.format(java.util.Locale.getDefault(), "%.0f km", distanceLeft))
                                if (s.status == "In Transit") {
                                    TrackingStat("Delivery OTP", s.deliveryOtp)
                                } else {
                                    TrackingStat("Next Stop", "Behror")
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Progress Timeline matching the image dots
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                TimelineStep("Confirmed", active = true, completed = true)
                                TimelineStep("Picked Up", active = s.status != "Available", completed = s.status != "Available")
                                TimelineStep("In Transit", active = s.status == "In Transit", completed = s.status == "Delivered")
                                TimelineStep("Delivered", active = s.status == "Delivered", completed = s.status == "Delivered")
                            }
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
