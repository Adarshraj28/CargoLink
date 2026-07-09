package com.cargolink.app.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
    
    // Check if we need to show rating dialog
    LaunchedEffect(shipment?.status) {
        if (shipment?.status == "Delivered") {
            val alreadyRated = if (userRole == "Vendor") shipment?.vendorRatedDriver == true else shipment?.driverRatedVendor == true
            if (!alreadyRated) {
                delay(1000) // Small delay for better UX
                showRatingDialog = true
            }
        }
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5f)
    }

    // 1. REAL-TIME DATA LISTENING
    DisposableEffect(shipmentId) {
        if (shipmentId.isEmpty()) return@DisposableEffect onDispose {}
        
        val registration = FirestoreManager.listenToShipment(shipmentId) { updatedShipment ->
            // If load becomes available again (e.g. cancelled by driver), redirect vendor
            if (userRole == "Vendor" && updatedShipment.status == "Available") {
                onAvailableRedirect(shipmentId)
            }
            coroutineScope.launch {
                val target = when {
                    updatedShipment.currentLat != 0.0 -> LatLng(updatedShipment.currentLat, updatedShipment.currentLng)
                    updatedShipment.status == "Accepted" || updatedShipment.status == "Arrived" -> LatLng(updatedShipment.pickupLat, updatedShipment.pickupLng)
                    else -> LatLng(updatedShipment.destLat, updatedShipment.destLng)
                }
                
                // If it's the first time, show both driver and destination if possible
                if (shipment == null && updatedShipment.currentLat != 0.0) {
                    val bounds = LatLngBounds.Builder()
                        .include(LatLng(updatedShipment.currentLat, updatedShipment.currentLng))
                        .include(LatLng(updatedShipment.destLat, updatedShipment.destLng))
                        .build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } else {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 14f))
                }
            }
            shipment = updatedShipment
        }
        
        // Listen for OTP if Vendor
        var otpReg: com.google.firebase.firestore.ListenerRegistration? = null
        if (userRole == "Vendor") {
            otpReg = FirestoreManager.getShipmentOtp(shipmentId) { otp ->
                deliveryOtp = otp
            }
        }
        
        onDispose {
            registration?.remove()
            otpReg?.remove()
        }
    }

    // 2. DRIVER LOCATION UPDATES
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

    // 3. FETCH DRIVER DATA
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
        topBar = {
            TrackingTopBar(onBack, userRole, context)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // MAP LAYER
            MapLayer(shipment, userRole, cameraPositionState)

            // OVERLAYS (SOS, RECENTER)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (userRole == "Driver") {
                    SOSButton {
                        shipment?.let { s ->
                            FirestoreManager.sendSOSAlert(s.id, s.driverEmail)
                            Toast.makeText(context, "SOS Alert Sent to Team & Family", Toast.LENGTH_LONG).show()
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
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(24.dp))
                }
            }

            // BOTTOM SHEET (RAPIDO STYLE)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 0.dp) // Professional flush look
            ) {
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
                            FirestoreManager.submitDriverReview(review) {
                                showRatingDialog = false
                                Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            FirestoreManager.submitVendorReview(review) {
                                showRatingDialog = false
                                Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
                            }
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
                    text = if (userRole == "Driver") "You're on the move" else "Your shipment is safe",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Track My CargoLink Trip")
                        putExtra(Intent.EXTRA_TEXT, "I'm on a delivery! Track me live: [CargoLink Link]")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Trip"))
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
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
                title = "Pickup Location",
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
                    title = "Current Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                )
                
                // Route drawing
                Polyline(
                    points = if (s.status == "In Transit" || s.status == "Arrived at Destination") {
                        listOf(driver, dest)
                    } else {
                        listOf(driver, pickup, dest)
                    },
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
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .clip(CircleShape)
                    .background(DarkBorder)
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            if (shipment == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryBlue)
            } else {
                // Header: Driver / Status Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userRole == "Vendor") (driverData?.get("name") as? String ?: "Finding Driver...") else "Your Active Trip",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                            Text(
                                text = " ${(driverData?.get("rating") as? Number)?.toDouble() ?: 4.8} • RJ14 GA 2024",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    // Contact Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(icon = Icons.Default.Call, color = SuccessGreen) {
                            val phone = driverData?.get("phone") as? String ?: "9999999999"
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                        ActionButton(icon = Icons.AutoMirrored.Filled.Chat, color = PrimaryBlue, onClick = onChatClick)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distance = if (shipment.currentLat != 0.0) {
                        calculateDistance(shipment.currentLat, shipment.currentLng, shipment.destLat, shipment.destLng)
                    } else {
                        calculateDistance(shipment.pickupLat, shipment.pickupLng, shipment.destLat, shipment.destLng)
                    }
                    val etaMinutes = (distance / 40.0 * 60).toInt() // Assume 40km/h avg
                    
                    TripStat("ETA", "${etaMinutes.coerceAtLeast(1)} mins", Icons.Default.AccessTime)
                    TripStat("Distance", "${String.format(java.util.Locale.getDefault(), "%.1f", distance)} km", Icons.Default.Route)
                    
                    if (userRole == "Vendor" && shipment.status == "In Transit") {
                        TripStat("OTP", deliveryOtp ?: "----", Icons.Default.VpnKey)
                    } else {
                        TripStat("Price", shipment.price, Icons.Default.Payments)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Timeline
                ProfessionalTimeline(shipment.status)

                if (shipment.isInsured) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SuccessGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("This shipment is insured up to ₹${shipment.declaredValue.toInt()}", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // CTA Buttons
                if (userRole == "Driver") {
                    DriverActions(shipment, context, onVerifyClick, onBack)
                } else if (userRole == "Vendor") {
                    VendorActions(shipment, onVerifyClick)
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun TripStat(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun ProfessionalTimeline(status: String) {
    val stages = listOf("Accepted", "Arrived", "In Transit", "Reached", "Delivered")
    val displayStatus = when(status) {
        "Arrived at Destination" -> "Reached"
        else -> status
    }
    val currentIndex = stages.indexOf(displayStatus).coerceAtLeast(0)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, stage ->
            TimelineNode(
                label = stage,
                isCompleted = index < currentIndex,
                isActive = index == currentIndex,
                isLast = index == stages.size - 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TimelineNode(label: String, isCompleted: Boolean, isActive: Boolean, isLast: Boolean, modifier: Modifier) {
    val color = when {
        isCompleted -> SuccessGreen
        isActive -> PrimaryBlue
        else -> DarkBorder
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            // Horizontal Line
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterEnd),
                    thickness = 2.dp,
                    color = if (isCompleted) SuccessGreen else DarkBorder
                )
            }
            if (label != "Accepted") {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterStart),
                    thickness = 2.dp,
                    color = if (isCompleted || isActive) SuccessGreen else DarkBorder
                )
            }

            // Dot
            Box(
                modifier = Modifier
                    .size(if (isActive) 16.dp else 10.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(if (isActive) Modifier.shadow(8.dp, CircleShape, spotColor = PrimaryBlue) else Modifier)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Color.White else TextSecondary,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DriverActions(shipment: Shipment, context: android.content.Context, onVerifyClick: () -> Unit, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (shipment.status) {
            "In Transit" -> {
                Button(
                    onClick = {
                        FirestoreManager.markArrivedAtDestination(shipment.id, shipment.vendorEmail) {
                            Toast.makeText(context, "Marked as Reached Destination", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("I've Reached", fontWeight = FontWeight.Bold)
                }
            }
            "Arrived at Destination" -> {
                Button(
                    onClick = onVerifyClick, 
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Verify Delivery OTP", fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Button(
                    onClick = {
                        val targetLat = if (shipment.status == "Accepted" || shipment.status == "Arrived") shipment.pickupLat else shipment.destLat
                        val targetLng = if (shipment.status == "Accepted" || shipment.status == "Arrived") shipment.pickupLng else shipment.destLng
                        val gmmIntentUri = Uri.parse("google.navigation:q=$targetLat,$targetLng")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply { setPackage("com.google.android.apps.maps") }
                        context.startActivity(mapIntent)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        OutlinedButton(
            onClick = {
                FirestoreManager.cancelShipmentByDriver(shipment.id, shipment.driverEmail) {
                    // Stop Background Tracking Service on cancellation
                    context.stopService(Intent(context, TrackingService::class.java))
                    onBack()
                    Toast.makeText(context, "Trip Cancelled", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
        }
    }
}

@Composable
fun VendorActions(shipment: Shipment, onVerifyClick: () -> Unit) {
    if (shipment.status == "Delivered") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SuccessGreen.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Shipment Delivered Successfully!", color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    } else if (shipment.status == "Arrived at Destination" || shipment.status == "In Transit") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Provide the Delivery OTP to the driver once goods are received.", color = TextSecondary, fontSize = 13.sp)
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = InfoBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Waiting for driver to reach pickup location.", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(
    userRole: String,
    shipment: Shipment,
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (Review) -> Unit
) {
    var rating1 by remember { mutableFloatStateOf(0f) }
    var rating2 by remember { mutableFloatStateOf(0f) }
    var rating3 by remember { mutableFloatStateOf(0f) }
    var feedback by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Rate your experience",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "How was your trip with $targetName?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (userRole == "Vendor") {
                    RatingItem(label = "Driving Quality", rating = rating1, onRatingChange = { rating1 = it })
                    RatingItem(label = "Communication", rating = rating2, onRatingChange = { rating2 = it })
                    RatingItem(label = "On-time Delivery", rating = rating3, onRatingChange = { rating3 = it })
                } else {
                    RatingItem(label = "Vendor Behavior", rating = rating1, onRatingChange = { rating1 = it })
                    RatingItem(label = "Loading Experience", rating = rating2, onRatingChange = { rating2 = it })
                    RatingItem(label = "Payment Experience", rating = rating3, onRatingChange = { rating3 = it })
                }
                
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    placeholder = { Text("Additional feedback (optional)", color = TextDisabled) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PrimaryBlue,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Button(
                onClick = {
                    val avg = (rating1 + rating2 + rating3) / 3.0
                    val review = Review(
                        shipmentId = shipment.id,
                        reviewerEmail = if (userRole == "Vendor") shipment.vendorEmail else shipment.driverEmail,
                        targetEmail = if (userRole == "Vendor") shipment.driverEmail else shipment.vendorEmail,
                        rating = avg,
                        feedback = feedback,
                        drivingRating = if (userRole == "Vendor") rating1 else 0f,
                        communicationRating = if (userRole == "Vendor") rating2 else 0f,
                        onTimeRating = if (userRole == "Vendor") rating3 else 0f,
                        behaviorRating = if (userRole == "Driver") rating1 else 0f,
                        loadingExpRating = if (userRole == "Driver") rating2 else 0f,
                        paymentExpRating = if (userRole == "Driver") rating3 else 0f
                    )
                    onSubmit(review)
                },
                enabled = rating1 > 0 && rating2 > 0 && rating3 > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Submit Review", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun RatingItem(label: String, rating: Float, onRatingChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            (1..5).forEach { index ->
                Icon(
                    imageVector = if (index <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (index <= rating) WarningOrange else TextDisabled,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onRatingChange(index.toFloat()) }
                        .padding(2.dp)
                )
            }
        }
    }
}
@Composable
fun SOSButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.size(64.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        shape = CircleShape,
        color = ErrorRed,
        shadowElevation = 12.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(28.dp))
        }
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
