package com.activemap

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.activemap.ui.screens.AddPlaceScreen
import com.activemap.ui.screens.ChallengeScreen
import com.activemap.ui.screens.MapScreen
import com.activemap.ui.screens.ReportScreen
import com.activemap.ui.theme.ActiveMapTheme
import com.activemap.viewmodel.MainViewModel
import com.activemap.viewmodel.Screen

@Composable
fun App(viewModel: MainViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val selectedScreen by viewModel.selectedScreen.collectAsState()

    ActiveMapTheme(darkTheme = isDarkTheme) {
        val backgroundColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.background,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            when (selectedScreen) {
                is Screen.Map -> MapScreen(viewModel = viewModel)
                is Screen.AddPlace -> AddPlaceScreen(viewModel = viewModel)
                is Screen.Report -> ReportScreen(viewModel = viewModel)
                is Screen.Challenges -> ChallengeScreen(viewModel = viewModel)
            }
        }
    }
}
