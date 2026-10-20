/*
 * Copyright 2024 David Takač
 *
 * This file is part of Bura.
 *
 * Bura is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Bura is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Bura. If not, see <https://www.gnu.org/licenses/>.
 */

package com.davidtakac.bura.places.current

import com.davidtakac.bura.places.Coordinates
import com.davidtakac.bura.places.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId

class CurrentLocationRepository(private val root: File) {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    // The current location is updated when the summary refreshes, not where it's shown
    val changes: Flow<Unit> = _changes

    // Only kept in memory, the app locates again after it restarts anyway
    private var error: CurrentLocationError? = null

    fun getError(): CurrentLocationError? = error

    fun setError(error: CurrentLocationError?) {
        this.error = error
        _changes.tryEmit(Unit)
    }

    suspend fun getCurrentLocation(): CurrentLocation? =
        withContext(Dispatchers.IO) {
            val file = getFile()
            if (!file.exists()) return@withContext null
            val json = JSONObject(file.readText())
            CurrentLocation(
                location = Location(
                    timeZone = ZoneId.of(json.getString("timeZone")),
                    coordinates = Coordinates(
                        latitude = json.getDouble("latitude"),
                        longitude = json.getDouble("longitude")
                    )
                ),
                updatedAt = Instant.ofEpochSecond(json.getLong("updatedAt"))
            )
        }

    suspend fun setCurrentLocation(current: CurrentLocation) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("timeZone", current.location.timeZone.id)
                put("latitude", current.location.coordinates.latitude)
                put("longitude", current.location.coordinates.longitude)
                put("updatedAt", current.updatedAt.epochSecond)
            }
            getFile().writeText(json.toString())
        }
        _changes.tryEmit(Unit)
    }

    private fun getFile() = File(root, "current_location")
}
