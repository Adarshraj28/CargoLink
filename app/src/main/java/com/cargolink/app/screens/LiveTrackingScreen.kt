package com.cargolink.app.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.service.TrackingService
import com.cargolink.app.models.Shipment
import com.cargolink.app.models.Review
import com.cargolink.app.ui.theme.*
import com.cargolink.app.utils.calculateDistance
import kotlinx.coroutines.delay
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
    val coroutineScope = rememberCoroutineScope()
    var shipment by remember { mutableStateOf<Shipment?>(null) }
    var deliveryOtp by remember { mutableStateOf<String?>(null) }
    var driverData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(shipment?.status) {
        if (shipment?.status == "Delivered") {
            val alreadyRated = if (userRole == "Vendor") shipment?.vendorRatedDriver == true else shipment?.driverRatedVendor == true
            if (!alreadyRated) {
                delay(1000)
                showRatingDialog = true
            }
        }
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5f)
    }

    DisposableEffect(shipmentId) {
        if (shipmentId.isEmpty()) return@DisposableEffect onDispose {}
        
        val registration = FirestoreManager.listenToShipment(shipmentId) { updatedShipment ->
            if (userRole == "Vendor" && updatedShipment.status == "Available") {
                onAvailableRedirect(shipmentId)
            }
            coroutineScope.launch {
                val target = when {
                    updatedShipment.currentLat != 0.0 -> LatLng(updatedShipment.currentLat, updatedShipment.currentLng)
                    updatedShipment.status == "Accepted" || updatedShipment.status == "Arrived" -> LatLng(updatedShipment.pickupLat, updatedShipment.pickupLng)
                    else -> LatLng(updatedShipment.destLat, updatedShipment.destLng)
                }
                
                if (shipment == null && updatedShipment.currentLat != 0.0) {
                    val bounds = LatLngBounds.Builder()
                        .include(LatLng(updatedShipment.currentLat, updatedShipment.currentLng))
                        .include(if (updatedShipment.status == "Accepted") LatLng(updatedShipment.pickupLat, updatedShipment.pickupLng) else LatLng(updatedShipment.destLat, updatedShipment.destLng))
                        .build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } else {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 14f))
                }
            }
            shipment = updatedShipment
        }
        
        var otpReg: com.google.firebase.firestore.ListenerRegistration? = null
        if (userRole == "Vendor") {
            otpReg = FirestoreManager.getShipmentOtp(shipmentId) { otp -> deliveryOtp = otp }
        }
        
        onDispose {
            registration?.remove()
            otpReg?.remove()
        }
    }

    if (userRole == "Driver" && shipmentId.isNotEmpty()) {
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
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

    LaunchedEffect(shipment?.driverEmail) {
        shipment?.driverEmail?.let { email ->
            if (email.isNotEmpty()) {
                FirestoreManager.getUserData(email) { data -> driverData = data }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = { TrackingTopBar(onBack, userRole, context) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            MapLayer(shipment, userRole, cameraPositionState)

            Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (userRole == "Driver") {
                    SOSButton {
                        shipment?.let { s ->
                            FirestoreManager.sendSOSAlert(s.id, s.driverEmail)
                            Toast.makeText(context, "SOS Alert Sent", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = {
                        shipment?.let { s ->
                            val target = if (s.currentLat != 0.0) LatLng(s.currentLat, s.currentLng) else LatLng(s.pickupLat, s.pickupLng)
                            coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f)) }
                        }
                    },
                    containerColor = DarkSurface,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, "Recenter")
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                ShipmentDetailSheet(
                    shipment = shipment,
                    driverData = driverData,
                    userRole = userRole,
                    deliveryOtp = deliveryOtp,
                    onChatClick = onChatClick,
                    onVerifyClick = onVerifyClick,
                    onBack = onBack
                )
            }

            if (showRatingDialog && shipment != null) {
                RatingBottomSheet(
                    userRole = userRole,
                    shipment = shipment!!,
                    targetName = if (userRole == "Vendor") (driverData?.get("name") as? String ?: "Driver") else "Vendor",
                    onDismiss = { showRatingDialog = false },
                    onSubmit = { review ->
                        if (userRole == "Vendor") {
                            FirestoreManager.submitDriverReview(review) { showRatingDialog = false }
                        } else {
                            FirestoreManager.submitVendorReview(review) { showRatingDialog = false }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingTopBar(onBack: () -> Unit, userRole: String, context: android.content.Context) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Live Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = if (userRole == "Driver") "On the move" else "Shipment tracking active",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBackground.copy(alpha = 0.8f))
    )
}

@Composable
fun MapLayer(shipment: Shipment?, userRole: String, cameraPositionState: CameraPositionState) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = userRole == "Driver",
            mapStyleOptions = MapStyleOptions(DarkMapStyle)
        ),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, tiltGesturesEnabled = true)
    ) {
        shipment?.let { s ->
            val pickup = LatLng(s.pickupLat, s.pickupLng)
            val dest = LatLng(s.destLat, s.destLng)
            val driver = if (s.currentLat != 0.0) LatLng(s.currentLat, s.currentLng) else null

            Marker(
                state = MarkerState(position = pickup),
                title = "Pickup",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
            Marker(
                state = MarkerState(position = dest),
                title = "Destination",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
            
            if (driver != null) {
                Marker(
                    state = MarkerState(position = driver),
                    title = "Driver",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
                
                val routePoints = when (s.status) {
                    "Accepted" -> listOf(driver, pickup)
                    "In Transit" -> listOf(driver, dest)
                    else -> emptyList<LatLng>()
                }

                if (routePoints.isNotEmpty()) {
                    Polyline(
                        points = routePoints,
                        color = PrimaryBlue,
                        width = 12f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                }
            }
        }
    }
}

@Composable
fun ShipmentDetailSheet(
    shipment: Shipment?,
    driverData: Map<String, Any>?,
    userRole: String,
    deliveryOtp: String?,
    onChatClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth().shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(Beige.copy(alpha = 0.2f)).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(20.dp))

            if (shipment == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryBlue)
            } else {
                val statusText = when(shipment.status) {
                    "Accepted" -> "Driver heading to pickup"
                    "Arrived" -> "Driver at pickup"
                    "In Transit" -> "Heading to destination"
                    else -> shipment.status
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(DarkCard), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (userRole == "Vendor") (driverData?.get("name") as? String ?: "Driver Found") else "Trip Active", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = statusText, color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(icon = Icons.Default.Call, color = SuccessGreen) {
                            val phone = driverData?.get("phone") as? String ?: "9999999999"
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                        ActionButton(icon = Icons.AutoMirrored.Filled.Chat, color = PrimaryBlue, onClick = onChatClick)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val targetLat = if (shipment.status == "Accepted") shipment.pickupLat else shipment.destLat
                    val targetLng = if (shipment.status == "Accepted") shipment.pickupLng else shipment.destLng
                    
                    val distance = if (shipment.currentLat != 0.0) {
                        calculateDistance(shipment.currentLat, shipment.currentLng, targetLat, targetLng)
                    } else {
                        calculateDistance(shipment.pickupLat, shipment.pickupLng, shipment.destLat, shipment.destLng)
                    }
                    val eta = (distance / 40.0 * 60).toInt().coerceAtLeast(1)
                    
                    TripStat(if (shipment.status == "Accepted") "TO PICKUP" else "TO DROP", "${String.format("%.1f", distance)} km", Icons.Default.Route)
                    TripStat("ETA", "$eta min", Icons.Default.AccessTime)
                    
                    if (userRole == "Vendor" && shipment.status == "In Transit") {
                        TripStat("OTP", deliveryOtp ?: "----", Icons.Default.VpnKey)
                    } else {
                        TripStat("PRICE", shipment.price, Icons.Default.Payments)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                ProfessionalTimeline(shipment.status)
                Spacer(modifier = Modifier.height(32.dp))

                if (userRole == "Driver") DriverActions(shipment, context, onVerifyClick, onBack)
                else VendorActions(shipment, onVerifyClick)
            }
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(44.dp), shape = CircleShape, color = color.copy(alpha = 0.15f), contentColor = color) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun TripStat(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun ProfessionalTimeline(status: String) {
    val stages = listOf("Accepted", "Arrived", "In Transit", "Reached", "Delivered")
    val displayStatus = if (status == "Arrived at Destination") "Reached" else status
    val currentIndex = stages.indexOf(displayStatus).coerceAtLeast(0)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, stage ->
            TimelineNode(label = stage, isCompleted = index < currentIndex, isActive = index == currentIndex, isLast = index == stages.size - 1, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TimelineNode(label: String, isCompleted: Boolean, isActive: Boolean, isLast: Boolean, modifier: Modifier) {
    val color = when {
        isCompleted -> TealGreen
        isActive -> PrimaryBlue
        else -> DarkBorder
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (!isLast) HorizontalDivider(modifier = Modifier.fillMaxWidth(0.5f).align(Alignment.CenterEnd), thickness = 2.dp, color = if (isCompleted) TealGreen else DarkBorder)
            if (label != "Accepted") HorizontalDivider(modifier = Modifier.fillMaxWidth(0.5f).align(Alignment.CenterStart), thickness = 2.dp, color = if (isCompleted || isActive) TealGreen else DarkBorder)
            
            Box(
                modifier = Modifier
                    .size(if (isActive) 16.dp else 10.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(if (isActive) Modifier.shadow(8.dp, CircleShape, spotColor = color) else Modifier)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = if (isActive) Color.White else TextSecondary, 
            textAlign = TextAlign.Center,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun DriverActions(shipment: Shipment, context: android.content.Context, onVerifyClick: () -> Unit, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (shipment.status) {
            "In Transit" -> {
                Button(onClick = { FirestoreManager.markArrivedAtDestination(shipment.id, shipment.vendorEmail) { Toast.makeText(context, "Reached Destination", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(16.dp)) { Text("I've Reached", fontWeight = FontWeight.Bold) }
            }
            "Arrived at Destination" -> {
                Button(onClick = onVerifyClick, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(16.dp)) { Text("Verify OTP", fontWeight = FontWeight.Bold) }
            }
            else -> {
                Button(onClick = {
                    val targetLat = if (shipment.status == "Accepted") shipment.pickupLat else shipment.destLat
                    val targetLng = if (shipment.status == "Accepted") shipment.pickupLng else shipment.destLng
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$targetLat,$targetLng")).apply { setPackage("com.google.android.apps.maps") })
                }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(8.dp)); Text("Navigate", fontWeight = FontWeight.Bold) }
            }
        }
        OutlinedButton(onClick = { FirestoreManager.cancelShipmentByDriver(shipment.id, shipment.driverEmail) { context.stopService(Intent(context, TrackingService::class.java)); onBack(); Toast.makeText(context, "Trip Cancelled", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) { Icon(Icons.Default.Close, null) }
    }
}

@Composable
fun VendorActions(shipment: Shipment, onVerifyClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = DarkCard, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = if (shipment.status == "Delivered") Icons.Default.CheckCircle else Icons.Default.Info
            val tint = if (shipment.status == "Delivered") SuccessGreen else InfoBlue
            val text = when(shipment.status) {
                "Delivered" -> "Goods Delivered Successfully!"
                "Accepted" -> "Driver is coming to pick up the goods."
                "In Transit" -> "Goods are on the way. share OTP at drop."
                else -> "Trip is active."
            }
            Icon(icon, null, tint = tint)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = if (shipment.status == "Delivered") SuccessGreen else TextSecondary, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(userRole: String, shipment: Shipment, targetName: String, onDismiss: () -> Unit, onSubmit: (Review) -> Unit) {
    var r1 by remember { mutableFloatStateOf(0f) }
    var r2 by remember { mutableFloatStateOf(0f) }
    var r3 by remember { mutableFloatStateOf(0f) }
    var feedback by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Rate Trip with $targetName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            RatingItem(label = if (userRole == "Vendor") "Driving Quality" else "Vendor Behavior", rating = r1, onRatingChange = { r1 = it })
            RatingItem(label = if (userRole == "Vendor") "Communication" else "Loading Experience", rating = r2, onRatingChange = { r2 = it })
            RatingItem(label = if (userRole == "Vendor") "Punctuality" else "Payment Experience", rating = r3, onRatingChange = { r3 = it })
            OutlinedTextField(value = feedback, onValueChange = { feedback = it }, placeholder = { Text("Add a comment...") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onSubmit(Review(shipmentId = shipment.id, reviewerEmail = if (userRole == "Vendor") shipment.vendorEmail else shipment.driverEmail, targetEmail = if (userRole == "Vendor") shipment.driverEmail else shipment.vendorEmail, rating = (r1+r2+r3)/3.0, feedback = feedback)) }, enabled = r1>0 && r2>0 && r3>0, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Submit") }
        }
    }
}

@Composable
fun RatingItem(label: String, rating: Float, onRatingChange: (Float) -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            (1..5).forEach { i ->
                Icon(if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (i <= rating) WarningOrange else TextDisabled, modifier = Modifier.size(36.dp).clickable { onRatingChange(i.toFloat()) })
            }
        }
    }
}

@Composable
fun SOSButton(onClick: () -> Unit) {
    val scale by rememberInfiniteTransition().animateFloat(1f, 1.2f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse))
    Surface(onClick = onClick, modifier = Modifier.size(64.dp).graphicsLayer(scaleX = scale, scaleY = scale), shape = CircleShape, color = ErrorRed, shadowElevation = 12.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, "SOS", tint = Color.White) }
    }
}

const val DarkMapStyle = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#121212" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#746855" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#242f3e" }] },
  { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
  { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
  { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#2c2c2c" }] },
  { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#212a37" }] },
  { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#9ca5b3" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#3c3c3c" }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#17263c" }] }
]
"""
