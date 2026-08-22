package com.status.simplecompass

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.status.simplecompass.data.AppSettings
import com.status.simplecompass.data.NorthMode
import com.status.simplecompass.data.SettingsRepository
import com.status.simplecompass.data.ThemeMode
import com.status.simplecompass.data.ThemePalette
import com.status.simplecompass.location.CompassLocationManager
import com.status.simplecompass.sensor.CompassSensorManager
import com.status.simplecompass.ui.CompassApp
import com.status.simplecompass.ui.theme.SimpleCompassTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var compassSensorManager: CompassSensorManager
    private lateinit var compassLocationManager: CompassLocationManager
    private lateinit var settingsRepository: SettingsRepository
    private var pendingTrueNorth = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        compassLocationManager.refreshPermission(requestCompleted = true)
        if (pendingTrueNorth) {
            setNorthModeDirect(
                if (compassLocationManager.hasLocationPermission()) NorthMode.TRUE
                else NorthMode.MAGNETIC,
            )
        }
        pendingTrueNorth = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(applicationContext)
        compassLocationManager = CompassLocationManager(applicationContext)
        compassSensorManager = CompassSensorManager(applicationContext) {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        lifecycle.addObserver(compassSensorManager)
        lifecycle.addObserver(compassLocationManager)

        setContent {
            val settings = settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            ).value
            val reading = compassSensorManager.reading.collectAsStateWithLifecycle().value
            val location = compassLocationManager.location.collectAsStateWithLifecycle().value

            SimpleCompassTheme(
                themeMode = settings.themeMode,
                palette = settings.themePalette,
                window = window,
            ) {
                CompassApp(
                    reading = reading,
                    location = location,
                    settings = settings,
                    onThemeModeChanged = ::setThemeMode,
                    onThemePaletteChanged = ::setThemePalette,
                    onKeepScreenOnChanged = ::setKeepScreenOn,
                    onNorthModeChanged = ::setNorthMode,
                    onRequestLocation = ::requestLocationAccess,
                    onNorthHapticsChanged = ::setNorthHaptics,
                )
            }
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        lifecycleScope.launch { settingsRepository.setThemeMode(mode) }
    }

    private fun setThemePalette(palette: ThemePalette) {
        lifecycleScope.launch { settingsRepository.setThemePalette(palette) }
    }

    private fun setKeepScreenOn(enabled: Boolean) {
        lifecycleScope.launch { settingsRepository.setKeepScreenOn(enabled) }
    }

    private fun setNorthMode(mode: NorthMode) {
        if (mode == NorthMode.MAGNETIC || compassLocationManager.hasLocationPermission()) {
            setNorthModeDirect(mode)
            return
        }
        pendingTrueNorth = true
        launchLocationPermissionRequest()
    }

    private fun requestLocationAccess() {
        if (compassLocationManager.hasLocationPermission()) {
            if (!compassLocationManager.location.value.locationEnabled) {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                compassLocationManager.refreshPermission()
            }
            return
        }
        pendingTrueNorth = false
        launchLocationPermissionRequest()
    }

    private fun launchLocationPermissionRequest() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun setNorthModeDirect(mode: NorthMode) {
        lifecycleScope.launch { settingsRepository.setNorthMode(mode) }
    }

    private fun setNorthHaptics(enabled: Boolean) {
        lifecycleScope.launch { settingsRepository.setNorthHapticsEnabled(enabled) }
    }
}
