package com.cargolink.app.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import java.util.Locale
import com.cargolink.app.utils.getFriendlyAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext

@Composable
fun LocationText(
    lat: Double,
    lng: Double,
    defaultAddress: String = "",
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = 1
) {
    val context = LocalContext.current
    var address by remember(lat, lng, defaultAddress) { 
        val initial = if (defaultAddress.isNotEmpty() && !defaultAddress.any { it.isDigit() }) {
            defaultAddress.split(",").first()
        } else {
            "Loading..."
        }
        mutableStateOf(initial)
    }

    LaunchedEffect(lat, lng, defaultAddress) {
        if (lat != 0.0 && lng != 0.0) {
            val friendly = withContext(Dispatchers.IO) {
                getFriendlyAddress(context, lat, lng)
            }
            address = friendly
        } else if (defaultAddress.isNotEmpty()) {
            address = defaultAddress.split(",").first()
        }
    }

    Text(
        text = address,
        style = style,
        color = color,
        fontFamily = Inter,
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}

@Composable
fun ActiveShipmentCard(
    shipment: Shipment = Shipment(),
    onTrackClick: (String) -> Unit = {},
    onQrClick: (String) -> Unit = {},
    onRepostClick: (Shipment) -> Unit = {},
    userRole: String = "Vendor"
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick(shipment.id) },
        shape = MaterialTheme.shapes.medium,
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlue.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when(shipment.status) {
                                "Available" -> Icons.Default.Search
                                "In Transit" -> Icons.Default.LocalShipping
                                "Delivered" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Inventory
                            },
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LocationText(
                            lat = shipment.pickupLat,
                            lng = shipment.pickupLng,
                            defaultAddress = shipment.pickupAddress,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            " → ", 
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        LocationText(
                            lat = shipment.destLat,
                            lng = shipment.destLng,
                            defaultAddress = shipment.destinationAddress,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Shipment #${if (shipment.id.isEmpty()) "TRK902" else shipment.id.takeLast(4).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                
                Text(
                    text = shipment.price.ifEmpty { "₹0" },
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(shipment.status)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (shipment.status == "In Transit") {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "On Track", 
                            color = WarningOrange, 
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    if (shipment.status != "Delivered") {
                        IconButton(
                            onClick = { onQrClick(shipment.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (userRole == "Driver") Icons.Default.QrCodeScanner else Icons.Default.QrCode,
                                contentDescription = "QR Action",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (userRole == "Vendor" && (shipment.status == "Delivered" || shipment.status == "Available")) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onRepostClick(shipment) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-post Load", color = PrimaryBlue, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "Available" -> WarningOrange.copy(alpha = 0.1f) to WarningOrange
        "In Transit" -> PrimaryBlue.copy(alpha = 0.1f) to PrimaryBlue
        "Delivered" -> SuccessGreen.copy(alpha = 0.1f) to SuccessGreen
        else -> TextSecondary.copy(alpha = 0.1f) to TextSecondary
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status, 
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor, 
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DriverLoadCard(shipment: Shipment = Shipment(), onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LocationText(
                            lat = shipment.pickupLat,
                            lng = shipment.pickupLng,
                            defaultAddress = shipment.pickupAddress,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            " → ", 
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextSecondary
                        )
                        LocationText(
                            lat = shipment.destLat,
                            lng = shipment.destLng,
                            defaultAddress = shipment.destinationAddress,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Weight: ${shipment.weight.ifEmpty { "N/A" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Text(
                    text = shipment.price,
                    style = MaterialTheme.typography.displayMedium,
                    color = PrimaryBlue
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoChip("FastPay")
                InfoChip("Verified")
                InfoChip("Insurance")
            }
            Spacer(modifier = Modifier.height(24.dp))
            CargoLinkButton(
                text = "Review Details",
                onClick = onClick
            )
        }
    }
}
