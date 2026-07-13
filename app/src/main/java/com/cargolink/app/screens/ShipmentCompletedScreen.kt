package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*

@Composable
fun ShipmentCompletedScreen(
    onBookAnother: () -> Unit,
    onReturnDashboard: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            Surface(
                modifier = Modifier.shadow(32.dp),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = onBookAnother,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Book Another Shipment", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onReturnDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Dashboard", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // SUCCESS ANIMATION (DELIGHTFUL)
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(tween(800, easing = CustomOvershootInterpolator(3f).toEasing())) + fadeIn()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow
                    Box(modifier = Modifier.size(160.dp).background(SuccessGreen.copy(alpha = 0.1f), CircleShape))
                    // Inner trophy circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(16.dp, CircleShape)
                            .background(SuccessGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Celebration, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Shipment Delivered!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue,
                textAlign = TextAlign.Center
            )
            Text(
                "Cargo #CL-984210 was successfully handed over at 4:32 PM.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // TRIP SUMMARY CARDS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TripSummaryMetric("Total Time", "3h 12m", Icons.Default.Timer, modifier = Modifier.weight(1f))
                TripSummaryMetric("Distance", "124 km", Icons.Default.Route, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Final Cost", color = Color.Gray)
                        Text("₹4,850", fontWeight = FontWeight.ExtraBold, color = DarkBlue, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Invoice", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Download, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // RATE DRIVER (Premium Card)
            Text("Rate Your Experience", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Beige.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF5D4037))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Ramesh Kumar", fontWeight = FontWeight.Bold, color = DarkBlue)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = if (index < 4) WarningOrange else Color.LightGray.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp).clickable { }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Top professional. Highly recommended.", color = Color.Gray, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SHARE RECEIPT (Apple style link)
            Surface(
                modifier = Modifier.clickable { },
                color = Color.Transparent
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.IosShare, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Shipment Receipt", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun TripSummaryMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, color = DarkBlue, fontSize = 18.sp)
        }
    }
}
