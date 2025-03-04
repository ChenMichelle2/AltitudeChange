package com.example.altitudechange

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.altitudechange.ui.theme.AltitudeChangeTheme
import kotlin.math.pow

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null

    // Current real sensor reading
    private var currentPressure by mutableStateOf(1013.25f)  // Default sea-level pressure.

    // Whether to simulate pressure readings
    private var simulate by mutableStateOf(false)

    // Simulated pressure
    private var simulatedPressure by mutableStateOf(1013.25f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        setContent {
            AltitudeChangeTheme {
                AltitudeScreen(
                    realPressure = currentPressure,
                    simulate = simulate,
                    simulatedPressure = simulatedPressure,
                    onSimulateChanged = { simulate = it },
                    onSimulatedPressureChanged = { simulatedPressure = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener if present
        pressureSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listener to save battery
        sensorManager.unregisterListener(this)
    }

    // Called when the sensor detects a new reading
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // Only update if not simulating
            if (!simulate && it.sensor.type == Sensor.TYPE_PRESSURE) {
                currentPressure = it.values[0]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}

/**
 * Composable screen that shows:
 * - A toggle for simulation vs. real sensor
 * - A slider to adjust simulated pressure
 * - The current pressure reading
 * - The computed altitude
 * - A dynamic background color that gets darker at higher altitudes
 */
@Composable
fun AltitudeScreen(
    realPressure: Float,
    simulate: Boolean,
    simulatedPressure: Float,
    onSimulateChanged: (Boolean) -> Unit,
    onSimulatedPressureChanged: (Float) -> Unit
) {
    // If simulating, use the slider's pressure
    // Otherwise, use the real sensor reading
    val pressure = if (simulate) simulatedPressure else realPressure

    // Compute altitude using the formula
    val altitude = altitudeFromPressure(pressure)

    //change color based on altitude
    val fraction = (altitude / 10000f).coerceIn(0f, 1f)
    val bgColor = Color(
        red = 1f - fraction,
        green = 1f - fraction,
        blue = 1f - fraction
    )

    //UI
    Scaffold(
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Altimeter",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Simulate Pressure?")
                Switch(
                    checked = simulate,
                    onCheckedChange = onSimulateChanged
                )
            }

            if (simulate) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Simulated Pressure: ${simulatedPressure.format(2)} hPa")
                Slider(
                    value = simulatedPressure,
                    onValueChange = onSimulatedPressureChanged,
                    valueRange = 800f..1100f,
                    steps = 31
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Real Pressure: ${realPressure.format(2)} hPa")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Show computed altitude
            Text(
                text = "Altitude: ${altitude.format(2)} m",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Computes altitude from the given pressure using:
 *
 *   h = 44330 × [1 − (P / P0)^(1/5.255)]
 *
 * where P0 = 1013.25 hPa is standard sea-level pressure
 */
fun altitudeFromPressure(pressure: Float, p0: Float = 1013.25f): Float {
    if (pressure <= 0f) return 0f
    return 44330f * (1f - (pressure / p0).pow(1f / 5.255f))
}

/**
 * Helper function to format floats to a given number of decimal places
 */
fun Float.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}

@Preview(showBackground = true)
@Composable
fun PreviewAltitudeScreen() {
    AltitudeChangeTheme {
        AltitudeScreen(
            realPressure = 1013.25f,
            simulate = true,
            simulatedPressure = 900f,
            onSimulateChanged = {},
            onSimulatedPressureChanged = {}
        )
    }
}
