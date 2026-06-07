package com.truckify.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.truckify.app.components.NotificationCard
import com.truckify.app.components.TruckifyTopAppBar
import com.truckify.app.firebase.AuthManager
import com.truckify.app.firebase.FirestoreManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(onBack: () -> Unit) {
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    DisposableEffect(Unit) {
        val email = AuthManager.getCurrentUserEmail() ?: ""
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        if (email.isNotEmpty()) {
            registration = FirestoreManager.getNotifications(email) { notifications = it }
        }
        onDispose { registration?.remove() }
    }

    Scaffold(
        topBar = {
            TruckifyTopAppBar(
                title = "Notifications",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (notifications.isEmpty()) {
                    item { Text("No new notifications.", color = Color.Gray) }
                } else {
                    items(notifications.size) { index ->
                        val n = notifications[index]
                        NotificationCard(
                            title = n["title"] as? String ?: "Update",
                            message = n["message"] as? String ?: "",
                            time = "Just now"
                        )
                    }
                }
            }
        }
    }
}
