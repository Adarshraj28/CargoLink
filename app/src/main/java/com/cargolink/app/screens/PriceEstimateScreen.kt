package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.*
import com.cargolink.app.viewmodel.ShipmentViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceEstimateScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    var showBreakdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        showBreakdown = true
    }

    Scaffold(
        containerColor = Beige.copy(alpha = 0.05f),
        topBar = {
            Column(modifier = Modifier.background(Beige.copy(alpha = 0.1f))) {
                CenterAlignedTopAppBar(
                    title = { Text("New Shipment", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                StepProgressBar(currentStep = 5, totalSteps = 8)
            }
        },
        bottomBar = {
            PriceSummaryCard(totalPrice = viewModel.totalPrice, onContinue = onContinue)
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
                text = "Price Estimation",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                text = "Detailed breakdown of your shipment cost.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // PRICE GRAPH (Apple/Stripe Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Market Trend", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxSize()) {
                        MarketTrendChart()
                        // Overlay price indicator
                        Surface(
                            color = PrimaryBlue,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = 20.dp)
                        ) {
                            Text(
                                "Optimal",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // AI CHEAPEST ROUTE BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SuccessGreen.copy(alpha = 0.1f))
                    .border(1.dp, SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "AI optimized route is saving you ₹420 today!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // COST BREAKDOWN (Stripe inspired)
            Text("Cost Breakdown", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val distValue = viewModel.estimatedDistance.replace(" km", "").toDoubleOrNull() ?: 120.0
                    val distCost = (distValue * 25).toInt()
                    
                    CostItem("Distance Cost (${viewModel.estimatedDistance})", "₹$distCost")
                    CostItem("Fuel Surcharge", "₹450")
                    CostItem("Driver Fee", "₹800")
                    CostItem("Platform Fee", "₹120")
                    CostItem("Insurance (Optional)", "₹280", isAccent = viewModel.isInsured)
                    
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    val subtotal = distCost + 450 + 800 + 120 + (if (viewModel.isInsured) 280 else 0) - 500
                    val gst = (subtotal * 0.18).toInt()
                    val total = subtotal + gst
                    
                    viewModel.totalPrice = "₹$total"

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GST (18%)", color = Color.Gray, fontSize = 14.sp)
                        Text("₹$gst", fontWeight = FontWeight.Medium, color = DarkBlue)
                    }

                    // DISCOUNT CODE (Apple style)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Beige.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Beige.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ConfirmationNumber, null, tint = Color(0xFF5D4037), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("FIRST500 applied", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), modifier = Modifier.weight(1f))
                            Text("-₹500", fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun CostItem(label: String, value: String, isAccent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isAccent) PrimaryBlue else Color.Gray, fontSize = 14.sp, fontWeight = if (isAccent) FontWeight.SemiBold else FontWeight.Normal)
        Text(value, fontWeight = FontWeight.SemiBold, color = DarkBlue)
    }
}

@Composable
fun MarketTrendChart() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.8f)
            cubicTo(
                size.width * 0.2f, size.height * 0.7f,
                size.width * 0.4f, size.height * 0.9f,
                size.width * 0.6f, size.height * 0.4f
            )
            cubicTo(
                size.width * 0.8f, size.height * 0.1f,
                size.width * 0.9f, size.height * 0.3f,
                size.width, size.height * 0.2f
            )
        }
        drawPath(
            path = path,
            color = PrimaryBlue.copy(alpha = 0.3f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Final point
        drawCircle(
            color = PrimaryBlue,
            radius = 6.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f)
        )
    }
}

@Composable
fun PriceSummaryCard(totalPrice: String, onContinue: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(32.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total Final Price", color = Color.Gray, fontSize = 12.sp)
                    Text(totalPrice, fontWeight = FontWeight.ExtraBold, color = DarkBlue, fontSize = 24.sp)
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.height(56.dp).width(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Match Driver", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
