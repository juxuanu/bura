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

package com.davidtakac.bura.places.saved.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.davidtakac.bura.R
import com.davidtakac.bura.common.compose.relativeTimeString
import com.davidtakac.bura.theme.AppTheme
import java.time.Instant

@Composable
fun CurrentLocationItem(
    selected: Boolean,
    updatedAt: Instant?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .then(modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = if (selected) R.drawable.location_on else R.drawable.my_location),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(16.dp)
            )
            Text(
                text = stringResource(id = R.string.place_picker_automatic_location),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        updatedAt?.let {
            Text(
                text = relativeTimeString(it),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

@Preview
@Composable
private fun CurrentLocationItemPreview() {
    AppTheme(darkTheme = true) {
        Surface(tonalElevation = 3.dp) {
            CurrentLocationItem(
                selected = false,
                updatedAt = Instant.now().minusSeconds(120),
                onClick = {},
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}
