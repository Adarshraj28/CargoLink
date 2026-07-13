package com.cargolink.app.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import com.cargolink.app.utils.getFriendlyAddress
import com.cargolink.app.utils.NetworkUtils
import com.cargolink.app.utils.PriceEstimator
import com.cargolink.app.utils.calculateDistance
import com.cargolink.app.components.*
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShipmentScreen(onBack: () -> Unit, onPostSuccess: (String) -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Core State
    var pickupAddress by rememberSaveable { mutableStateOf("") }
    var pickupLat by rememberSaveable { mutableStateOf(0.0) }
    var pickupLng by rememberSaveable { mutableStateOf(0.0) }
    var dropAddress by rememberSaveable { mutableStateOf("") }
    var dropLat by rememberSaveable { mutableStateOf(0.0) }
    var dropLng by rememberSaveable { mutableStateOf(0.0) }
    
    var truckType by remember { mutableStateOf("Mini Truck") }
    var weightInput by remember { mutableStateOf("1") }
    var isInsured by remember { mutableStateOf(false) }
    
    var selectingField by rememberSaveable { mutableStateOf("pickup") }
    val locationsSelected = pickupAddress.isNotEmpty() && dropAddress.isNotEmpty()

    // Price Calculation
    val estimation = remember(pickupLat, pickupLng, dropLat, dropLng, truckType, weightInput, isInsured) {
        if (pickupLat != 0.0 && dropLat != 0.0) {
            val dist = calculateDistance(pickupLat, pickupLng, dropLat, dropLng)
            val weight = weightInput.toDoubleOrNull() ?: 1.0
            val baseResult = PriceEstimator.estimatePrice(dist, weight, truckType)
            val insurance = if (isInsured) PriceEstimator.calculateInsurancePremium(10000.0) else 0.0
            baseResult.copy(totalPrice = baseResult.totalPrice + insurance, insurancePremium = insurance)
        } else null
    }

    // Auto-fetch current location for pickup
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && pickupAddress.isEmpty()) {
                    pickupLat = location.latitude
                    pickupLng = location.longitude
                    coroutineScope.launch {
                        val friendly = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            getFriendlyAddress(context, location.latitude, location.longitude)
                        }
                        pickupAddress = friendly
                        if (dropAddress.isEmpty()) selectingField = "drop"
                    }
                }
            }
        }
    }

    // Google Places Launchers
    val pickupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                val place = Autocomplete.getPlaceFromIntent(it)
                pickupAddress = place.address ?: place.name ?: ""
                place.latLng?.let { latLng ->
                    pickupLat = latLng.latitude; pickupLng = latLng.longitude
                    if (dropAddress.isEmpty()) selectingField = "drop"
                }
            }
        }
    }
    val dropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                val place = Autocomplete.getPlaceFromIntent(it)
                dropAddress = place.address ?: place.name ?: ""
                place.latLng?.let { latLng ->
                    dropLat = latLng.latitude; dropLng = latLng.longitude
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(28.6139, 77.2090), 12f) }

    // Map sync
    LaunchedEffect(pickupLat, pickupLng, dropLat, dropLng) {
        if (pickupLat != 0.0 && dropLat != 0.0) {
            val bounds = LatLngBounds.Builder()
                .include(LatLng(pickupLat, pickupLng))
                .include(LatLng(dropLat, dropLng))
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200))
        } else if (pickupLat != 0.0 && selectingField == "pickup") {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(pickupLat, pickupLng), 15f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deliver Goods", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    coroutineScope.launch {
                        val address = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            getFriendlyAddress(context, latLng.latitude, latLng.longitude)
                        }
                        if (selectingField == "pickup") {
                            pickupLat = latLng.latitude
                            pickupLng = latLng.longitude
                            pickupAddress = address
                            if (dropAddress.isEmpty()) selectingField = "drop"
                        } else {
                            dropLat = latLng.latitude
                            dropLng = latLng.longitude
                            dropAddress = address
                        }
                    }
                }
            ) {
                if (pickupLat != 0.0) Marker(state = MarkerState(LatLng(pickupLat, pickupLng)), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                if (dropLat != 0.0) Marker(state = MarkerState(LatLng(dropLat, dropLng)), title = "Drop Location")
            }

            // RAPIDO STYLE SELECTION CARD
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                    if (!locationsSelected) {
                        // PHASE 1: LOCATION SELECTION (PRIORITY 1)
                        Text("Pick-up & Drop Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DarkBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LocationField(
                            label = if (pickupAddress.isEmpty()) "Set Pickup Location" else pickupAddress,
                            isSelected = selectingField == "pickup",
                            icon = Icons.Default.MyLocation,
                            onClick = {
                                selectingField = "pickup"
                                pickupLauncher.launch(Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)).build(context))
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LocationField(
                            label = if (dropAddress.isEmpty()) "Set Drop Location" else dropAddress,
                            isSelected = selectingField == "drop",
                            icon = Icons.Default.LocationOn,
                            onClick = {
                                selectingField = "drop"
                                dropLauncher.launch(Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)).build(context))
                            }
                        )
                    } else {
                        // PHASE 2: VEHICLE BAR (RAPIDO/OLA STYLE)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Choose Vehicle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { pickupAddress = ""; dropAddress = ""; pickupLat = 0.0; dropLat = 0.0 }) {
                                Text("Change Locations", fontSize = 12.sp, color = PrimaryBlue)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        val vehicles = listOf(
                            Triple("Mini Truck", "Up to 1.5T", Icons.Default.LocalShipping),
                            Triple("Pick-up", "Up to 3T", Icons.Default.LocalShipping),
                            Triple("Mid-size", "Up to 7T", Icons.Default.LocalShipping),
                            Triple("Heavy Duty", "7T+", Icons.Default.LocalShipping)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vehicles.forEach { (type, desc, icon) ->
                                val isSelected = truckType == type
                                val price = PriceEstimator.estimatePrice(calculateDistance(pickupLat, pickupLng, dropLat, dropLng), weightInput.toDoubleOrNull() ?: 1.0, type).totalPrice

                                Surface(
                                    onClick = { truckType = type },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent,
                                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, null, tint = if (isSelected) PrimaryBlue else Color.Gray, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(type, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else Color.Black)
                                            Text(desc, fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Text("₹${price.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DarkBlue)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Goods Weight (Tons)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { if (it.length <= 2) weightInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        var loading by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (NetworkUtils.isInternetAvailable(context)) {
                                    loading = true
                                    val shipment = Shipment(
                                        vendorEmail = AuthManager.getCurrentUserEmail() ?: "",
                                        pickupAddress = pickupAddress, destinationAddress = dropAddress,
                                        pickupLat = pickupLat, pickupLng = pickupLng,
                                        destLat = dropLat, destLng = dropLng,
                                        price = "₹${estimation?.totalPrice?.toInt() ?: 0}",
                                        weight = "$weightInput Tons",
                                        truckType = truckType,
                                        timestamp = System.currentTimeMillis(),
                                        status = "Available"
                                    )
                                    FirestoreManager.postShipment(shipment, 
                                        onSuccess = { loading = false; onPostSuccess(it) },
                                        onError = { loading = false; Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Book $truckType", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationField(label: String, isSelected: Boolean, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.05f) else Background)
            .border(1.dp, if (isSelected) PrimaryBlue.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isSelected) PrimaryBlue else Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = if (label.startsWith("Set") || label.startsWith("Select")) Color.Gray else DarkBlue, maxLines = 1, fontWeight = FontWeight.Medium)
    }
}
