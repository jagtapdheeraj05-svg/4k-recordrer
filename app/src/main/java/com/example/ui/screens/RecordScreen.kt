package com.example.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.AspectRatioMode
import com.example.camera.CameraLens
import com.example.camera.RecordingStatus
import com.example.camera.SpecialMode
import com.example.camera.WhiteBalanceMode
import com.example.ui.MainViewModel
import com.example.ui.components.LiveHistogramOverlay
import com.example.ui.components.RuleOfThirdsGrid
import com.example.ui.components.ShutterControlCluster
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Indicator4K
import com.example.ui.theme.PeakingGreen
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceGlass
import com.example.ui.theme.TelemetryZebra
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val recordingStatus by viewModel.cameraManager.recordingStatus.collectAsStateWithLifecycle()
    val telemetry by viewModel.cameraManager.telemetry.collectAsStateWithLifecycle()
    val histogramData by viewModel.cameraManager.histogramData.collectAsStateWithLifecycle()
    val currentLens by viewModel.cameraManager.currentLens.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.cameraManager.isTorchOn.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.cameraManager.zoomRatio.collectAsStateWithLifecycle()
    val exposureIndex by viewModel.cameraManager.exposureIndex.collectAsStateWithLifecycle()
    val selectedMode by viewModel.cameraManager.selectedMode.collectAsStateWithLifecycle()
    val aspectRatio by viewModel.cameraManager.aspectRatio.collectAsStateWithLifecycle()
    val whiteBalance by viewModel.cameraManager.whiteBalance.collectAsStateWithLifecycle()
    val deviceSpec by viewModel.deviceSpec.collectAsStateWithLifecycle()
    val motionOffset by viewModel.sensorStabilizer.motionOffset.collectAsStateWithLifecycle()

    var showManualControls by remember { mutableStateOf(false) }
    var showHistogram by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var tapFocusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val isRearCamera = currentLens == CameraLens.REAR_MAIN

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview with Software EIS Motion Offset Transform
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                    viewModel.cameraManager.startCamera(lifecycleOwner, this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (motionOffset.isStabilizing) {
                        translationX = motionOffset.offsetX
                        translationY = motionOffset.offsetY
                        rotationZ = motionOffset.rotationDeg
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        tapFocusPoint = Pair(offset.x, offset.y)
                        previewViewRef?.let {
                            viewModel.cameraManager.tapToFocus(it, offset.x, offset.y)
                        }
                    }
                }
        )

        // Rule of Thirds & Anamorphic 20:9 Guides
        if (showGrid) {
            RuleOfThirdsGrid(
                showAnamorphicGuides = aspectRatio == AspectRatioMode.RATIO_20_9
            )
        }

        // Tap-to-Focus Ring Indicator
        tapFocusPoint?.let { (fx, fy) ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        translationX = fx - 32.dp.toPx()
                        translationY = fy - 32.dp.toPx()
                    }
                    .border(1.5.dp, AccentGold, CircleShape)
            )
        }

        // TOP HUD BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // Thermal & Low Storage Warning Banner
            if (deviceSpec.isThermalThrottling || deviceSpec.isStorageLow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCCEF233C))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (deviceSpec.isThermalThrottling) "Thermal alert: Auto-optimizing frame pipeline" else "Low storage: ${deviceSpec.freeStorageGB} GB remaining",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Top Control Icons & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolution / Sensor Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        text = if (isRearCamera) "4K UHD 50MP" else "1080P FHD",
                        textColor = if (isRearCamera) Indicator4K else AccentCyan,
                        backgroundColor = Color(0xB3090C10)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(
                        text = "20 Mbps",
                        textColor = AccentGold,
                        backgroundColor = Color(0xB3090C10)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (motionOffset.isStabilizing) {
                        StatusBadge(
                            text = "EIS-GYRO",
                            textColor = AccentGreen,
                            backgroundColor = Color(0xB3090C10)
                        )
                    }
                }

                // Top Quick Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Aspect ratio cycle button (16:9 -> 20:9 -> 4:3)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioSurfaceGlass)
                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                val nextRatio = when (aspectRatio) {
                                    AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_20_9
                                    AspectRatioMode.RATIO_20_9 -> AspectRatioMode.RATIO_4_3
                                    else -> AspectRatioMode.RATIO_16_9
                                }
                                viewModel.cameraManager.setAspectRatio(nextRatio)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("aspect_ratio_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (aspectRatio == AspectRatioMode.RATIO_20_9) "20:9" else if (aspectRatio == AspectRatioMode.RATIO_16_9) "16:9" else "4:3",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Torch Toggle
                    IconButton(
                        onClick = { viewModel.cameraManager.toggleTorch() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioSurfaceGlass)
                            .testTag("torch_button")
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) AccentGold else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Pro Manual Controls toggle
                    IconButton(
                        onClick = { showManualControls = !showManualControls },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (showManualControls) Color(0x66FFB703) else StudioSurfaceGlass)
                            .testTag("manual_controls_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Manual Controls",
                            tint = if (showManualControls) AccentGold else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Recording Active Timer Bar
            if (recordingStatus == RecordingStatus.RECORDING || recordingStatus == RecordingStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC090C10))
                        .border(1.dp, if (recordingStatus == RecordingStatus.RECORDING) AccentRed else AccentAmber, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (recordingStatus == RecordingStatus.RECORDING) AccentRed else AccentAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (recordingStatus == RecordingStatus.RECORDING) "REC" else "PAUSED",
                            color = if (recordingStatus == RecordingStatus.RECORDING) AccentRed else AccentAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = telemetry.formattedDuration,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = telemetry.formattedFileSize,
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Live Histogram & Zebra Clipping Overlay (Collapsible)
        if (showHistogram) {
            LiveHistogramOverlay(
                histogramData = histogramData,
                isZebraWarning = histogramData.clippingHighlightRatio > 0.05f,
                modifier = Modifier
                    .padding(start = 16.dp, top = 80.dp)
                    .width(150.dp)
                    .align(Alignment.TopStart)
            )
        }

        // RIGHT-SIDE MANUAL ADJUSTMENT STRIP
        AnimatedVisibility(
            visible = showManualControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xD9131720))
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .width(140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MANUAL PRO", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))

                // EV Slider (-2 to +2)
                Text("EV: ${if (exposureIndex > 0) "+$exposureIndex" else exposureIndex}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = exposureIndex.toFloat(),
                    onValueChange = { viewModel.cameraManager.setExposureCompensation(it.toInt()) },
                    valueRange = -2f..2f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold),
                    modifier = Modifier.height(28.dp).testTag("ev_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Zoom Slider (1x to 6x)
                Text("ZOOM: ${String.format("%.1fx", zoomRatio)}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = zoomRatio,
                    onValueChange = { viewModel.cameraManager.setZoom(it) },
                    valueRange = 1.0f..6.0f,
                    colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan),
                    modifier = Modifier.height(28.dp).testTag("zoom_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // White Balance Quick Selector
                Text("WB MODE", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                WhiteBalanceMode.entries.forEach { wb ->
                    val isWbSelected = whiteBalance == wb
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isWbSelected) Color(0x33FFB703) else Color(0x1AFFFFFF))
                            .clickable { viewModel.cameraManager.setWhiteBalanceMode(wb) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = wb.displayName.split(" ")[0],
                            color = if (isWbSelected) AccentGold else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // BOTTOM CONTROLS CLUSTER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp) // accommodate bottom nav bar
        ) {
            // Special Mode Selector Carousel (Video, Pro, Timelapse, Hyperlapse, SlowMo, Night, Dual PiP)
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpecialMode.entries.forEach { mode ->
                    val isModeSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isModeSelected) Color(0x4DFFB703) else Color(0x80000000))
                            .border(
                                width = 1.dp,
                                color = if (isModeSelected) AccentGold else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                if (recordingStatus == RecordingStatus.IDLE) {
                                    viewModel.cameraManager.setSpecialMode(mode)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.displayName.uppercase(),
                            color = if (isModeSelected) AccentGold else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isModeSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Shutter Button, Pause/Resume & Lens Flip Cluster
            ShutterControlCluster(
                recordingStatus = recordingStatus,
                onRecordToggle = { viewModel.onRecordButtonPressed() },
                onPauseResume = { viewModel.onPauseResumePressed() },
                onLensFlip = {
                    previewViewRef?.let {
                        viewModel.cameraManager.toggleLens(lifecycleOwner, it)
                    }
                }
            )
        }
    }
}
