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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class UpdateCurrentLocationTest {
    private val barcelona = Coordinates(41.39, 2.17)
    // ~18 km from Barcelona
    private val sabadell = Coordinates(41.55, 2.11)
    // ~9 km from Barcelona
    private val badalona = Coordinates(41.45, 2.25)
    private val updatedAt = Instant.ofEpochSecond(0)

    private fun currentAt(coordinates: Coordinates) =
        CurrentLocation(Location(ZoneId.of("Europe/Madrid"), coordinates), updatedAt)

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
}
