package com.cargolink.app.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import com.cargolink.app.utils.getFriendlyAddress
import com.cargolink.app.utils.NetworkUtils
import java.util.Locale

import com.cargolink.app.firebase.GeminiManager
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.cargolink.app.utils.PriceEstimator
import com.cargolink.app.utils.calculateDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShipmentScreen(onBack: () -> Unit, onPostSuccess: (String) -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pickup by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    
    var predictionResult by remember { mutableStateOf<String?>(null) }
    var isPredicting by remember { mutableStateOf(false) }
    var truckType by remember { mutableStateOf("Heavy Duty") }
    var weightInput by remember { mutableStateOf("5") }
    var isInsured by remember { mutableStateOf(false) }
    var declaredValue by remember { mutableStateOf("") }
    var pickupLat by rememberSaveable { mutableStateOf(0.0) }
    var pickupLng by rememberSaveable { mutableStateOf(0.0) }
    var destLat by rememberSaveable { mutableStateOf(0.0) }
    var destLng by rememberSaveable { mutableStateOf(0.0) }
    
    val stops = remember { mutableStateListOf<String>() }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationResult by remember { mutableStateOf<String?>(null) }

    var selectingField by rememberSaveable { mutableStateOf("pickup") }

    val estimation = remember(pickupLat, pickupLng, destLat, destLng, truckType, weightInput, isInsured, declaredValue) {
        if (pickupLat != 0.0 && destLat != 0.0) {
            val dist = calculateDistance(pickupLat, pickupLng, destLat, destLng)
            val weight = weightInput.toDoubleOrNull() ?: 5.0
            val baseResult = PriceEstimator.estimatePrice(dist, weight, truckType)
            
            val insurance = if (isInsured) {
                PriceEstimator.calculateInsurancePremium(declaredValue.toDoubleOrNull() ?: 0.0)
            } else 0.0
            
            baseResult.copy(
                totalPrice = baseResult.totalPrice + insurance,
                insurancePremium = insurance
            )
        } else null
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && pickup.isEmpty()) {
                    pickupLat = location.latitude
                    pickupLng = location.longitude
                    coroutineScope.launch {
                        val friendly = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            getFriendlyAddress(context, location.latitude, location.longitude)
                        }
                        pickup = friendly
                    }
                }
            }
        }
    }

    val pickupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                val place = Autocomplete.getPlaceFromIntent(it)
                pickup = place.address ?: place.name ?: ""
                place.latLng?.let { latLng ->
                    pickupLat = latLng.latitude
                    pickupLng = latLng.longitude
                    if (destination.isEmpty()) selectingField = "destination"
                }
            }
        }
    }
    val destLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                val place = Autocomplete.getPlaceFromIntent(it)
                destination = place.address ?: place.name ?: ""
                place.latLng?.let { latLng ->
                    destLat = latLng.latitude
                    destLng = latLng.longitude
                }
            }
        }
    }

    val pickupLatLng = if (pickupLat != 0.0) LatLng(pickupLat, pickupLng) else null
    val destLatLng = if (destLat != 0.0) LatLng(destLat, destLng) else null
    
    val delhi = LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(delhi, 10f) }

    LaunchedEffect(pickupLatLng) { 
        if (pickupLatLng != null && selectingField == "pickup") {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f))
        }
    }
    LaunchedEffect(destLatLng) { 
        if (destLatLng != null && selectingField == "destination") {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(destLatLng, 14f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Your Route", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkBlue) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    coroutineScope.launch {
                        val address = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            getFriendlyAddress(context, latLng.latitude, latLng.longitude)
                        }

                        if (selectingField == "pickup") {
                            pickupLat = latLng.latitude
                            pickupLng = latLng.longitude
                            pickup = address
                            if (destination.isEmpty()) selectingField = "destination"
                        } else {
                            destLat = latLng.latitude
                            destLng = latLng.longitude
                            destination = address
                        }
                    }
                }
            ) {
                pickupLatLng?.let { Marker(state = MarkerState(position = it), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) }
                destLatLng?.let { Marker(state = MarkerState(position = it), title = "Destination") }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
                    .fillMaxWidth()
                    .heightIn(max = 500.dp), // Limit height so it doesn't cover whole map
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Plan Your Trip", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = DarkBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Select your mode of trip", color = Color.Gray)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Trip Mode Selector like Image 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Background)
                            .padding(4.dp)
                    ) {
                        var tripMode by remember { mutableStateOf("Single") }
                        Button(
                            onClick = { tripMode = "Single" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tripMode == "Single") DarkBlue else Color.Transparent,
                                contentColor = if (tripMode == "Single") Color.White else Color.Gray
                            )
                        ) { Text("Single Trip") }
                        Button(
                            onClick = { tripMode = "Multi" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tripMode == "Multi") DarkBlue else Color.Transparent,
                                contentColor = if (tripMode == "Multi") Color.White else Color.Gray
                            )
                        ) { Text("Multi Trip") }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Select Truck Type", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkBlue)
                    Spacer(modifier = Modifier.height(12.dp))

                    val truckTypes = listOf(
                        Triple("Mini Truck", "Up to 1.5T", Icons.Default.LocalShipping),
                        Triple("Pick-up", "Up to 3T", Icons.Default.LocalShipping),
                        Triple("Mid-size", "Up to 7T", Icons.Default.LocalShipping),
                        Triple("Heavy Duty", "7T+", Icons.Default.LocalShipping)
                    )

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(truckTypes.size) { index ->
                            val type = truckTypes[index].first
                            val capacity = truckTypes[index].second
                            val icon = truckTypes[index].third
                            val isSelected = truckType == type
                            val estimatedPrice = if (pickupLat != 0.0 && destLat != 0.0) {
                                val dist = calculateDistance(pickupLat, pickupLng, destLat, destLng)
                                PriceEstimator.estimatePrice(dist, weightInput.toDoubleOrNull() ?: 1.0, type).totalPrice
                            } else null

                            Card(
                                onClick = { truckType = type },
                                modifier = Modifier.width(120.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.White
                                ),
                                border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(icon, contentDescription = null, tint = if (isSelected) PrimaryBlue else Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(type, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) PrimaryBlue else Color.DarkGray)
                                    Text(capacity, fontSize = 10.sp, color = Color.Gray)
                                    if (estimatedPrice != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("₹${estimatedPrice.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DarkBlue)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (pickup.isNotEmpty() && destination.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Add Multi-Stop Waypoints", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var stopInput by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = stopInput,
                                onValueChange = { stopInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Enter stop address...") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(onClick = {
                                if (stopInput.isNotBlank()) {
                                    stops.add(stopInput)
                                    stopInput = ""
                                }
                            }) { Icon(Icons.Default.AddCircle, contentDescription = null, tint = PrimaryBlue) }
                        }
                        stops.forEach { stop ->
                            Text("• $stop", fontSize = 12.sp, color = TextGray)
                        }
                        
                        if (stops.isNotEmpty()) {
                            Button(
                                onClick = {
                                    isOptimizing = true
                                    coroutineScope.launch {
                                        optimizationResult = GeminiManager.getOptimizedRoute(pickup, stops, destination)
                                        isOptimizing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                if (isOptimizing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else Text("Optimize Multi-Stop Route")
                            }
                        }
                    }

                    if (optimizationResult != null) {
                        Card(
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Background)
                        ) {
                            Text(optimizationResult!!, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                        }
                    }

                    if (pickup.isNotEmpty() && destination.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Shipment Weight", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter Weight (Tons)") },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Rapido-style Insurance Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInsured) SuccessGreen.copy(alpha = 0.05f) else Background
                            ),
                            border = BorderStroke(1.dp, if (isInsured) SuccessGreen.copy(alpha = 0.3f) else Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (isInsured) SuccessGreen else Color.Gray)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Insurance Protection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Cover against damage or loss", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Switch(
                                        checked = isInsured,
                                        onCheckedChange = { isInsured = it },
                                        colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                                    )
                                }

                                if (isInsured) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = declaredValue,
                                        onValueChange = { declaredValue = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Declared Value of Goods (₹)") },
                                        placeholder = { Text("e.g. 50000") },
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        prefix = { Text("₹") }
                                    )
                                    if ((estimation?.insurancePremium ?: 0.0) > 0.0) {
                                        Text(
                                            "Premium: ₹${estimation?.insurancePremium?.toInt()} (Added to total)",
                                            modifier = Modifier.padding(top = 8.dp),
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isPredicting = true
                                coroutineScope.launch {
                                    predictionResult = GeminiManager.getCostPrediction(pickup, destination, truckType, "$weightInput Tons")
                                    isPredicting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(45.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LightBlue.copy(alpha = 0.2f), contentColor = LightBlue)
                        ) {
                            if (isPredicting) CircularProgressIndicator(color = LightBlue, modifier = Modifier.size(20.dp))
                            else Text("Get AI Cost Prediction", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (predictionResult != null) {
                        Card(
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI ESTIMATES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF388E3C))
                                }
                                Text(predictionResult!!, fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    if (estimation != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Fare", fontWeight = FontWeight.Bold, color = DarkBlue)
                                    Text("₹${String.format("%.0f", estimation.totalPrice)}", fontWeight = FontWeight.ExtraBold, color = DarkBlue, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Distance", fontSize = 12.sp, color = Color.Gray)
                                    Text("${String.format("%.1f", estimation.distance)} km", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Platform Fee (10%)", fontSize = 12.sp, color = Color.Gray)
                                    Text("₹${String.format("%.0f", estimation.commission)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (isInsured) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Insurance Premium", fontSize = 12.sp, color = Color.Gray)
                                        Text("₹${String.format("%.0f", estimation.insurancePremium)}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.1f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Driver Payout", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    Text("₹${String.format("%.0f", estimation.driverPayout)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    var loading by remember { mutableStateOf(false) }
                    val canPost = pickup.isNotEmpty() && destination.isNotEmpty() && pickupLat != 0.0 && destLat != 0.0 && !loading

                    Button(
                        onClick = {
                            if (!NetworkUtils.isInternetAvailable(context)) {
                                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!canPost) {
                                Toast.makeText(context, "Please complete all location details", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            loading = true

                            val finalShipment = Shipment(
                                vendorEmail = AuthManager.getCurrentUserEmail() ?: "",
                                pickupAddress = pickup,
                                destinationAddress = destination,
                                pickupLat = pickupLat, pickupLng = pickupLng,
                                destLat = destLat, destLng = destLng,
                                price = "₹${String.format(Locale.getDefault(), "%.0f", estimation?.totalPrice ?: 0.0)}",
                                commission = estimation?.commission ?: 0.0,
                                driverPayout = estimation?.driverPayout ?: 0.0,
                                weight = "$weightInput Tons",
                                truckType = truckType,
                                isInsured = isInsured,
                                insurancePremium = estimation?.insurancePremium ?: 0.0,
                                declaredValue = declaredValue.toDoubleOrNull() ?: 0.0,
                                timestamp = System.currentTimeMillis()
                            )

                            FirestoreManager.postShipment(finalShipment,
                                onSuccess = { id ->
                                    loading = false
                                    Toast.makeText(context, "Shipment Posted Successfully!", Toast.LENGTH_LONG).show()
                                    onPostSuccess(id)
                                },
                                onError = {
                                    loading = false
                                    Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        enabled = canPost,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canPost) DarkBlue else Color.Gray,
                            contentColor = Color.White
                        )
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text(text = "Confirm & Post Shipment", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationField(label: String, isSelected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) LightBlue.copy(alpha = 0.1f) else Background)
            .clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) LightBlue else Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = if (label.startsWith("Select")) Color.Gray else DarkBlue, maxLines = 1, fontWeight = FontWeight.SemiBold)
    }
}
