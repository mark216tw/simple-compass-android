package com.status.simplecompass.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.status.simplecompass.R
import com.status.simplecompass.data.AppSettings
import com.status.simplecompass.data.NorthMode
import com.status.simplecompass.data.ThemeMode
import com.status.simplecompass.data.ThemePalette
import com.status.simplecompass.location.CompassLocation
import com.status.simplecompass.location.LocationPermission
import com.status.simplecompass.sensor.CompassMath
import com.status.simplecompass.sensor.CompassReading
import com.status.simplecompass.sensor.CompassStatus
import com.status.simplecompass.sensor.NorthAlignmentGate
import com.status.simplecompass.ui.theme.palettePreviewColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassApp(
    reading: CompassReading,
    location: CompassLocation,
    settings: AppSettings,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onThemePaletteChanged: (ThemePalette) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onNorthModeChanged: (NorthMode) -> Unit,
    onRequestLocation: () -> Unit,
    onNorthHapticsChanged: (Boolean) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val useTrueNorth = settings.northMode == NorthMode.TRUE &&
        location.permission != LocationPermission.NONE &&
        location.declinationDegrees != null
    val displayedHeading = if (useTrueNorth) {
        CompassMath.trueHeading(reading.headingDegrees, location.declinationDegrees ?: 0f)
    } else {
        reading.headingDegrees
    }

    KeepScreenOn(settings.keepScreenOn)
    NorthHapticEffect(
        headingDegrees = displayedHeading,
        enabled = settings.northHapticsEnabled,
        canTrigger = reading.hasReading && reading.isLevel,
        northMode = if (useTrueNorth) NorthMode.TRUE else NorthMode.MAGNETIC,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        onThemeModeChanged(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                    },
                ) {
                    Text(
                        stringResource(
                            if (darkTheme) R.string.quick_theme_light
                            else R.string.quick_theme_dark,
                        ),
                    )
                }
                TextButton(onClick = { showSettings = true }) {
                    Text(stringResource(R.string.settings))
                }
            }
        },
    ) { paddingValues ->
        CompassContent(
            reading = reading,
            location = location,
            displayedHeading = displayedHeading,
            selectedNorthMode = settings.northMode,
            useTrueNorth = useTrueNorth,
            onNorthModeChanged = onNorthModeChanged,
            onRequestLocation = onRequestLocation,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        )
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            darkTheme = darkTheme,
            onDismiss = { showSettings = false },
            onThemePaletteChanged = onThemePaletteChanged,
            onKeepScreenOnChanged = onKeepScreenOnChanged,
            onNorthHapticsChanged = onNorthHapticsChanged,
        )
    }
}

@Composable
private fun CompassContent(
    reading: CompassReading,
    location: CompassLocation,
    displayedHeading: Float,
    selectedNorthMode: NorthMode,
    useTrueNorth: Boolean,
    onNorthModeChanged: (NorthMode) -> Unit,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = stringResource(directionResource(displayedHeading))

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.heading_format, displayedHeading),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = direction,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
            )
        }
        Text(
            text = stringResource(
                if (useTrueNorth) R.string.heading_true_north else R.string.heading_magnetic_north,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NorthModeSelector(
            selectedMode = selectedNorthMode,
            onModeChanged = onNorthModeChanged,
        )

        CompassDial(
            headingDegrees = displayedHeading,
            pitchDegrees = reading.pitchDegrees,
            rollDegrees = reading.rollDegrees,
            description = stringResource(
                R.string.compass_description,
                direction,
                displayedHeading,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        LocationInfo(location = location, onRequestLocation = onRequestLocation)
        Spacer(Modifier.height(8.dp))
        SensorStatus(status = reading.status)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun NorthModeSelector(
    selectedMode: NorthMode,
    onModeChanged: (NorthMode) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.74f)
            .padding(top = 6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            NorthModeOption(
                label = stringResource(R.string.magnetic_north),
                selected = selectedMode == NorthMode.MAGNETIC,
                onClick = { onModeChanged(NorthMode.MAGNETIC) },
                modifier = Modifier.weight(1f),
            )
            NorthModeOption(
                label = stringResource(R.string.true_north),
                selected = selectedMode == NorthMode.TRUE,
                onClick = { onModeChanged(NorthMode.TRUE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NorthModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    if (selected) R.string.current_selection else R.string.tap_to_switch,
                ),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SensorStatus(status: CompassStatus) {
    val isWarning = status == CompassStatus.UNAVAILABLE ||
        status == CompassStatus.CALIBRATION ||
        status == CompassStatus.INTERFERENCE
    val message = stringResource(
        when (status) {
            CompassStatus.UNAVAILABLE -> R.string.sensor_unavailable
            CompassStatus.LOADING -> R.string.sensor_loading
            CompassStatus.INTERFERENCE -> R.string.sensor_interference
            CompassStatus.CALIBRATION -> R.string.sensor_calibration
            CompassStatus.READY -> R.string.sensor_ready
        },
    )
    val statusColor = if (isWarning) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.secondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        color = statusColor.copy(alpha = if (status == CompassStatus.READY) 0.07f else 0.12f),
        contentColor = statusColor,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun LocationInfo(location: CompassLocation, onRequestLocation: () -> Unit) {
    val hasCoordinates = location.latitude != null && location.longitude != null
    val prompt = when {
        location.permission == LocationPermission.NONE && location.permissionDenied ->
            stringResource(R.string.location_permission_denied)
        location.permission == LocationPermission.NONE -> stringResource(R.string.location_enable)
        !location.locationEnabled -> stringResource(R.string.location_service_disabled)
        !hasCoordinates -> stringResource(R.string.location_loading)
        else -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = prompt != null, onClick = onRequestLocation),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (prompt != null) {
            Text(
                text = prompt,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.coordinates),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.coordinates_format,
                            location.latitude ?: 0.0,
                            location.longitude ?: 0.0,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.altitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = location.altitudeMeters?.let {
                            stringResource(R.string.altitude_format, it)
                        } ?: stringResource(R.string.value_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassDial(
    headingDegrees: Float,
    pitchDegrees: Float,
    rollDegrees: Float,
    description: String,
    modifier: Modifier = Modifier,
) {
    val dialColor = MaterialTheme.colorScheme.onBackground
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val northColor = MaterialTheme.colorScheme.primary
    val levelColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .semantics { contentDescription = description },
        ) {
            val center = this.center
            val radius = (size.minDimension / 2f - 12.dp.toPx()).coerceAtLeast(1f)

            drawCircle(color = surfaceColor, radius = radius)
            drawCircle(
                color = mutedColor.copy(alpha = 0.55f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = mutedColor.copy(alpha = 0.22f),
                radius = radius * 0.72f,
                style = Stroke(width = 1.dp.toPx()),
            )

            rotate(degrees = -headingDegrees, pivot = center) {
                for (index in 0 until 72) {
                    val angle = index * 5f
                    val radians = (angle - 90f) * PI.toFloat() / 180f
                    val isMajor = index % 6 == 0
                    val isMedium = index % 2 == 0
                    val tickLength = when {
                        isMajor -> 18.dp.toPx()
                        isMedium -> 11.dp.toPx()
                        else -> 6.dp.toPx()
                    }
                    val outerRadius = radius - 7.dp.toPx()
                    val outer = Offset(
                        center.x + cos(radians) * outerRadius,
                        center.y + sin(radians) * outerRadius,
                    )
                    val inner = Offset(
                        center.x + cos(radians) * (outerRadius - tickLength),
                        center.y + sin(radians) * (outerRadius - tickLength),
                    )
                    drawLine(
                        color = if (index == 0) northColor else mutedColor,
                        start = inner,
                        end = outer,
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = with(density) { 22.sp.toPx() }
                }
                listOf("N", "E", "S", "W").forEachIndexed { index, label ->
                    val radians = (index * 90f - 90f) * PI.toFloat() / 180f
                    val labelRadius = radius * 0.72f
                    textPaint.color = if (index == 0) northColor.toArgb() else dialColor.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        center.x + cos(radians) * labelRadius,
                        center.y + sin(radians) * labelRadius + textPaint.textSize * 0.35f,
                        textPaint,
                    )
                }

                drawLine(
                    color = northColor.copy(alpha = 0.65f),
                    start = Offset(center.x, center.y - radius * 0.43f),
                    end = Offset(center.x, center.y - radius * 0.15f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val pointerTop = center.y - radius - 3.dp.toPx()
            val pointer = Path().apply {
                moveTo(center.x, pointerTop)
                lineTo(center.x - 9.dp.toPx(), pointerTop + 18.dp.toPx())
                lineTo(center.x + 9.dp.toPx(), pointerTop + 18.dp.toPx())
                close()
            }
            drawPath(pointer, northColor)

            val levelRadius = 29.dp.toPx().coerceAtMost(radius * 0.22f)
            val travel = levelRadius * 0.56f
            val bubbleOffset = Offset(
                x = (-rollDegrees / 12f).coerceIn(-1f, 1f) * travel,
                y = (pitchDegrees / 12f).coerceIn(-1f, 1f) * travel,
            )
            val bubbleCentered = abs(pitchDegrees) <= 1.5f && abs(rollDegrees) <= 1.5f
            drawCircle(
                color = mutedColor.copy(alpha = 0.55f),
                radius = levelRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = mutedColor.copy(alpha = 0.25f),
                radius = levelRadius * 0.48f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = if (bubbleCentered) levelColor else northColor,
                radius = levelRadius * 0.28f,
                center = center + bubbleOffset,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: AppSettings,
    darkTheme: Boolean,
    onDismiss: () -> Unit,
    onThemePaletteChanged: (ThemePalette) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onNorthHapticsChanged: (Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.color_palette), style = MaterialTheme.typography.titleMedium)
            val palettes = ThemePalette.entries.filter {
                it != ThemePalette.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }
            palettes.forEach { palette ->
                PaletteOption(
                    label = stringResource(paletteResource(palette)),
                    color = palettePreviewColor(palette, darkTheme),
                    selected = settings.themePalette == palette,
                    onClick = { onThemePaletteChanged(palette) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SettingSwitch(
                title = stringResource(R.string.keep_screen_on),
                description = stringResource(R.string.keep_screen_on_description),
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChanged,
            )
            SettingSwitch(
                title = stringResource(R.string.north_haptics),
                description = stringResource(R.string.north_haptics_description),
                checked = settings.northHapticsEnabled,
                onCheckedChange = onNorthHapticsChanged,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.privacy_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaletteOption(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(22.dp)) {}
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun NorthHapticEffect(
    headingDegrees: Float,
    enabled: Boolean,
    canTrigger: Boolean,
    northMode: NorthMode,
) {
    val view = LocalView.current
    val gate = remember(northMode) { NorthAlignmentGate() }
    LaunchedEffect(headingDegrees, enabled, canTrigger) {
        if (!enabled) {
            gate.reset()
            return@LaunchedEffect
        }
        if (gate.shouldTrigger(headingDegrees, canTrigger)) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}

@StringRes
private fun directionResource(degrees: Float): Int = when (CompassMath.directionIndex(degrees)) {
    0 -> R.string.direction_north
    1 -> R.string.direction_northeast
    2 -> R.string.direction_east
    3 -> R.string.direction_southeast
    4 -> R.string.direction_south
    5 -> R.string.direction_southwest
    6 -> R.string.direction_west
    else -> R.string.direction_northwest
}

@StringRes
private fun paletteResource(palette: ThemePalette): Int = when (palette) {
    ThemePalette.CLASSIC -> R.string.palette_classic
    ThemePalette.OCEAN -> R.string.palette_ocean
    ThemePalette.FOREST -> R.string.palette_forest
    ThemePalette.SUNSET -> R.string.palette_sunset
    ThemePalette.HIGH_CONTRAST -> R.string.palette_high_contrast
    ThemePalette.DYNAMIC -> R.string.palette_dynamic
}
