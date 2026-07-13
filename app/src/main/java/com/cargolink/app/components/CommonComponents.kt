package com.cargolink.app.components

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.cargolink.app.ui.theme.*
import com.cargolink.app.utils.getFriendlyAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoLinkTopAppBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back", 
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Beige.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon, 
                            contentDescription = null, 
                            tint = Color(0xFF5D4037),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                contentDescription = null, 
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CargoLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = PrimaryBlue.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (enabled) Modifier.background(BlueGradient) 
                else Modifier.background(Color.Gray.copy(alpha = 0.3f))
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CargoLinkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    visualTransformation: VisualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = PrimaryBlue) }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = PrimaryBlue,
            unfocusedLabelColor = TextSecondary,
            cursorColor = PrimaryBlue,
            focusedContainerColor = PrimaryBlue.copy(alpha = 0.02f),
            unfocusedContainerColor = Color.White,
            disabledBorderColor = Color.Transparent,
            disabledLabelColor = TextSecondary,
            disabledTextColor = TextPrimary
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = DarkBlue)
    )
}

@Composable
fun InfoChip(text: String, color: Color = Color(0xFF5D4037)) {
    Surface(
        color = Beige.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
    ) {
        Text(
            text = text, 
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Text(
            value, 
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun CargoLinkLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                ),
                shape = MaterialTheme.shapes.medium
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun RatingDialog(onDismiss: () -> Unit, onSubmit: (Int) -> Unit) {
    var rating by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSubmit(rating) },
                enabled = rating > 0
            ) {
                Text("Submit Rating", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = { 
            Text(
                "Rate the Driver", 
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "How was your experience with the driver?", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { index ->
                        IconButton(onClick = { rating = index }) {
                            Icon(
                                if (index <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index <= rating) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    background(brush)
}

fun Modifier.designerBackground(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshOffset"
    )

    val brush = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            Color.Transparent
        ),
        center = Offset(offset, offset),
        radius = 800f
    )

    this.background(MaterialTheme.colorScheme.background).background(brush)
}

@Composable
fun LocationText(
    lat: Double,
    lng: Double,
    defaultAddress: String = "",
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
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
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
}

const val GOOGLE_MAPS_DARK_STYLE = "[{\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#242f3e\"}]},{\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#746855\"}]},{\"elementType\":\"labels.text.stroke\",\"stylers\":[{\"color\":\"#242f3e\"}]},{\"featureType\":\"administrative.locality\",\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#d59563\"}]},{\"featureType\":\"road\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#38414e\"}]},{\"featureType\":\"road\",\"elementType\":\"geometry.stroke\",\"stylers\":[{\"color\":\"#212a37\"}]},{\"featureType\":\"water\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#17263c\"}]}]"

@Composable
fun rememberBiometricState(): MutableState<Boolean> {
    return remember { mutableStateOf(false) }
}

fun authenticateUser(
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val activity = context as? FragmentActivity
    if (activity != null && com.cargolink.app.utils.SecurityUtils.isBiometricAvailable(context)) {
        com.cargolink.app.utils.SecurityUtils.authenticate(
            activity = activity,
            onSuccess = onSuccess,
            onError = onError
        )
    } else {
        onSuccess() // Fallback if no biometric available
    }
}

@Composable
fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Beige.copy(alpha = 0.3f))
        ) {
            val progress = currentStep.toFloat() / totalSteps
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(PremiumGradient)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Progress: Step $currentStep of $totalSteps",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryBlue,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun AppleSelector(text: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Beige.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        color = SoftBeige,
        border = BorderStroke(1.5.dp, Beige.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = text, fontWeight = FontWeight.Bold, color = NavyDeep, fontSize = 15.sp)
            Icon(icon, null, tint = NavyDeep, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PremiumInput(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector) {
    Column {
        Text(
            label, 
            fontSize = 11.sp, 
            color = Color.Gray, 
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 2.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = DarkBlue)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = Beige.copy(alpha = 0.6f), thickness = 1.dp)
    }
}
