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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class UpdateCurrentLocationTest {
    @get:Rule
    val root = TemporaryFolder()

    private val barcelona = Coordinates(41.39, 2.17)
    // ~18 km from Barcelona
    private val sabadell = Coordinates(41.55, 2.11)
    // ~9 km from Barcelona
    private val badalona = Coordinates(41.45, 2.25)
    private val updatedAt = Instant.ofEpochSecond(0)
    private val later = updatedAt + Duration.ofHours(1)

    private fun currentAt(coordinates: Coordinates) =
        CurrentLocation(Location(ZoneId.of("Europe/Madrid"), coordinates), updatedAt)

    private class FakeLocator(private val permission: Boolean = true, private val fix: Fix? = null) : Locator {
        var located = false

        override fun hasPermission() = permission

        override suspend fun locate(maxLastKnownAge: Duration): Fix? {
            located = true
            return fix
        }
    }

    private fun updateCurrentLocation(locator: Locator, repo: CurrentLocationRepository) =
        UpdateCurrentLocation(locator, repo, ForecastCacher(root.root, appVersionName = "test"))

    @Test
    fun `locates when there is no current location`() {
        assertTrue(shouldLocate(current = null, now = updatedAt))
    }

    @Test
    fun `locates when enough time passed`() {
        assertTrue(shouldLocate(currentAt(barcelona), now = updatedAt + Duration.ofMinutes(30)))
    }

    @Test
    fun `does not locate when updated recently`() {
        assertFalse(shouldLocate(currentAt(barcelona), now = updatedAt + Duration.ofMinutes(29)))
    }

    @Test
    fun `moved when far away`() {
        assertTrue(hasMoved(currentAt(barcelona), fix = sabadell))
    }

    @Test
    fun `not moved when nearby`() {
        assertFalse(hasMoved(currentAt(barcelona), fix = badalona))
    }

    @Test
    fun `longitude distance shrinks away from the equator`() {
        // 0.2 degrees of longitude is ~22 km at the equator, but ~11 km at 60 degrees latitude
        assertTrue(hasMoved(currentAt(Coordinates(0.0, 0.0)), fix = Coordinates(0.0, 0.2)))
        assertFalse(hasMoved(currentAt(Coordinates(60.0, 0.0)), fix = Coordinates(60.0, 0.2)))
    }

    @Test
    fun `stores the first fix`() = runTest {
        val repo = CurrentLocationRepository(root.root)
        updateCurrentLocation(FakeLocator(fix = Fix(barcelona, updatedAt)), repo)(now = updatedAt)
        val current = repo.getCurrentLocation()!!
        assertEquals(barcelona, current.location.coordinates)
        assertEquals(updatedAt, current.updatedAt)
        assertNull(repo.getError())
    }

    @Test
    fun `does not locate when the current location is recent`() = runTest {
        val repo = CurrentLocationRepository(root.root).apply { setCurrentLocation(currentAt(barcelona)) }
        val locator = FakeLocator(fix = Fix(sabadell, updatedAt + Duration.ofMinutes(10)))
        updateCurrentLocation(locator, repo)(now = updatedAt + Duration.ofMinutes(10))
        assertFalse(locator.located)
        assertEquals(currentAt(barcelona), repo.getCurrentLocation())
    }

    @Test
    fun `keeps a nearby location but marks it as up to date`() = runTest {
        val repo = CurrentLocationRepository(root.root).apply { setCurrentLocation(currentAt(barcelona)) }
        updateCurrentLocation(FakeLocator(fix = Fix(badalona, later)), repo)(now = later)
        assertEquals(currentAt(barcelona).copy(updatedAt = later), repo.getCurrentLocation())
    }

    @Test
    fun `moves to a far away location and deletes the previous forecast`() = runTest {
        val repo = CurrentLocationRepository(root.root).apply { setCurrentLocation(currentAt(barcelona)) }
        val previousForecast = File(root.newFolder("forecasts"), barcelona.id).apply { writeText("{}") }
        updateCurrentLocation(FakeLocator(fix = Fix(sabadell, later)), repo)(now = later)
        assertEquals(sabadell, repo.getCurrentLocation()!!.location.coordinates)
        assertFalse(previousForecast.exists())
    }

    @Test
    fun `errors when permission was revoked`() = runTest {
        val repo = CurrentLocationRepository(root.root).apply { setCurrentLocation(currentAt(barcelona)) }
        updateCurrentLocation(FakeLocator(permission = false), repo)(now = later)
        assertEquals(CurrentLocationError.PermissionDenied, repo.getError())
        assertEquals(currentAt(barcelona), repo.getCurrentLocation())
    }

    @Test
    fun `errors when there is no fix`() = runTest {
        val repo = CurrentLocationRepository(root.root)
        updateCurrentLocation(FakeLocator(fix = null), repo)(now = later)
        assertEquals(CurrentLocationError.Unavailable, repo.getError())
        assertNull(repo.getCurrentLocation())
    }

    @Test
    fun `errors when the fix is older than the current location`() = runTest {
        val repo = CurrentLocationRepository(root.root).apply { setCurrentLocation(currentAt(barcelona)) }
        updateCurrentLocation(FakeLocator(fix = Fix(sabadell, updatedAt - Duration.ofHours(1))), repo)(now = later)
        assertEquals(CurrentLocationError.Unavailable, repo.getError())
        assertEquals(currentAt(barcelona), repo.getCurrentLocation())
    }

    @Test
    fun `clears the error once updated`() = runTest {
        val repo = CurrentLocationRepository(root.root).apply {
            setCurrentLocation(currentAt(barcelona))
            setError(CurrentLocationError.Unavailable)
        }
        updateCurrentLocation(FakeLocator(fix = Fix(badalona, later)), repo)(now = later)
        assertNull(repo.getError())
    }
}
