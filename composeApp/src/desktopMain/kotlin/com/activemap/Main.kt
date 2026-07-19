package com.activemap

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.activemap.db.ActiveMapDatabase
import com.activemap.platform.DesktopFileExporter
import com.activemap.platform.DesktopLocationProvider
import com.activemap.platform.DesktopWeatherProvider
import com.activemap.repository.SqlDelightRepository
import com.activemap.viewmodel.MainViewModel
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun main() = application {
    val dbFile = java.io.File("activemap.db")
    if (dbFile.exists()) dbFile.delete()
    val driver = JdbcSqliteDriver("jdbc:sqlite:activemap.db")
    ActiveMapDatabase.Schema.create(driver)
    val database = ActiveMapDatabase(driver)
    val repository = SqlDelightRepository(database)

    val locationProvider = DesktopLocationProvider()
    val weatherProvider = DesktopWeatherProvider()
    val fileExporter = DesktopFileExporter()

    val viewModel = MainViewModel(
        repository = repository,
        locationProvider = locationProvider,
        weatherProvider = weatherProvider,
        fileExporter = fileExporter
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "ActiveMap: AI-Coach for Beginners",
        state = rememberWindowState(width = 400.dp, height = 800.dp)
    ) {
        App(viewModel = viewModel)
    }
}
