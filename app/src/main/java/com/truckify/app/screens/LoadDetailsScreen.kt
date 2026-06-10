package com.truckify.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.components.InfoChip
import com.truckify.app.components.LocationText
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager
import com.truckify.app.models.Shipment
import com.truckify.app.ui.theme.Background
import com.truckify.app.ui.theme.DarkBlue
import com.truckify.app.ui.theme.LightBlue
import com.truckify.app.ui.theme.Beige
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDetailsScreen(shipmentId: String, onBack: () -> Unit, onAccepted: () -> Unit) {
    val context = LocalContext.current
    var shipment by remember { mutableStateOf<Shipment?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(shipmentId) {
        FirestoreManager.listenToShipment(shipmentId) {
            shipment = it
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Load Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DarkBlue)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Background)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LightBlue)
            }
        } else if (shipment == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Load not found")
            }
        } else {
            val s = shipment!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Price Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, LightBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Payout Amount", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(s.price, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp)
                    }
                }

                // Route Info
                Card(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        RoutePoint(Icons.Default.RadioButtonChecked, "Pickup", s.pickupAddress, s.pickupLat, s.pickupLng, LightBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(start = 11.dp).width(2.dp).height(30.dp).background(Color.LightGray))
                        Spacer(modifier = Modifier.height(8.dp))
                        RoutePoint(Icons.Default.LocationOn, "Destination", s.destinationAddress, s.destLat, s.destLng, Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Load Specs
                Text(text = "Shipment Details", modifier = Modifier.padding(horizontal = 24.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip("Weight: ${s.weight}")
                    InfoChip("Verified Load")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Vendor Info
                Card(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(50.dp).clip(CircleShape).background(Beige.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(s.vendorEmail.split("@").first().replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Top-Rated Vendor", color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Text(" 4.9", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(40.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.LightGray)
                    ) {
                        Text("Decline", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            AuthManager.getCurrentUserEmail()?.let { email ->
                                FirestoreManager.acceptShipment(s.id, email, 
                                    onSuccess = {
                                        Toast.makeText(context, "Load Accepted!", Toast.LENGTH_SHORT).show()
                                        onAccepted()
                                    },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                        modifier = Modifier.weight(2f).height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Accept This Load", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RoutePoint(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, address: String, lat: Double, lng: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            LocationText(
                lat = lat,
                lng = lng,
                defaultAddress = address,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DarkBlue
            )
        }
    }
}
