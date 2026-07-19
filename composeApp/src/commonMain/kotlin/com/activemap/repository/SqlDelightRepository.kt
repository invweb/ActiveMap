package com.activemap.repository

import com.activemap.db.ActiveMapDatabase
import com.activemap.db.PlaceEntity
import com.activemap.db.ActivityEntity
import com.activemap.db.ChallengeEntity
import com.activemap.model.*
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class SqlDelightRepository(
    private val database: ActiveMapDatabase
) : PlaceRepository {

    private val queries get() = database.activeMapQueries

    override fun getAllPlaces(): Flow<List<Place>> {
        return queries.selectAllPlaces().asFlow().map { it.executeAsList().map { e -> e.toPlace() } }
    }

    override fun getPlacesByType(type: PlaceType): Flow<List<Place>> {
        return queries.selectPlacesByType(type.name).asFlow().map { it.executeAsList().map { e -> e.toPlace() } }
    }

    override suspend fun getPlaceById(id: Long): Place? {
        return withContext(Dispatchers.Default) {
            queries.selectPlaceById(id).executeAsOneOrNull()?.toPlace()
        }
    }

    override suspend fun insertPlace(place: Place): Long {
        return withContext(Dispatchers.Default) {
            queries.insertPlace(
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                type = place.type.name,
                description = place.description,
                createdAt = place.createdAt.toString()
            )
            queries.selectAllPlaces().executeAsList().first().id
        }
    }

    override suspend fun deletePlace(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deletePlace(id)
        }
    }

    override fun getRecentActivities(): Flow<List<Activity>> {
        return queries.selectAllActivities().asFlow().map { it.executeAsList().map { e -> e.toActivity() } }
    }

    override suspend fun insertActivity(activity: Activity): Long {
        return withContext(Dispatchers.Default) {
            queries.insertActivity(
                placeId = activity.placeId,
                description = activity.description,
                startedAt = activity.startedAt.toString(),
                completedAt = activity.completedAt?.toString(),
                durationMinutes = activity.durationMinutes.toLong()
            )
            queries.selectAllActivities().executeAsList().first().id
        }
    }

    override suspend fun completeActivity(id: Long, durationMinutes: Int) {
        withContext(Dispatchers.Default) {
            queries.updateActivityCompletion(
                completedAt = Clock.System.now().toString(),
                durationMinutes = durationMinutes.toLong(),
                id = id
            )
        }
    }

    override suspend fun getRecentActivityCount(days: Int): Int {
        return withContext(Dispatchers.Default) {
            val cutoff = Clock.System.now().minus(period = DateTimePeriod(days = days), timeZone = TimeZone.currentSystemDefault())
            queries.countRecentActivities(cutoff.toString()).executeAsOne().toInt()
        }
    }

    override suspend fun getLastActivityDaysAgo(): Int {
        return withContext(Dispatchers.Default) {
            val activities = queries.selectAllActivities().executeAsList()
            if (activities.isEmpty()) return@withContext 30
            val lastActivity = activities.maxByOrNull { it.startedAt } ?: return@withContext 30
            val lastDate = Instant.parse(lastActivity.startedAt)
            val now = Clock.System.now()
            val diffMs = now.toEpochMilliseconds() - lastDate.toEpochMilliseconds()
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    override suspend fun getWeeklyActivityCount(): Int {
        return withContext(Dispatchers.Default) {
            val weekAgo = Clock.System.now().minus(period = DateTimePeriod(days = 7), timeZone = TimeZone.currentSystemDefault())
            queries.countRecentActivities(weekAgo.toString()).executeAsOne().toInt()
        }
    }

    override fun getChallengesForDate(date: LocalDate): Flow<List<Challenge>> {
        return queries.selectChallengesByDate(date.toString()).asFlow().map { it.executeAsList().map { e -> e.toChallenge() } }
    }

    override fun getRecentChallenges(): Flow<List<Challenge>> {
        return queries.selectRecentChallenges().asFlow().map { it.executeAsList().map { e -> e.toChallenge() } }
    }

    override suspend fun insertChallenge(challenge: Challenge): Long {
        return withContext(Dispatchers.Default) {
            queries.insertChallenge(
                title = challenge.title,
                description = challenge.description,
                type = challenge.type.name,
                completed = if (challenge.completed) 1L else 0L,
                assignedDate = challenge.assignedDate.toString()
            )
            queries.selectChallengesByDate(challenge.assignedDate.toString()).executeAsList().first().id
        }
    }

    override suspend fun completeChallenge(id: Long) {
        withContext(Dispatchers.Default) {
            queries.updateChallengeCompletion(completed = 1L, id = id)
        }
    }

    override suspend fun getChallengeStreak(): Int {
        return withContext(Dispatchers.Default) {
            val challenges = queries.selectRecentChallenges().executeAsList()
            var streak = 0
            var currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            for (i in 0 until 30) {
                val dayChallenges = challenges.filter { it.assignedDate == currentDate.toString() }
                if (dayChallenges.isNotEmpty() && dayChallenges.all { it.completed == 1L }) {
                    streak++
                } else if (dayChallenges.isEmpty()) {
                    break
                } else {
                    break
                }
                currentDate = currentDate.minus(1, DateTimeUnit.DAY)
            }
            streak
        }
    }

    override suspend fun getPlacesCountByType(): Map<PlaceType, Int> {
        return withContext(Dispatchers.Default) {
            val results = queries.countPlacesByType().executeAsList()
            results.associate { PlaceType.valueOf(it.type) to it.cnt.toInt() }
        }
    }

    override suspend fun getWeatherCache(): Weather? {
        return withContext(Dispatchers.Default) {
            val entity = queries.selectLatestWeather().executeAsOneOrNull() ?: return@withContext null
            Weather(
                temperature = entity.temperature,
                condition = WeatherCondition.valueOf(entity.condition)
            )
        }
    }

    override suspend fun cacheWeather(weather: Weather) {
        withContext(Dispatchers.Default) {
            queries.insertWeather(
                temperature = weather.temperature,
                condition = weather.condition.name,
                fetchedAt = Clock.System.now().toString()
            )
        }
    }

    override suspend fun getAllPlacesList(): List<Place> {
        return withContext(Dispatchers.Default) {
            queries.selectAllPlaces().executeAsList().map { it.toPlace() }
        }
    }
}

private fun PlaceEntity.toPlace(): Place {
    return Place(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        type = PlaceType.valueOf(type),
        description = description,
        createdAt = Instant.parse(createdAt)
    )
}

private fun ActivityEntity.toActivity(): Activity {
    return Activity(
        id = id,
        placeId = placeId,
        description = description,
        startedAt = Instant.parse(startedAt),
        completedAt = completedAt?.let { Instant.parse(it) },
        durationMinutes = durationMinutes.toInt()
    )
}

private fun ChallengeEntity.toChallenge(): Challenge {
    return Challenge(
        id = id,
        title = title,
        description = description,
        type = ChallengeType.valueOf(type),
        completed = completed == 1L,
        assignedDate = LocalDate.parse(assignedDate)
    )
}
