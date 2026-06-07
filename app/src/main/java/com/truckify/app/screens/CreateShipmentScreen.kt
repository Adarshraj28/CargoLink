package com.truckify.app.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AddCircle
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
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import com.truckify.app.ui.theme.*
import com.truckify.app.utils.NetworkUtils
import java.util.Locale

import com.truckify.app.firebase.GeminiManager
import kotlinx.coroutines.launch

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
    var pickupLat by rememberSaveable { mutableStateOf(0.0) }
    var pickupLng by rememberSaveable { mutableStateOf(0.0) }
    var destLat by rememberSaveable { mutableStateOf(0.0) }
    var destLng by rememberSaveable { mutableStateOf(0.0) }
    
    val stops = remember { mutableStateListOf<String>() }
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationResult by remember { mutableStateOf<String?>(null) }

    var selectingField by rememberSaveable { mutableStateOf("pickup") }

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
                        val geocoder = android.location.Geocoder(context, Locale.getDefault())
                        val address = try {
                            // Run geocoding in a background dispatcher
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                if (addresses != null && addresses.isNotEmpty()) {
                                    addresses[0].getAddressLine(0)
                                } else {
                                    "${String.format(Locale.getDefault(), "%.4f", latLng.latitude)}, ${String.format(Locale.getDefault(), "%.4f", latLng.longitude)}"
                                }
                            }
                        } catch (e: Exception) {
                            "${String.format(Locale.getDefault(), "%.4f", latLng.latitude)}, ${String.format(Locale.getDefault(), "%.4f", latLng.longitude)}"
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
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
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
                    
                    LocationField(
                        label = if (pickup.isEmpty()) "Select Pickup Location" else pickup,
                        isSelected = selectingField == "pickup",
                        icon = Icons.Default.LocationOn,
                        onClick = {
                            selectingField = "pickup"
                            val fields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)
                            pickupLauncher.launch(Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(context))
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LocationField(
                        label = if (destination.isEmpty()) "Select Destination Location" else destination,
                        isSelected = selectingField == "destination",
                        icon = Icons.Default.Flag,
                        onClick = {
                            selectingField = "destination"
                            val fields = listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)
                            destLauncher.launch(Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(context))
                        }
                    )

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

                    Spacer(modifier = Modifier.height(24.dp))
                    var loading by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (!NetworkUtils.isInternetAvailable(context)) {
                                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (pickupLat == 0.0 || destLat == 0.0) { Toast.makeText(context, "Please set both locations", Toast.LENGTH_SHORT).show(); return@Button }
                            loading = true
                            val shipment = Shipment(
                                vendorEmail = AuthManager.getCurrentUserEmail() ?: "",
                                pickupAddress = pickup,
                                destinationAddress = destination,
                                pickupLat = pickupLat, pickupLng = pickupLng,
                                destLat = destLat, destLng = destLng,
                                price = "₹${(20000..50000).random()}",
                                weight = "${(1..10).random()} Tons",
                                timestamp = System.currentTimeMillis()
                            )
                            FirestoreManager.postShipment(shipment, 
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
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
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
