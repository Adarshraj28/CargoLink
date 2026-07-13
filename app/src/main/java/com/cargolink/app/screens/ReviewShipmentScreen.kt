package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.*
import com.cargolink.app.viewmodel.ShipmentViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewShipmentScreen(
    onBack: () -> Unit,
    onEdit: (Int) -> Unit, // Step index to go back to
    onConfirm: (String) -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isPosting by viewModel.isPosting.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            Column(modifier = Modifier.background(Beige.copy(alpha = 0.1f))) {
                CenterAlignedTopAppBar(
                    title = { Text("Review Shipment", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkBlue)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Help */ }) {
                            Icon(Icons.Default.HelpOutline, null, tint = PrimaryBlue)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                StepProgressBar(currentStep = 7, totalSteps = 8)
            }
        },
        bottomBar = {
            ConfirmPayBottomBar(
                onConfirm = {
                    viewModel.postShipment(
                        onSuccess = { shipmentId ->
                            onConfirm(shipmentId)
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                isLoading = isPosting
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = "Final Review",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                text = "Verify all details before proceeding to payment.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // ROUTE SUMMARY CARD
            ReviewSection(title = "Route Details", onEdit = { onEdit(1) }) {
                RouteSummaryItem(
                    pickup = viewModel.pickupAddress,
                    drop = viewModel.dropAddress,
                    distance = "120 km",
                    duration = "3h 45m"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LOAD & VEHICLE SUMMARY
            ReviewSection(title = "Load & Vehicle", onEdit = { onEdit(3) }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(label = "Vehicle", value = viewModel.truckType, icon = Icons.Default.LocalShipping)
                    DetailItem(label = "Weight", value = "${viewModel.weightInput} Tons", icon = Icons.Default.Scale)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItem(label = "Category", value = viewModel.loadCategory, icon = Icons.Default.Category)
                    DetailItem(label = "Handling", value = if (viewModel.isFragile) "Fragile" else "General", icon = Icons.Default.LocalBar)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DRIVER SUMMARY
            ReviewSection(title = "Assigned Driver", onEdit = { onEdit(6) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).background(Beige.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF5D4037))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(viewModel.selectedDriverName.ifBlank { "Ramesh Kumar" }, fontWeight = FontWeight.Bold, color = DarkBlue)
                        Text("4.9 Rating • Verified Professional", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BILLING PREVIEW
            ReviewSection(title = "Billing Summary", onEdit = { onEdit(5) }) {
                CostRow("Base Freight", "₹3,200")
                CostRow("Surcharges & Taxes", "₹1,650")
                if (viewModel.isInsured) CostRow("Insurance", "₹280", isAccent = true)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Payable", fontWeight = FontWeight.Bold, color = DarkBlue)
                    Text(viewModel.totalPrice, fontWeight = FontWeight.ExtraBold, color = PrimaryBlue, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TERMS & CONDITIONS (Minimal Apple Style)
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 100.dp)) {
                Checkbox(checked = true, onCheckedChange = {}, colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "I agree to the Cargolink Terms of Service and understand the insurance coverage policies.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ReviewSection(title: String, onEdit: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun RouteSummaryItem(pickup: String, drop: String, distance: String, duration: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(PrimaryBlue, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text(pickup, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DarkBlue, maxLines = 1)
        }
        Box(modifier = Modifier.padding(start = 3.5.dp).width(1.dp).height(20.dp).background(Color.LightGray))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).border(2.dp, SuccessGreen, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text(drop, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DarkBlue, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(color = Background, shape = RoundedCornerShape(12.dp)) {
            Text(
                "$distance • $duration", 
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
        }
    }
}

@Composable
fun CostRow(label: String, value: String, isAccent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isAccent) PrimaryBlue else Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, color = DarkBlue, fontSize = 13.sp)
    }
}

@Composable
fun ConfirmPayBottomBar(onConfirm: () -> Unit, isLoading: Boolean = false) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(32.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = PrimaryBlue.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(PremiumGradient),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm & Secure Pay", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Shield, null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
