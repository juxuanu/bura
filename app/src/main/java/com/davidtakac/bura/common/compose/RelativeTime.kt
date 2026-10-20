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

package com.davidtakac.bura.common.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.davidtakac.bura.R
import java.time.Duration
import java.time.Instant

@Composable
fun relativeTimeString(time: Instant): String {
    val age = Duration.between(time, Instant.now())
    return when {
        age.toMinutes() < 1 -> stringResource(R.string.general_time_now)
        age.toHours() < 1 -> stringResource(R.string.general_time_value_minutes_ago, age.toMinutes())
        age.toDays() < 1 -> stringResource(R.string.general_time_value_hours_ago, age.toHours())
        else -> stringResource(R.string.general_time_value_days_ago, age.toDays())
    }
}
