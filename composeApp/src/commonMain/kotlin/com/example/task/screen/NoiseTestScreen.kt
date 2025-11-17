package com.example.task.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.task.viewmodel.MainViewModel
import com.example.task.viewmodel.NoiseTestViewModel
import com.example.task.viewmodel.TestState
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.*
import androidx.compose.runtime.remember // <-- ADDED IMPORT

// Max value for the gauge
private const val GAUGE_MAX_DB = 60f
private const val THRESHOLD_DB = 40f

// FIX: Factory function to ensure the ViewModel instance is remembered across recompositions
@Composable
fun rememberNoiseTestViewModel(mainViewModel: MainViewModel): NoiseTestViewModel {
    return remember { NoiseTestViewModel(mainViewModel = mainViewModel) }
}

@Composable
fun NoiseTestScreen(mainViewModel: MainViewModel) {
    // FIX APPLIED: Use the factory function to get a retained ViewModel instance.
    val viewModel = rememberNoiseTestViewModel(mainViewModel = mainViewModel)

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Title Area
        Text(
            text = "Test Ambient Noise Level",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = uiState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (uiState.isTestSuccessful == false) Color.Red else Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Decibel Meter Gauge
        DecibelMeterGauge(
            liveDb = uiState.liveDb,
            // Show average only after test completes
            displayDb = if (uiState.state == TestState.COMPLETED) uiState.avgDb ?: 0f else uiState.liveDb,
            state = uiState.state
        )

        Spacer(Modifier.weight(1f))

        // Control Button
        Button(
            onClick = {
                if (uiState.state == TestState.IDLE || uiState.isTestSuccessful == false) {
                    viewModel.startTest()
                }
            },
            enabled = uiState.state == TestState.IDLE || uiState.isTestSuccessful == false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = when (uiState.state) {
                    TestState.IDLE -> "Start Test"
                    TestState.RUNNING -> "Testing..."
                    TestState.ANALYZING -> "Analyzing..."
                    TestState.COMPLETED -> if (uiState.isTestSuccessful == true) "Test Passed" else "Try Again"
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// Custom Composable for the Gauge Meter UI
@Composable
fun DecibelMeterGauge(
    liveDb: Float,
    displayDb: Float,
    state: TestState,
    modifier: Modifier = Modifier.size(300.dp)
) {
    val animatedDb by animateFloatAsState(targetValue = displayDb, label = "dbAnimation")
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val sweepAngle = 270f
        val startAngle = 135f
        val gaugeWidth = 20.dp.toPx()
        val radius = size.minDimension / 2 - gaugeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Define colors for the gauge (Green/Blue -> Yellow -> Red)
        val safeColor = Color(0xFF4CAF50) // Green
        val warningColor = Color(0xFFFFC107) // Yellow
        val dangerColor = Color(0xFFF44336) // Red

        val gaugeBrush = Brush.sweepGradient(
            0.0f to safeColor,
            0.66f to safeColor, // 40dB/60dB * 270/360 = 0.66
            0.66f to dangerColor,
            1.0f to dangerColor
        )

        // 1. Draw Background Arc
        drawArc(
            color = Color.LightGray.copy(alpha = 0.5f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = gaugeWidth, cap = StrokeCap.Round)
        )

        // 2. Draw Active/Color Arc
        val activeSweep = (animatedDb / GAUGE_MAX_DB) * sweepAngle
        drawArc(
            brush = gaugeBrush,
            startAngle = startAngle,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = gaugeWidth, cap = StrokeCap.Round)
        )

        // 3. Draw Needle (representing liveDb for better visual feedback during testing)
        if (state == TestState.RUNNING || state == TestState.IDLE) {
            val needleAngle = startAngle + (liveDb / GAUGE_MAX_DB) * sweepAngle
            val angleRadians = needleAngle.toDouble() * (PI / 180.0) // Convert Degrees to Radians
            val needleLength = radius * 0.8f

            // FIX: Explicitly convert all components to Double for calculation robustness
            // then convert the final coordinate back to Float.
            val endX = (center.x.toDouble() + needleLength.toDouble() * cos(angleRadians)).toFloat()
            val endY = (center.y.toDouble() + needleLength.toDouble() * sin(angleRadians)).toFloat()

            drawLine(
                color = Color.DarkGray,
                start = center,
                end = Offset(x = endX, y = endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Center Dot
            drawCircle(color = Color.DarkGray, radius = 5.dp.toPx(), center = center)
        }

        // 4. Draw Labels (0, 10, 20, 30, 40, 50, 60)
        for (i in 0..6) {
            val dbValue = i * 10
            val labelAngle = startAngle + (dbValue / GAUGE_MAX_DB) * sweepAngle
            val angleRadians = labelAngle.toDouble() * (PI / 180.0) // Convert Degrees to Radians
            val labelRadius = radius + 15.dp.toPx()

            // Re-apply the explicit Double calculation for labels as well, just to be safe
            val labelX = (center.x.toDouble() + labelRadius.toDouble() * cos(angleRadians)).toFloat()
            val labelY = (center.y.toDouble() + labelRadius.toDouble() * sin(angleRadians)).toFloat()

            val labelText = textMeasurer.measure(
                buildAnnotatedString { append("$dbValue") }
            )

            drawText(
                textLayoutResult = labelText,
                color = Color.Black,
                topLeft = Offset(
                    x = labelX - labelText.size.width / 2,
                    y = labelY - labelText.size.height / 2
                )
            )
        }

        // 5. Digital Display in Center
        // This part correctly uses the animated value for smooth transitions.
        val displayValue = animatedDb.roundToInt().toString()

        val valueText = textMeasurer.measure(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.Black)) {
                    append(displayValue)
                }
            }
        )
        val unitText = textMeasurer.measure(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 18.sp, color = Color.Gray)) {
                    append("dB")
                }
            }
        )

        drawText(
            textLayoutResult = valueText,
            topLeft = Offset(
                x = center.x - valueText.size.width / 2,
                y = center.y - 10.dp.toPx()
            )
        )
        drawText(
            textLayoutResult = unitText,
            topLeft = Offset(
                x = center.x - unitText.size.width / 2,
                y = center.y + valueText.size.height - 10.dp.toPx()
            )
        )
    }
}