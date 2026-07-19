package com.activemap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.activemap.model.PlaceType
import com.activemap.ui.animation.AnimationConfig
import com.activemap.ui.animation.fadeInSlideUp
import com.activemap.viewmodel.MainViewModel

@Composable
fun AddPlaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val animationConfig by viewModel.animationConfig.collectAsState()
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("55.7558") }
    var longitude by remember { mutableStateOf("37.6173") }
    var selectedType by remember { mutableStateOf(PlaceType.PARK) }
    var description by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectScreen(com.activemap.viewmodel.Screen.Map) }) {
                Text("←")
            }
            Text("Add New Place", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Place Name") },
                        modifier = Modifier.fillMaxWidth().then(mod),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            item {
                fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
                    Row(modifier = Modifier.fillMaxWidth().then(mod), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
                    Column(modifier = mod) {
                        Text("Place Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlaceType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(when (type) { PlaceType.PARK -> "🌳 Park"; PlaceType.STADIUM -> "🏟 Stadium"; PlaceType.GYM -> "💪 Gym"; PlaceType.STREET -> "🏃 Street" }) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).then(mod),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            }

            item {
                fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
                    Button(
                        onClick = {
                            val lat = latitude.replace(",", ".").trim().toDoubleOrNull() ?: 55.7558
                            val lon = longitude.replace(",", ".").trim().toDoubleOrNull() ?: 37.6173
                            viewModel.addPlace(name = name, latitude = lat, longitude = lon, type = selectedType, description = description)
                            viewModel.selectScreen(com.activemap.viewmodel.Screen.Map)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).then(mod),
                        shape = RoundedCornerShape(12.dp),
                        enabled = name.isNotBlank()
                    ) { Text("Save Place", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}
