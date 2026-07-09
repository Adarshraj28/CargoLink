package com.cargolink.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cargolink.app.firebase.AuthManager
import com.cargolink.app.firebase.FirestoreManager
import com.cargolink.app.ui.theme.DarkBlue

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AddAPhoto
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverVerificationScreen(onBack: () -> Unit) {
    var licenseNumber by remember { mutableStateOf("") }
    var rcNumber by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf("Aadhaar") }
    var idNumber by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    
    var licenseUri by remember { mutableStateOf<Uri?>(null) }
    var rcUri by remember { mutableStateOf<Uri?>(null) }
    var truckUri by remember { mutableStateOf<Uri?>(null) }

    val licenseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { licenseUri = it }
    val rcLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { rcUri = it }
    val truckLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { truckUri = it }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Driver Verification", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Verify your identity and vehicle to start taking orders.", color = Color.Gray) }
            item { OutlinedTextField(value = licenseNumber, onValueChange = { licenseNumber = it }, label = { Text("Driving License Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
            item { OutlinedTextField(value = rcNumber, onValueChange = { rcNumber = it }, label = { Text("Vehicle RC Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
            item {
                Text("ID Proof Type", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Aadhaar", "PAN").forEach { type -> FilterChip(selected = idType == type, onClick = { idType = type }, label = { Text(type) }) }
                }
            }
            item { OutlinedTextField(value = idNumber, onValueChange = { idNumber = it }, label = { Text("$idType Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
            item {
                Text("Upload Document Photos", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoPickerBox("Truck", truckUri) { truckLauncher.launch("image/*") }
                    PhotoPickerBox("License", licenseUri) { licenseLauncher.launch("image/*") }
                    PhotoPickerBox("RC", rcUri) { rcLauncher.launch("image/*") }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (licenseNumber.isBlank() || rcNumber.isBlank() || idNumber.isBlank() || licenseUri == null || rcUri == null || truckUri == null) { 
                            Toast.makeText(context, "Please fill all details and upload photos", Toast.LENGTH_SHORT).show()
                            return@Button 
                        }
                        loading = true
                        AuthManager.getCurrentUserEmail()?.let { email ->
                            // In a real app, upload URIs to Firebase Storage first
                            FirestoreManager.submitDriverDocs(email, licenseUri.toString(), rcUri.toString(), idType, idNumber, truckUri.toString(),
                                onSuccess = { loading = false; onBack() },
                                onError = { loading = false; Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Submit for Verification", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PhotoPickerBox(label: String, uri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (uri != null) Color.Transparent else Color.LightGray.copy(alpha = 0.4f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Text(label, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
