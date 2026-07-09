package com.cargolink.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cargolink.app.components.CargoLinkTopAppBar
import com.cargolink.app.components.LocationText
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.LightBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, userRole: String) {
    var history by remember { mutableStateOf<List<Shipment>>(emptyList()) }
    DisposableEffect(Unit) {
        val email = AuthManager.getCurrentUserEmail() ?: ""
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        if (email.isNotEmpty()) {
            registration = FirestoreManager.getShipmentHistory(email, userRole) { history = it }
        }
        onDispose { registration?.remove() }
    }

    Scaffold(
        topBar = {
            CargoLinkTopAppBar(
                title = "Order History",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(history) { shipment ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Shipment #${shipment.id.takeLast(4).uppercase()}", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            LocationText(
                                lat = shipment.pickupLat,
                                lng = shipment.pickupLng,
                                defaultAddress = shipment.pickupAddress,
                                color = Color.Gray
                            )
                            Text(" → ", color = Color.Gray)
                            LocationText(
                                lat = shipment.destLat,
                                lng = shipment.destLng,
                                defaultAddress = shipment.destinationAddress,
                                color = Color.Gray
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(shipment.status, color = if(shipment.status == "Delivered") Color(0xFF2E7D32) else LightBlue, fontWeight = FontWeight.Bold)
                            Text(shipment.price, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
