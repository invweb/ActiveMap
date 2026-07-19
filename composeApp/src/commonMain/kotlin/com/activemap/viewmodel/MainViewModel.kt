package com.activemap.viewmodel

import com.activemap.engine.RuleEngine
import com.activemap.export.MarkdownExporter
import com.activemap.model.*
import com.activemap.platform.FileExporter
import com.activemap.platform.LocationProvider
import com.activemap.platform.WeatherInfo
import com.activemap.platform.WeatherProvider
import com.activemap.repository.PlaceRepository
import com.activemap.ui.animation.AnimationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class MainViewModel(
    private val repository: PlaceRepository,
    private val locationProvider: LocationProvider,
    private val weatherProvider: WeatherProvider,
    private val fileExporter: FileExporter
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()

    private val _currentWeather = MutableStateFlow<WeatherInfo?>(null)
    val currentWeather: StateFlow<WeatherInfo?> = _currentWeather.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _animationConfig = MutableStateFlow(AnimationConfig(enabled = true))
    val animationConfig: StateFlow<AnimationConfig> = _animationConfig.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    private val _selectedScreen = MutableStateFlow<Screen>(Screen.Map)
    val selectedScreen: StateFlow<Screen> = _selectedScreen.asStateFlow()

    init {
        loadData()
    }

    fun selectScreen(screen: Screen) {
        _selectedScreen.value = screen
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        _animationConfig.value = AnimationConfig(enabled = enabled)
    }

    private fun loadData() {
        scope.launch {
            repository.getAllPlaces().collect { _places.value = it }
        }
        scope.launch {
            repository.getRecentActivities().collect { _activities.value = it }
        }
        scope.launch {
            loadWeather()
            generateRecommendations()
        }
        scope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            repository.getChallengesForDate(today).collect { _challenges.value = it }
        }
    }

    private suspend fun loadWeather() {
        try {
            val weather = weatherProvider.getCurrentWeather()
            _currentWeather.value = weather
        } catch (e: Exception) {
            _currentWeather.value = WeatherInfo(20.0, "SUNNY")
        }
    }

    fun generateRecommendations() {
        scope.launch {
            try {
                val weather = _currentWeather.value ?: WeatherInfo(20.0, "SUNNY")
                val recentCount = repository.getRecentActivityCount(7)
                val lastDaysAgo = repository.getLastActivityDaysAgo()
                val placesCount = repository.getPlacesCountByType()
                val weeklyCount = repository.getWeeklyActivityCount()
                val streak = repository.getChallengeStreak()

                val context = RuleEngine.buildContext(
                    weather = weather,
                    recentActivityCount = recentCount,
                    lastActivityDaysAgo = lastDaysAgo,
                    placesCount = placesCount,
                    totalActivitiesThisWeek = weeklyCount,
                    challengeStreak = streak
                )

                val allPlaces = repository.getAllPlacesList()
                val recs = RuleEngine.generateRecommendations(context, allPlaces)
                _recommendations.value = recs
            } catch (e: Exception) {
                _recommendations.value = emptyList()
            }
        }
    }

    fun addPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        type: PlaceType,
        description: String
    ) {
        scope.launch {
            val place = Place(
                name = name,
                latitude = latitude,
                longitude = longitude,
                type = type,
                description = description
            )
            repository.insertPlace(place)
            _places.value = repository.getAllPlacesList()
            generateRecommendations()
        }
    }

    fun deletePlace(id: Long) {
        scope.launch {
            repository.deletePlace(id)
            _places.value = repository.getAllPlacesList()
            generateRecommendations()
        }
    }

    fun startActivity(placeId: Long, description: String) {
        scope.launch {
            val activity = Activity(
                placeId = placeId,
                description = description,
                startedAt = Clock.System.now()
            )
            repository.insertActivity(activity)
            repository.getRecentActivities().first().let { _activities.value = it }
        }
    }

    fun completeActivity(id: Long, durationMinutes: Int) {
        scope.launch {
            repository.completeActivity(id, durationMinutes)
            repository.getRecentActivities().first().let { _activities.value = it }
        }
    }

    fun completeChallenge(id: Long) {
        scope.launch {
            repository.completeChallenge(id)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            repository.getChallengesForDate(today).first().let { _challenges.value = it }
            generateRecommendations()
        }
    }

    fun generateDailyChallenge() {
        scope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val existingChallenges = _challenges.value
            if (existingChallenges.isNotEmpty()) return@launch

            val challengeTypes = ChallengeType.entries
            val randomType = challengeTypes.random()

            val challenge = when (randomType) {
                ChallengeType.WALK -> Challenge(
                    title = "10-Minute Walk",
                    description = "Take a brisk 10-minute walk around your neighborhood.",
                    type = randomType,
                    assignedDate = today
                )
                ChallengeType.RUN -> Challenge(
                    title = "5-Minute Jog",
                    description = "Jog for 5 minutes at a comfortable pace.",
                    type = randomType,
                    assignedDate = today
                )
                ChallengeType.STRETCH -> Challenge(
                    title = "Morning Stretch",
                    description = "Do 5 minutes of full-body stretching.",
                    type = randomType,
                    assignedDate = today
                )
                ChallengeType.PLANK -> Challenge(
                    title = "30-Second Plank",
                    description = "Hold a plank position for 30 seconds.",
                    type = randomType,
                    assignedDate = today
                )
                ChallengeType.SQUATS -> Challenge(
                    title = "15 Squats",
                    description = "Do 15 bodyweight squats with good form.",
                    type = randomType,
                    assignedDate = today
                )
                ChallengeType.CARDIO -> Challenge(
                    title = "Jumping Jacks",
                    description = "Do 20 jumping jacks to get your heart rate up.",
                    type = randomType,
                    assignedDate = today
                )
            }

            repository.insertChallenge(challenge)
            repository.getChallengesForDate(today).first().let { _challenges.value = it }
        }
    }

    fun exportWeeklyReport() {
        scope.launch {
            val now = Clock.System.now()
            val weekStart = now.minus(period = DateTimePeriod(days = 7), timeZone = TimeZone.currentSystemDefault())
            val activities = repository.getRecentActivities().first()
            val challenges = repository.getRecentChallenges().first()
            val places = repository.getAllPlacesList()

            val report = MarkdownExporter.generateWeeklyReport(
                activities = activities,
                challenges = challenges,
                places = places,
                weekStart = weekStart,
                weekEnd = now
            )

            val filename = "ActiveMap_Report_${now.toLocalDateTime(TimeZone.currentSystemDefault()).date}.md"
            val path = fileExporter.exportMarkdown(report, filename)
            _exportResult.value = path
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
}

sealed class Screen {
    data object Map : Screen()
    data object AddPlace : Screen()
    data object Report : Screen()
    data object Challenges : Screen()
}
