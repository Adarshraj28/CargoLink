package com.cargolink.app.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sync
import com.google.android.gms.maps.model.MapStyleOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Driver
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentConfirmingScreen(shipmentId: String, onBack: () -> Unit, onConfirmed: () -> Unit = {}) {
    var shipment by remember { mutableStateOf<Shipment?>(null) }
    var drivers by remember { mutableStateOf<List<Driver>>(emptyList()) }
    val cameraPositionState = rememberCameraPositionState()

    DisposableEffect(shipmentId) {
        if (shipmentId.isEmpty()) return@DisposableEffect onDispose {}
        val registration = FirestoreManager.listenToShipment(shipmentId) { updated ->
            shipment = updated
            if (updated.status == "In Transit") {
                onConfirmed()
            }
            if (updated.pickupLat != 0.0) {
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(updated.pickupLat, updated.pickupLng), 14f))
            }
        }
        val driverReg = FirestoreManager.getAllActiveDrivers { list ->
            drivers = list
        }
        onDispose {
            registration?.remove()
            driverReg.remove()
        }
    }

    Scaffold(
        containerColor = BackgroundWhite
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = MapStyleOptions("""
                        [
                          {
                            "elementType": "geometry",
                            "stylers": [{ "color": "#ebe3cd" }]
                          },
                          {
                            "featureType": "road",
                            "elementType": "geometry",
                            "stylers": [{ "color": "#f5f1e6" }]
                          },
                          {
                            "featureType": "water",
                            "elementType": "geometry",
                            "stylers": [{ "color": "#c9dfdf" }]
                          }
                        ]
                    """.trimIndent())
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
                shipment?.let { s ->
                    // Animated Circle around Pickup
                    Circle(
                        center = LatLng(s.pickupLat, s.pickupLng),
                        radius = 500.0,
                        fillColor = PrimaryBlue.copy(alpha = 0.1f),
                        strokeColor = PrimaryBlue,
                        strokeWidth = 2f
                    )
                    
                    Marker(
                        state = MarkerState(position = LatLng(s.pickupLat, s.pickupLng)),
                        title = "Pickup Point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                drivers.forEach { driver ->
                    if (driver.currentLat != 0.0) {
                        Marker(
                            state = MarkerState(position = LatLng(driver.currentLat, driver.currentLng)),
                            title = driver.name,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                        )
                    }
                }
            }

            // Top Status Bar (Transparent)
            Surface(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkBlue)
                    }
                    
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finding Captain...", color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(44.dp)) // For symmetry
                }
            }

            // Rapido-style Bottom Sheet
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingPulse()
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Looking for Captains", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DarkBlue)
                            Text("Matching you with the best driver nearby", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = PrimaryBlue,
                        trackColor = PrimaryBlue.copy(alpha = 0.1f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Route Preview Mini
                    shipment?.let { s ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(s.pickupAddress.split(",").first(), color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(s.destinationAddress.split(",").first(), color = DarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    var isCancelling by remember { mutableStateOf(false) }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            isCancelling = true
                            FirestoreManager.cancelShipment(shipmentId) {
                                isCancelling = false
                                android.widget.Toast.makeText(context, "Shipment Cancelled", android.widget.Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ErrorRed, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Request", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingPulse() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
        ) {
            Icon(
                Icons.Default.LocalShipping, 
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
