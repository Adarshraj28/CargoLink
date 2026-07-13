package com.cargolink.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cargolink.app.components.ActiveShipmentCard
import com.cargolink.app.viewmodel.OrdersViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cargolink.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentOrderScreen(
    onBack: () -> Unit,
    userRole: String,
    onTrackClick: (String) -> Unit,
    onQrClick: (String, String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val activeShipments by viewModel.activeShipments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userRole) {
        viewModel.observeOrders(userRole)
    }

    Scaffold(
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            TopAppBar(
                title = { Text("Active Shipments", fontWeight = FontWeight.ExtraBold, color = NavyDeep) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NavyDeep) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refresh()
                coroutineScope.launch {
                    delay(1200)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && !isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DarkBlue)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (activeShipments.isEmpty()) {
                            item { 
                                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No active orders found.", color = Color.Gray) 
                                }
                            }
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
}
