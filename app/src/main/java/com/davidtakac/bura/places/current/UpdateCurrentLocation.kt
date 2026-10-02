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

import com.davidtakac.bura.forecast.cache.ForecastCacher
import com.davidtakac.bura.places.Coordinates
import com.davidtakac.bura.places.selected.SelectedPlaceRepository
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sqrt

private const val MIN_DISTANCE_KM = 5.0
private const val EARTH_RADIUS_KM = 6371.0

class UpdateCurrentLocation(
    private val locator: DeviceLocator,
    private val selectedPlaceRepo: SelectedPlaceRepository,
    private val forecastCacher: ForecastCacher,
) {
    suspend operator fun invoke() {
        val old = selectedPlaceRepo.getCurrentLocation()
        val new = locator.locate()?.let { nextCurrentLocation(old, it) } ?: return
        selectedPlaceRepo.setCurrentLocation(new)
        // The old location's forecast won't be used again, don't let them pile up while traveling
        if (old != null) forecastCacher.delete(old)
    }
}

/**
 * Returns [fix] rounded to ~1 km if it is far enough from [current] to count as a new place, or
 * null if [current] should be kept. Keeping it avoids downloading a new forecast on every move.
 */
fun nextCurrentLocation(current: Coordinates?, fix: Coordinates): Coordinates? {
    if (current != null && current.distanceKmTo(fix) < MIN_DISTANCE_KM) return null
    return Coordinates(
        latitude = round(fix.latitude * 100) / 100,
        longitude = round(fix.longitude * 100) / 100
    )
}

// Equirectangular approximation, accurate enough at the distances that matter here
private fun Coordinates.distanceKmTo(other: Coordinates): Double {
    val x = Math.toRadians(other.longitude - longitude) * cos(Math.toRadians((latitude + other.latitude) / 2))
    val y = Math.toRadians(other.latitude - latitude)
    return sqrt(x * x + y * y) * EARTH_RADIUS_KM
}
