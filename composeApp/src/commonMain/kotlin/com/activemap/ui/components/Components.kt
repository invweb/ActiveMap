package com.activemap.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.activemap.model.*
import com.activemap.ui.animation.AnimationConfig
import com.activemap.ui.animation.bounceAppear
import com.activemap.ui.animation.fadeInSlideUp
import com.activemap.ui.animation.pulseGlow
import com.activemap.ui.theme.ActiveMapColors
import com.activemap.ui.theme.LightColors

@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    animationConfig: AnimationConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .then(mod)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recommendation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    ScoreBadge(score = recommendation.score)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Double) {
    Surface(
        shape = CircleShape,
        color = when {
            score >= 9.0 -> Color(0xFF4CAF50)
            score >= 7.0 -> Color(0xFF2196F3)
            score >= 5.0 -> Color(0xFFFF9800)
            else -> Color(0xFF9E9E9E)
        },
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = String.format("%.1f", score),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    animationConfig: AnimationConfig,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    fadeInSlideUp(visible = visible, config = animationConfig) { mod ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .then(mod),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (challenge.completed)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (challenge.completed) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✓", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (!challenge.completed) {
                    pulseGlow(config = animationConfig) { mod ->
                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(mod),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Complete Challenge")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceListItem(
    place: Place,
    index: Int,
    animationConfig: AnimationConfig,
    onClick: () -> Unit,
    onNavigate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300 + (index * 50),
            easing = FastOutSlowInEasing
        )
    )

    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(
            durationMillis = 300 + (index * 50),
            easing = FastOutSlowInEasing
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translateY
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content area — clickable
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = when (place.type) {
                        PlaceType.PARK -> Color(0xFF4CAF50)
                        PlaceType.STADIUM -> Color(0xFF2196F3)
                        PlaceType.GYM -> Color(0xFFFF5722)
                        PlaceType.STREET -> Color(0xFF9C27B0)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (place.type) {
                                PlaceType.PARK -> "🌳"
                                PlaceType.STADIUM -> "🏟"
                                PlaceType.GYM -> "💪"
                                PlaceType.STREET -> "🏃"
                            },
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${place.type.name.lowercase().replaceFirstChar { it.uppercase() }} • ${String.format("%.4f", place.latitude)}, ${String.format("%.4f", place.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (place.description.isNotEmpty()) {
                        Text(
                            text = place.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }
            }
            // Navigate button
            IconButton(onClick = onNavigate, modifier = Modifier.size(40.dp)) {
                Text("📍", fontSize = 20.sp)
            }
            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Text("🗑", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun StartWorkoutButton(
    animationConfig: AnimationConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    pulseGlow(config = animationConfig, modifier = modifier) { mod ->
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .then(mod),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Start Workout",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
