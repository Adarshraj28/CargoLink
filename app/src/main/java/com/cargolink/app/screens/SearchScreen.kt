package com.cargolink.app.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cargolink.app.components.StatusChip
import com.cargolink.app.models.Shipment
import com.cargolink.app.ui.theme.*
import com.cargolink.app.viewmodel.SearchItem
import com.cargolink.app.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    userRole: String = "Vendor",
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Shipments", "People", "Payments", "Locations")

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkBlue)
                        }
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onQueryChange(it, userRole) },
                            placeholder = { Text("Search shipments, drivers...", color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("", userRole) }) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                } else {
                                    Icon(Icons.Default.Mic, null, tint = PrimaryBlue)
                                }
                            }
                        )
                        
                        Button(
                            onClick = { viewModel.performSearch(searchQuery, userRole) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Search")
                        }
                    }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Background)) {
            if (searchQuery.isEmpty()) {
                // Show History and Trending
                SearchIdleContent(
                    history = history,
                    onHistoryClick = { viewModel.performSearch(it, userRole) },
                    onClearHistory = { viewModel.clearHistory() }
                )
            } else if (suggestions.isNotEmpty() && searchResults.isEmpty()) {
                // Show Suggestions
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(suggestions) { suggestion ->
                        SuggestionItem(suggestion) {
                            viewModel.onQueryChange(suggestion, userRole)
                            viewModel.performSearch(suggestion, userRole)
                        }
                    }
                }
            } else {
                // Show Results
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (searchResults.isEmpty()) {
                    EmptyResultsView(searchQuery)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults) { item ->
                            when (item) {
                                is SearchItem.ShipmentItem -> ShipmentResultCard(item.shipment)
                                is SearchItem.UserItem -> UserResultCard(item)
                                is SearchItem.TransactionItem -> TransactionResultCard(item)
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchIdleContent(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", fontWeight = FontWeight.Bold, color = DarkBlue)
                TextButton(onClick = onClearHistory) {
                    Text("Clear All", color = Color.Gray, fontSize = 12.sp)
                }
            }
            
            history.forEach { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHistoryClick(query) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(query, color = Color.DarkGray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Trending Categories", fontWeight = FontWeight.Bold, color = DarkBlue)
        Spacer(modifier = Modifier.height(16.dp))
        
        val categories = listOf(
            "Delhi Warehouse" to Icons.Default.Warehouse,
            "Available Loads" to Icons.Default.LocalShipping,
            "Completed Trips" to Icons.Default.CheckCircle,
            "Urgent Deliveries" to Icons.Default.Bolt
        )
        
        categories.forEach { (title, icon) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(PrimaryBlue.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(title, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 16.sp)
    }
}

@Composable
fun ShipmentResultCard(shipment: Shipment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("#${shipment.id.takeLast(6)}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                StatusChip(shipment.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${shipment.pickupAddress} → ${shipment.destinationAddress}", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(shipment.loadType, color = Color.Gray, fontSize = 12.sp)
                Text(shipment.price, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }
        }
    }
}

@Composable
fun UserResultCard(user: SearchItem.UserItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(Color.LightGray.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = DarkBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text(user.role, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TransactionResultCard(item: SearchItem.TransactionItem) {
    // Placeholder
}

@Composable
fun EmptyResultsView(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No results found for \"$query\"", color = Color.Gray)
        Text("Try searching for cities, IDs, or statuses", color = Color.LightGray, fontSize = 12.sp)
    }
}
