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

package com.davidtakac.bura.places.selected

import android.content.SharedPreferences
import androidx.core.content.edit
import com.davidtakac.bura.places.Coordinates
import com.davidtakac.bura.places.Location
import com.davidtakac.bura.places.Place
import com.davidtakac.bura.places.current.CurrentLocationRepository
import com.davidtakac.bura.places.saved.SavedPlacesRepository

private const val SELECTED_PLACE_KEY = "selected_place_coords"
// Stored as the selected place instead of coordinates when following the device location
private const val CURRENT_LOCATION = "current_location"

class SelectedPlaceRepository(
    private val prefs: SharedPreferences,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val currentLocationRepository: CurrentLocationRepository
) {
    suspend fun selectPlace(place: Place) =
        prefs.edit { putString(SELECTED_PLACE_KEY, place.location.coordinates.id) }

    suspend fun selectCurrentLocation() =
        prefs.edit { putString(SELECTED_PLACE_KEY, CURRENT_LOCATION) }

    suspend fun isCurrentLocationSelected(): Boolean =
        prefs.getString(SELECTED_PLACE_KEY, null) == CURRENT_LOCATION

    suspend fun getSelectedPlace(): Place? {
        val selected = prefs.getString(SELECTED_PLACE_KEY, null) ?: return null
        if (selected == CURRENT_LOCATION) {
            return currentLocationRepository.getCurrentLocation()?.location?.let(::currentLocationPlace)
        }
        return savedPlacesRepository.getSavedPlace(Coordinates.fromId(selected))
    }
}

// Unnamed because the UI labels it
private fun currentLocationPlace(location: Location) = Place(
    name = "",
    admin1 = null,
    admin2 = null,
    admin3 = null,
    admin4 = null,
    countryCode = null,
    countryName = null,
    location = location
)
