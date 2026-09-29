package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.HistogramData
import com.example.camera.RecordingStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Indicator4K
import com.example.ui.theme.PeakingGreen
import com.example.ui.theme.ShutterRed
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceGlass
import com.example.ui.theme.TelemetryZebra
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x99000000),
    textColor: Color = AccentGold,
    borderColor: Color = Color(0x44FFFFFF)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LiveHistogramOverlay(
    histogramData: HistogramData,
    modifier: Modifier = Modifier,
    isZebraWarning: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB3090C10))
            .border(1.dp, if (isZebraWarning) TelemetryZebra else StudioBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTOGRAM (RGB/LUM)",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (histogramData.clippingHighlightRatio > 0.05f) {
                    Text(
                        text = "ZEBRA ${(histogramData.clippingHighlightRatio * 100).toInt()}%",
                        color = TelemetryZebra,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                val width = size.width
                val height = size.height
                val bins = histogramData.bins
                val binWidth = width / bins.size.toFloat()

                // Draw gradient background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x2238BDF8), Color(0x05000000))
                    )
                )

                // Draw bins
                for (i in bins.indices) {
                    val binHeight = (bins[i] * height).coerceIn(0f, height)
                    val x = i * binWidth
                    val y = height - binHeight

                    val barColor = when {
                        i < 16 -> AccentCyan.copy(alpha = 0.8f) // Shadows
                        i < 48 -> AccentGreen.copy(alpha = 0.85f) // Midtones
                        else -> AccentAmber.copy(alpha = 0.9f) // Highlights
                    }

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(binWidth.coerceAtLeast(1f), binHeight)
                    )
                }

                // Draw Grid line
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(0f, height / 2),
                    end = Offset(width, height / 2),
                    strokeWidth = 1f
                )
            }
        }
    }
}

@Composable
fun RuleOfThirdsGrid(
    modifier: Modifier = Modifier,
    showAnamorphicGuides: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 3x3 Rule of Thirds
        val col1 = width / 3f
        val col2 = (width / 3f) * 2f
        val row1 = height / 3f
        val row2 = (height / 3f) * 2f

        val gridPaintColor = Color(0x33FFFFFF)
        val strokeWidth = 1.dp.toPx()

        drawLine(gridPaintColor, Offset(col1, 0f), Offset(col1, height), strokeWidth)
        drawLine(gridPaintColor, Offset(col2, 0f), Offset(col2, height), strokeWidth)
        drawLine(gridPaintColor, Offset(0f, row1), Offset(width, row1), strokeWidth)
        drawLine(gridPaintColor, Offset(0f, row2), Offset(width, row2), strokeWidth)

        // Center crosshair
        val cx = width / 2f
        val cy = height / 2f
        val chLen = 14.dp.toPx()
        drawLine(Color(0x66FFB703), Offset(cx - chLen, cy), Offset(cx + chLen, cy), 1.5f)
        drawLine(Color(0x66FFB703), Offset(cx, cy - chLen), Offset(cx, cy + chLen), 1.5f)

        // Anamorphic 20:9 letterbox guides if requested
        if (showAnamorphicGuides) {
            val letterboxHeight = (height - (width * 9f / 20f)) / 2f
            if (letterboxHeight > 0) {
                drawRect(
                    color = Color(0x88000000),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(width, letterboxHeight)
                )
                drawRect(
                    color = Color(0x88000000),
                    topLeft = Offset(0f, height - letterboxHeight),
                    size = androidx.compose.ui.geometry.Size(width, letterboxHeight)
                )
                drawLine(
                    color = Indicator4K.copy(alpha = 0.5f),
                    start = Offset(0f, letterboxHeight),
                    end = Offset(width, letterboxHeight),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawLine(
                    color = Indicator4K.copy(alpha = 0.5f),
                    start = Offset(0f, height - letterboxHeight),
                    end = Offset(width, height - letterboxHeight),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
    }
}

@Composable
fun ShutterControlCluster(
    recordingStatus: RecordingStatus,
    onRecordToggle: () -> Unit,
    onPauseResume: () -> Unit,
    onLensFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = recordingStatus == RecordingStatus.RECORDING
    val isPaused = recordingStatus == RecordingStatus.PAUSED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause / Resume Button (Visible when recording or paused)
        if (isRecording || isPaused) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(StudioSurfaceGlass)
                    .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                    .clickable(onClick = onPauseResume)
                    .testTag("pause_resume_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                    tint = if (isPaused) AccentGreen else AccentAmber,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(52.dp))
        }

        // Primary Large Shutter Button
        Box(
            modifier = Modifier
                .size(84.dp)
                .testTag("main_shutter_button"),
            contentAlignment = Alignment.Center
        ) {
            // Outer Ring
            Box(
                modifier = Modifier
                    .size(if (isRecording) 84.dp * pulseScale else 84.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color(0x33EF233C) else Color(0x22FFFFFF))
                    .border(
                        width = 3.dp,
                        color = if (isRecording) AccentRed else Color.White,
                        shape = CircleShape
                    )
            )

            // Inner Red Button
            Box(
                modifier = Modifier
                    .size(if (isRecording) 36.dp else 66.dp)
                    .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isRecording) listOf(AccentRed, Color(0xFFB91C1C))
                            else listOf(ShutterRed, Color(0xFF991B1B))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRecordToggle
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Lens Flip Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(StudioSurfaceGlass)
                .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                .clickable(enabled = !isRecording && !isPaused, onClick = onLensFlip)
                .testTag("flip_camera_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FlipCameraAndroid,
                contentDescription = "Switch Camera Lens",
                tint = if (!isRecording && !isPaused) TextPrimary else TextDisabled,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
