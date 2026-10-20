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
import com.davidtakac.bura.places.Location
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sqrt

// Forecast models have a resolution of a few kilometers, so smaller moves don't change the forecast
private const val MIN_UPDATE_DISTANCE_KM = 15.0
// Limits forecast downloads when traveling fast
private val minUpdateInterval = Duration.ofMinutes(30)
private const val EARTH_RADIUS_KM = 6371.0

class UpdateCurrentLocation(
    private val locator: DeviceLocator,
    private val currentLocationRepo: CurrentLocationRepository,
    private val forecastCacher: ForecastCacher,
) {
    suspend operator fun invoke(now: Instant) {
        val current = currentLocationRepo.getCurrentLocation()
        if (!shouldLocate(current, now)) return
        // A location known to the device that is younger than the interval is as good as a new one
        val fix = locator.locate(maxLastKnownAge = minUpdateInterval) ?: return
        if (current != null && !hasMoved(current, fix)) {
            // Keep the location and its forecast, but remember it's still up to date
            currentLocationRepo.setCurrentLocation(current.copy(updatedAt = now))
            return
        }
        // The device's time zone is the one where the user is at the time of the fix
        val location = Location(timeZone = ZoneId.systemDefault(), coordinates = fix)
        currentLocationRepo.setCurrentLocation(CurrentLocation(location, updatedAt = now))
        // The previous location's forecast won't be used again, don't let them pile up while traveling
        if (current != null) forecastCacher.delete(current.location.coordinates)
    }
}

fun shouldLocate(current: CurrentLocation?, now: Instant): Boolean =
    current == null || Duration.between(current.updatedAt, now) >= minUpdateInterval

fun hasMoved(current: CurrentLocation, fix: Coordinates): Boolean =
    current.location.coordinates.distanceKmTo(fix) >= MIN_UPDATE_DISTANCE_KM

// Equirectangular approximation, accurate enough at the distances that matter here
private fun Coordinates.distanceKmTo(other: Coordinates): Double {
    val x = Math.toRadians(other.longitude - longitude) * cos(Math.toRadians((latitude + other.latitude) / 2))
    val y = Math.toRadians(other.latitude - latitude)
    return sqrt(x * x + y * y) * EARTH_RADIUS_KM
}
