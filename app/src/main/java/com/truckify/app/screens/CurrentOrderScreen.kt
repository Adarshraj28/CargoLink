package com.truckify.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckify.app.components.ActiveShipmentCard
import com.truckify.app.navigation.Screen
import com.truckify.app.viewmodel.OrdersViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentOrderScreen(
    onBack: () -> Unit,
    userRole: String,
    onTrackClick: (String) -> Unit,
    onQrClick: (String, String) -> Unit, // Added status
    viewModel: OrdersViewModel = viewModel()
) {
    val activeShipments by viewModel.activeShipments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(userRole) {
        viewModel.observeOrders(userRole)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Current Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = com.truckify.app.ui.theme.DarkBlue)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (activeShipments.isEmpty()) {
                        item { Text("No active orders found.", color = Color.Gray) }
                    } else {
                        items(activeShipments) { shipment ->
                            ActiveShipmentCard(
                                shipment = shipment,
                                onTrackClick = onTrackClick,
                                onQrClick = { id -> onQrClick(id, shipment.status) },
                                onRepostClick = { item ->
                                    viewModel.repostShipment(
                                        item,
                                        onSuccess = { android.widget.Toast.makeText(context, "Load Re-posted!", android.widget.Toast.LENGTH_SHORT).show() },
                                        onError = { android.widget.Toast.makeText(context, "Error: $it", android.widget.Toast.LENGTH_SHORT).show() }
                                    )
                                },
                                userRole = userRole
                            )
                        }
                    }
                }
            }
        }
    }
}
