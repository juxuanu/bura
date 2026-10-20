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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.ZoneId

class CurrentLocationRepositoryTest {
    @get:Rule
    val root = TemporaryFolder()

    @Test
    fun `no current location before one is set`() = runTest {
        assertNull(CurrentLocationRepository(root.root).getCurrentLocation())
    }

    @Test
    fun `persists current location`() = runTest {
        val location = CurrentLocation(
            location = Location(ZoneId.of("Europe/Madrid"), Coordinates(41.39, 2.17)),
            updatedAt = Instant.ofEpochSecond(1_700_000_000)
        )
        CurrentLocationRepository(root.root).setCurrentLocation(location)
        assertEquals(location, CurrentLocationRepository(root.root).getCurrentLocation())
    }
}
