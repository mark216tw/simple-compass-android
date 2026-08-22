package com.status.simplecompass.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class LocationPermission {
    NONE,
    APPROXIMATE,
    PRECISE,
}

data class CompassLocation(
    val permission: LocationPermission = LocationPermission.NONE,
    val permissionDenied: Boolean = false,
    val locationEnabled: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val declinationDegrees: Float? = null,
    val updatedAtMillis: Long? = null,
)

class CompassLocationManager(context: Context) :
    LocationListener,
    DefaultLifecycleObserver {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _location = MutableStateFlow(CompassLocation())
    val location: StateFlow<CompassLocation> = _location

    private var lifecycleStarted = false
    private var lastLocation: Location? = null

    override fun onStart(owner: LifecycleOwner) {
        lifecycleStarted = true
        refreshPermission()
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleStarted = false
        stopUpdates()
    }

    fun hasLocationPermission(): Boolean = currentPermission() != LocationPermission.NONE

    fun refreshPermission(requestCompleted: Boolean = false) {
        val permission = currentPermission()
        _location.value = _location.value.copy(
            permission = permission,
            permissionDenied = permission == LocationPermission.NONE &&
                (requestCompleted || _location.value.permissionDenied),
            locationEnabled = isLocationEnabled(),
        )
        if (lifecycleStarted && permission != LocationPermission.NONE) startUpdates()
        else stopUpdates()
    }

    override fun onLocationChanged(location: Location) {
        if (!isBetterLocation(location, lastLocation)) return
        lastLocation = location

        val altitude = location.takeIf(Location::hasAltitude)?.altitude
        val timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        val magneticField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            (altitude ?: 0.0).toFloat(),
            timestamp,
        )
        _location.value = _location.value.copy(
            locationEnabled = true,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = altitude,
            accuracyMeters = location.takeIf(Location::hasAccuracy)?.accuracy,
            declinationDegrees = magneticField.declination,
            updatedAtMillis = timestamp,
        )
    }

    override fun onProviderEnabled(provider: String) {
        _location.value = _location.value.copy(locationEnabled = isLocationEnabled())
        if (lifecycleStarted && hasLocationPermission()) startUpdates()
    }

    override fun onProviderDisabled(provider: String) {
        _location.value = _location.value.copy(locationEnabled = isLocationEnabled())
    }

    @Deprecated("Deprecated by Android")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    private fun startUpdates() {
        stopUpdates()
        val permission = currentPermission()
        val enabledProviders = buildList {
            if (permission == LocationPermission.PRECISE && isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
        _location.value = _location.value.copy(
            permission = permission,
            permissionDenied = false,
            locationEnabled = enabledProviders.isNotEmpty(),
        )

        try {
            enabledProviders.forEach { provider ->
                locationManager.getLastKnownLocation(provider)?.let(::onLocationChanged)
                locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_INTERVAL_MILLIS,
                    LOCATION_DISTANCE_METERS,
                    this,
                    Looper.getMainLooper(),
                )
            }
        } catch (_: SecurityException) {
            stopUpdates()
            _location.value = _location.value.copy(permission = currentPermission())
        }
    }

    private fun stopUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (_: SecurityException) {
            // Permission may be revoked while the app is running.
        }
    }

    private fun currentPermission(): LocationPermission = when {
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED -> LocationPermission.PRECISE
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED -> LocationPermission.APPROXIMATE
        else -> LocationPermission.NONE
    }

    private fun isLocationEnabled(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    } catch (_: Exception) {
        false
    }

    private fun isProviderEnabled(provider: String): Boolean = try {
        locationManager.isProviderEnabled(provider)
    } catch (_: Exception) {
        false
    }

    private fun isBetterLocation(newLocation: Location, current: Location?): Boolean {
        current ?: return true
        val timeDelta = newLocation.time - current.time
        val significantlyNewer = timeDelta > TWO_MINUTES_MILLIS
        val significantlyOlder = timeDelta < -TWO_MINUTES_MILLIS
        if (significantlyNewer) return true
        if (significantlyOlder) return false

        val accuracyDelta = newLocation.accuracy - current.accuracy
        val moreAccurate = accuracyDelta < 0f
        val significantlyLessAccurate = accuracyDelta > 200f
        return moreAccurate ||
            (timeDelta > 0L && !significantlyLessAccurate && accuracyDelta <= 10f)
    }

    private companion object {
        const val LOCATION_INTERVAL_MILLIS = 15_000L
        const val LOCATION_DISTANCE_METERS = 10f
        const val TWO_MINUTES_MILLIS = 120_000L
    }
}
