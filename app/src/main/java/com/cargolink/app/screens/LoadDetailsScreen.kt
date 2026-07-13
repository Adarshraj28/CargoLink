package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.ui.theme.*
import com.cargolink.app.components.*
import com.cargolink.app.viewmodel.ShipmentViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDetailsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: ShipmentViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

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
                StepProgressBar(currentStep = 3, totalSteps = 8)
            }
        },
        bottomBar = {
            val context = androidx.compose.ui.platform.LocalContext.current
            LoadSummaryCard(weight = viewModel.weightInput, onContinue = {
                val error = viewModel.validateLoad()
                if (error == null) onContinue()
                else android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            })
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
                text = "Load Details",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkBlue
            )
            Text(
                text = "Specify the details of your cargo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // CATEGORY SELECTION
            Text("Cargo Category", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            var showCategoryDialog by remember { mutableStateOf(false) }
            val categories = listOf("Industrial Goods", "FMCG", "Construction", "Perishables", "Furniture", "Electronics")
            
            AppleSelector(
                text = viewModel.loadCategory, 
                icon = Icons.Default.Category,
                onClick = { showCategoryDialog = true }
            )

            if (showCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showCategoryDialog = false },
                    title = { Text("Select Cargo Category", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            categories.forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.loadCategory = category
                                            showCategoryDialog = false 
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = viewModel.loadCategory == category,
                                        onClick = { 
                                            viewModel.loadCategory = category
                                            showCategoryDialog = false 
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCategoryDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PHYSICAL SPECIFICATIONS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LoadInputField(
                            label = "Weight (Tons)", 
                            value = viewModel.weightInput, 
                            onValueChange = { viewModel.weightInput = it }, 
                            icon = Icons.Default.Scale,
                            modifier = Modifier.weight(1f)
                        )
                        LoadInputField(
                            label = "Packages", 
                            value = viewModel.numPackages.toString(), 
                            onValueChange = { viewModel.numPackages = it.toIntOrNull() ?: 1 }, 
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PACKAGING & HANDLING
            Text("Handling Requirements", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            PremiumToggleCard(
                title = "Fragile Cargo", 
                subtitle = "Items requiring extra care", 
                icon = Icons.Default.LocalBar, 
                isChecked = viewModel.isFragile, 
                onCheckedChange = { viewModel.isFragile = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PremiumToggleCard(
                title = "Hazardous Material", 
                subtitle = "Requires certified handling", 
                icon = Icons.Default.Warning, 
                isChecked = false, 
                onCheckedChange = {  }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // UPLOADS SECTION (Premium beige style)
            Text("Documents & Photos", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                UploadBox(label = "Cargo Photo", icon = Icons.Default.AddAPhoto, modifier = Modifier.weight(1f))
                UploadBox(label = "Invoice / Waybill", icon = Icons.Default.Description, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun LoadInputField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    icon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.LightGray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = PrimaryBlue,
                unfocusedIndicatorColor = Color.LightGray.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
fun PremiumToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(20.dp),
        color = if (isChecked) PrimaryBlue.copy(alpha = 0.05f) else Color.White,
        border = BorderStroke(1.dp, if (isChecked) PrimaryBlue.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(if (isChecked) PrimaryBlue else Background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (isChecked) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Background
                )
            )
        }
    }
}

@Composable
fun UploadBox(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        color = Beige.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Beige.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color(0xFF5D4037), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
            Text("Tap to upload", fontSize = 10.sp, color = Color(0xFF5D4037).copy(alpha = 0.6f))
        }
    }
}

@Composable
fun LoadSummaryCard(weight: String, onContinue: () -> Unit) {
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
                    Text("Total Load", color = Color.Gray, fontSize = 12.sp)
                    Text("${weight.ifEmpty { "0" }} Tons", fontWeight = FontWeight.Bold, color = DarkBlue, fontSize = 18.sp)
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.height(56.dp).width(160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Select Truck", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}
