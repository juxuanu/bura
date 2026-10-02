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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCurrentLocationTest {
    @Test
    fun `first fix is rounded to two decimals`() {
        assertEquals(
            Coordinates(45.55, 18.69),
            nextCurrentLocation(current = null, fix = Coordinates(45.55111, 18.69389))
        )
    }

    @Test
    fun `keeps current location when fix is nearby`() {
        assertNull(nextCurrentLocation(current = Coordinates(45.55, 18.69), fix = Coordinates(45.56123, 18.70456)))
    }

    @Test
    fun `moves to fix when it is far away`() {
        assertEquals(
            Coordinates(45.81, 15.98),
            nextCurrentLocation(current = Coordinates(45.55, 18.69), fix = Coordinates(45.81444, 15.97798))
        )
    }

    @Test
    fun `longitude distance shrinks away from the equator`() {
        // 0.08 degrees of longitude is ~8.9 km at the equator, but ~4.4 km at 60 degrees latitude
        assertEquals(
            Coordinates(0.0, 0.08),
            nextCurrentLocation(current = Coordinates(0.0, 0.0), fix = Coordinates(0.0, 0.08))
        )
        assertNull(nextCurrentLocation(current = Coordinates(60.0, 0.0), fix = Coordinates(60.0, 0.08)))
    }
}
