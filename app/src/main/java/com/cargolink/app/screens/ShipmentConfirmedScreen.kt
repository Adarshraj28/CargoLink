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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*

@Composable
fun ShipmentConfirmedScreen(
    onTrackShipment: () -> Unit,
    onReturnDashboard: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(modifier = Modifier.fillMaxSize().background(Beige.copy(alpha = 0.05f))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // SUCCESS ANIMATION PLACEHOLDER
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(tween(600, easing = CustomOvershootInterpolator().toEasing())) + fadeIn()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(160.dp).background(TealGreen.copy(alpha = 0.08f), CircleShape))
                    Box(modifier = Modifier.size(110.dp).background(SuccessGradient, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(60.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Shipment Confirmed!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue,
                textAlign = TextAlign.Center
            )
            Text(
                "Your load #CL-984210 is scheduled and a driver is on the way to your location.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // DRIVER & VEHICLE INFO CARD
            Card(
                modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Beige.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF5D4037), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ramesh Kumar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlue)
                            Text("Arriving in 12 mins", color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier.background(PrimaryBlue.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Call, null, tint = PrimaryBlue)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("VEHICLE", fontSize = 10.sp, color = Color.Gray)
                            Text("Pickup • DL 01 AB 1234", fontWeight = FontWeight.Bold, color = DarkBlue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PIN", fontSize = 10.sp, color = Color.Gray)
                            Text("4 8 5 2", fontWeight = FontWeight.ExtraBold, color = PrimaryBlue, letterSpacing = 4.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // QUICK ACTIONS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invoice", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onTrackShipment,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Track Live", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onReturnDashboard) {
                Text("Return to Dashboard", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Simple interpolator helper for Compose
class CustomOvershootInterpolator(private val tension: Float = 2f) {
    fun toEasing() = Easing { x ->
        val t = x - 1f
        t * t * ((tension + 1f) * t + tension) + 1f
    }
}
