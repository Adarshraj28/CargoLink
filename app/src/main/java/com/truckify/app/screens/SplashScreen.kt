package com.truckify.app.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckify.app.ui.theme.*

@Composable
fun SplashScreen() {
    val backgroundColor = MaterialTheme.colorScheme.background
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Subtle Pattern Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            for (i in 0..20) {
                drawLine(
                    color = outlineColor.copy(alpha = 0.1f),
                    start = Offset(0f, canvasHeight * i / 20),
                    end = Offset(canvasWidth, canvasHeight * i / 20),
                    strokeWidth = 1f
                )
                drawLine(
                    color = outlineColor.copy(alpha = 0.1f),
                    start = Offset(canvasWidth * i / 20, 0f),
                    end = Offset(canvasWidth * i / 20, canvasHeight),
                    strokeWidth = 1f
                )
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Logo and Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 80.dp)
            ) {
                TruckifyLogoIcon()

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TRUCK",
                        fontFamily = Sora,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "IFY",
                        fontFamily = Sora,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Beige,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tagline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "MOVE", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(text = " • ", color = onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(text = "CONNECT", color = onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(text = " • ", color = onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(text = "DELIVER", color = Beige, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }

            // Middle Section: Truck Visualization
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier
                        .size(280.dp)
                        .alpha(0.03f),
                    tint = primaryColor
                )
            }

            // Bottom Section: Features
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureItem(Icons.Outlined.Shield, "SECURE")
                FeatureItem(Icons.Outlined.Timer, "FAST")
                FeatureItem(Icons.Outlined.Payments, "BEST RATE")
                FeatureItem(Icons.Outlined.HeadsetMic, "SUPPORT")
            }
        }
    }
}

@Composable
fun TruckifyLogoIcon() {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            // Secondary T part (Beige)
            val pathBeige = Path().apply {
                moveTo(size.width * 0.75f, size.height * 0.15f)
                lineTo(size.width * 0.95f, size.height * 0.15f)
                lineTo(size.width * 0.75f, size.height * 0.75f)
                lineTo(size.width * 0.6f, size.height * 0.75f)
                close()
            }
            
            // Main Stylized T (Blue)
            val pathBlue = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.1f)
                lineTo(size.width * 0.85f, size.height * 0.1f)
                lineTo(size.width * 0.8f, size.height * 0.25f)
                lineTo(size.width * 0.55f, size.height * 0.25f)
                lineTo(size.width * 0.45f, size.height * 0.9f)
                lineTo(size.width * 0.25f, size.height * 0.9f)
                lineTo(size.width * 0.35f, size.height * 0.25f)
                lineTo(size.width * 0.1f, size.height * 0.25f)
                close()
            }

            drawPath(path = pathBeige, color = Beige)
            drawPath(path = pathBlue, color = PrimaryBlue)
            
            // Simple Truck Silhouette inside the logo area
            val truckPath = Path().apply {
                moveTo(size.width * 0.05f, size.height * 0.55f)
                lineTo(size.width * 0.35f, size.height * 0.55f)
                lineTo(size.width * 0.4f, size.height * 0.75f)
                lineTo(size.width * 0.05f, size.height * 0.75f)
                close()
            }
            drawPath(path = truckPath, color = PrimaryBlue)
            drawCircle(color = PrimaryBlue, radius = 6f, center = Offset(size.width * 0.12f, size.height * 0.8f))
            drawCircle(color = PrimaryBlue, radius = 6f, center = Offset(size.width * 0.33f, size.height * 0.8f))
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(TextPrimary.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PrimaryBlue
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            fontFamily = Inter,
            color = TextSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}
