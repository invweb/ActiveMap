package com.activemap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.activemap.map.OsmMap
import com.activemap.map.OsmState
import com.activemap.model.*
import com.activemap.ui.animation.AnimationConfig
import com.activemap.ui.animation.fadeInSlideUp
import com.activemap.ui.animation.pulseGlow
import com.activemap.ui.components.ChallengeCard
import com.activemap.ui.components.PlaceListItem
import com.activemap.ui.components.RecommendationCard
import com.activemap.viewmodel.MainViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val places by viewModel.places.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val challenges by viewModel.challenges.collectAsState()
    val animationConfig by viewModel.animationConfig.collectAsState()
    val currentWeather = viewModel.currentWeather.collectAsState()
    val isDarkTheme = viewModel.isDarkTheme.collectAsState()

    val osmState by viewModel.osmState.collectAsState()
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var placeToDelete by remember { mutableStateOf<Place?>(null) }

    val navigateTo = viewModel.navigateTo.collectAsState()
    LaunchedEffect(navigateTo.value) {
        navigateTo.value?.let { (lat, lon) ->
            viewModel.updateOsmState(OsmState(centerLat = lat, centerLon = lon, zoom = 16))
            viewModel.consumeNavigation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.generateDailyChallenge()
        viewModel.generateRecommendations()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Map — full screen, captures drag in all directions
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                    change.consume()
                    val n = 2.0.pow(osmState.zoom.toDouble())
                    val tileSize = 256f
                    val pixelsPerDegree = tileSize * n / 360.0
                    val lonDelta = dragAmount.x / pixelsPerDegree
                    val latDelta = dragAmount.y / pixelsPerDegree
                    viewModel.updateOsmState(osmState.copy(
                        centerLon = (osmState.centerLon - lonDelta).coerceIn(-180.0, 180.0),
                        centerLat = (osmState.centerLat + latDelta).coerceIn(-85.0, 85.0)
                    ))
                }
            }
        ) {
            OsmMap(
                state = osmState,
                onStateChanged = { viewModel.updateOsmState(it) },
                places = places,
                modifier = Modifier.fillMaxSize()
            )

            // Zoom + Location controls — right side, above bottom panel
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 90.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.updateOsmState(osmState.copy(zoom = (osmState.zoom + 1).coerceAtMost(19))) },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { Text("+", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(6.dp))
                FloatingActionButton(
                    onClick = { viewModel.updateOsmState(osmState.copy(zoom = (osmState.zoom - 1).coerceAtLeast(1))) },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { Text("-", fontSize = 20.sp) }
                Spacer(modifier = Modifier.height(6.dp))
                FloatingActionButton(
                    onClick = { viewModel.updateOsmState(OsmState()) },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text("📍", fontSize = 16.sp) }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🗺", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OpenStreetMap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    LegendItem(color = Color(0xFF4CAF50), label = "Park")
                    LegendItem(color = Color(0xFFFF5722), label = "Gym")
                    LegendItem(color = Color(0xFF2196F3), label = "Stadium")
                    LegendItem(color = Color(0xFF9C27B0), label = "Street")
                }
            }
        }

        // Top bar — transparent overlay on map
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ActiveMap", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    currentWeather.value?.let { weather ->
                        Text("${weather.temperature.toInt()}°C • ${weather.condition}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Row {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Text(if (isDarkTheme.value) "☀️" else "🌙")
                    }
                    IconButton(onClick = { viewModel.selectScreen(com.activemap.viewmodel.Screen.Report) }) {
                        Text("📊")
                    }
                }
            }
        }

        // Bottom panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.50f),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (challenges.isNotEmpty()) {
                    item { Text("Today's Challenge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) }
                    itemsIndexed(challenges) { _, challenge ->
                        ChallengeCard(challenge = challenge, animationConfig = animationConfig, onComplete = { viewModel.completeChallenge(challenge.id) })
                    }
                }
                if (recommendations.isNotEmpty()) {
                    item { Text("Recommended for You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) }
                    itemsIndexed(recommendations) { _, recommendation ->
                        RecommendationCard(recommendation = recommendation, animationConfig = animationConfig, onClick = {
                            val place = places.find { it.id == recommendation.placeId }
                            if (place != null) { selectedPlace = place; showWorkoutDialog = true }
                        })
                    }
                }
                item { Text("Your Places", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp)) }
                itemsIndexed(places) { index, place ->
                    PlaceListItem(
                        place = place,
                        index = index,
                        animationConfig = animationConfig,
                        onClick = { selectedPlace = place; showWorkoutDialog = true },
                        onNavigate = { viewModel.updateOsmState(osmState.copy(centerLat = place.latitude, centerLon = place.longitude, zoom = 16)) },
                        onDelete = { placeToDelete = place }
                    )
                }
                item {
                    Button(
                        onClick = {
                            viewModel.setPendingCoords(osmState.centerLat, osmState.centerLon)
                            viewModel.selectScreen(com.activemap.viewmodel.Screen.AddPlace)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("+ Add New Place") }
                }
            }
        }
    }

    if (showWorkoutDialog && selectedPlace != null) {
        AlertDialog(
            onDismissRequest = { showWorkoutDialog = false },
            title = { Text("Start Workout") },
            text = { Column { Text("Start a workout at ${selectedPlace?.name}?"); Spacer(modifier = Modifier.height(8.dp)); pulseGlow(config = animationConfig) { mod -> Text("Ready to go!", modifier = mod) } } },
            confirmButton = { Button(onClick = { selectedPlace?.let { place -> viewModel.startActivity(place.id, "Workout at ${place.name}") }; showWorkoutDialog = false }) { Text("Start") } },
            dismissButton = { TextButton(onClick = { showWorkoutDialog = false }) { Text("Cancel") } }
        )
    }

    // Delete confirmation dialog
    if (placeToDelete != null) {
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text("Delete Place") },
            text = { Text("Delete \"${placeToDelete?.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        placeToDelete?.let { viewModel.deletePlace(it.id) }
                        placeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { placeToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}
