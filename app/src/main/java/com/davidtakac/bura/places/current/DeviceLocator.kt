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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.davidtakac.bura.places.Coordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import kotlin.coroutines.resume

private val maxLastKnownAge = Duration.ofMinutes(15)
private val fixTimeout = Duration.ofSeconds(10)

@SuppressLint("InlinedApi")
private val preferredProviders = listOf(
    LocationManager.FUSED_PROVIDER,
    LocationManager.NETWORK_PROVIDER,
    LocationManager.GPS_PROVIDER
)

class DeviceLocator(private val context: Context) {
    @SuppressLint("MissingPermission")
    suspend fun locate(): Coordinates? {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) return null

        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        // Only returns providers that are enabled and allowed by the granted permissions
        val providers = manager.getProviders(true)
        val lastKnown = providers
            .mapNotNull(manager::getLastKnownLocation)
            .maxByOrNull { it.elapsedRealtimeNanos }
        val location = lastKnown?.takeIf { it.age < maxLastKnownAge }
            ?: preferredProviders.firstOrNull(providers::contains)?.let { provider ->
                withTimeoutOrNull(fixTimeout.toMillis()) { manager.awaitCurrentLocation(provider) }
            }
            ?: lastKnown
        return location?.let { Coordinates(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun LocationManager.awaitCurrentLocation(provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            cont.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                this,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { cont.resume(it) }
        }

    private val Location.age: Duration
        get() = Duration.ofNanos(SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos)
}
