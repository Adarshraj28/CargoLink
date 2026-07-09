package com.cargolink.app.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.LocationText
import com.cargolink.app.utils.calculateDistance
import com.cargolink.app.viewmodel.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindLoadScreen(onBack: () -> Unit, viewModel: DriverViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val loads by viewModel.availableLoads.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var driverLocation by remember { mutableStateOf<LatLng?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { driverLocation = LatLng(it.latitude, it.longitude) }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { driverLocation = LatLng(it.latitude, it.longitude) }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        viewModel.loadDashboardData()
    }

    val nearbyLoads = remember(loads, driverLocation) {
        loads.map { shipment ->
            val dist = driverLocation?.let {
                calculateDistance(
                    it.latitude, it.longitude,
                    shipment.pickupLat, shipment.pickupLng
                )
            }
            
            // Priority Scoring: Lower is Better
            // 1. Distance (km)
            // 2. Age (hours) - older loads get higher (worse) score
            val ageHours = (System.currentTimeMillis() - shipment.timestamp) / (1000.0 * 60 * 60)
            val priorityScore = (dist ?: 50.0) + (ageHours * 5) // 1 hour age is like being 5km further away
            
            Triple(shipment, dist, priorityScore)
        }.sortedBy { it.third }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Loads Nearby", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryBlue)
            } else if (nearbyLoads.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No loads found in your area", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(nearbyLoads) { (shipment, distance, _) ->
                        LoadItemCard(
                            shipment = shipment,
                            distance = distance,
                            onAccept = {
                                viewModel.acceptLoad(shipment.id,
                                    onSuccess = { 
                                        Toast.makeText(context, "Load Accepted!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadItemCard(shipment: Shipment, distance: Double?, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = shipment.price,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                if (distance != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        Text(text = String.format(" %.1f km away", distance), color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryBlue))
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(ErrorRed))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    LocationText(
                        lat = shipment.pickupLat,
                        lng = shipment.pickupLng,
                        defaultAddress = shipment.pickupAddress,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(text = "Pickup Location", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    LocationText(
                        lat = shipment.destLat,
                        lng = shipment.destLng,
                        defaultAddress = shipment.destinationAddress,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(text = "Destination Location", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = shipment.weight, color = Color.Gray, fontSize = 14.sp)
                }
                
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Accept Load", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
