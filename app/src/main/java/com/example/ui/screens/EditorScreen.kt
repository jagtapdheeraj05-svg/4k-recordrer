package com.example.ui.screens

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.model.DrawStroke
import com.example.data.model.TextAnimationType
import com.example.data.model.TransitionType
import com.example.data.model.WatermarkPosition
import com.example.editor.EditorTab
import com.example.graphics.FilterType
import com.example.graphics.LutManager
import com.example.ui.MainViewModel
import com.example.ui.components.ExportDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Indicator4K
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceGlass
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosMs by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.playerManager.duration.collectAsStateWithLifecycle()
    val showExportDialog by viewModel.showExportDialog.collectAsStateWithLifecycle()
    val exportConfig by viewModel.exportConfig.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportManager.exportProgress.collectAsStateWithLifecycle()

    var newTextTitle by remember { mutableStateOf("") }
    var selectedTextAnim by remember { mutableStateOf(TextAnimationType.FADE_IN) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.playerManager.pause()
        }
    }

    if (showExportDialog) {
        ExportDialog(
            config = exportConfig,
            progress = exportProgress,
            onConfigChange = { viewModel.updateExportConfig(it) },
            onStartExport = { viewModel.startExport() },
            onDismiss = { viewModel.closeExportDialog() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TOP TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(text = "STUDIO 4K", textColor = Indicator4K)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cine Editor",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Undo / Redo & Export Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = editorState.canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (editorState.canUndo) AccentGold else TextDisabled
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = editorState.canRedo,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (editorState.canRedo) AccentGold else TextDisabled
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = { viewModel.openExportDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // VIDEO PREVIEW VIEWPORT with Overlays & Shader Matrix Filter
        val activeMatrix = editorState.colorGrading.toCombinedMatrix()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(Color.Black)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        player = viewModel.playerManager.getPlayer()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Text Overlays on Video
            editorState.textOverlays.forEach { overlay ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(
                        text = overlay.text,
                        color = Color(android.graphics.Color.parseColor(overlay.colorHex)),
                        fontSize = overlay.fontSizeSp.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Watermark Overlay
            editorState.imageOverlays.filter { it.isWatermark }.forEach { wm ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    StatusBadge(text = "CINEPRO 4K", textColor = Indicator4K, backgroundColor = Color(0x99000000))
                }
            }

            // Freehand Drawings Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(editorState.isDrawingMode) {
                        if (editorState.isDrawingMode) {
                            var currentPoints = mutableListOf<Pair<Float, Float>>()
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = mutableListOf(Pair(offset.x, offset.y))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints.add(Pair(change.position.x, change.position.y))
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        viewModel.addDrawStroke(
                                            DrawStroke(
                                                points = currentPoints.toList(),
                                                colorHex = editorState.activeBrushColor,
                                                strokeWidthDp = editorState.activeBrushWidth
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
            ) {
                editorState.drawStrokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points[0].first, stroke.points[0].second)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].first, stroke.points[i].second)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(android.graphics.Color.parseColor(stroke.colorHex)),
                            style = Stroke(width = stroke.strokeWidthDp.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        // PLAYBACK & FRAME SCRUBBER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.playerManager.togglePlayPause() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFB703))
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time Readout
            val curSec = (currentPosMs / 1000) % 60
            val curMin = (currentPosMs / 60000) % 60
            val totSec = (totalDurationMs / 1000) % 60
            val totMin = (totalDurationMs / 60000) % 60
            Text(
                text = String.format(Locale.US, "%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec),
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            Slider(
                value = currentPosMs.toFloat(),
                onValueChange = { viewModel.playerManager.seekTo(it.toLong()) },
                valueRange = 0f..totalDurationMs.coerceAtLeast(1L).toFloat(),
                colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold),
                modifier = Modifier.weight(1f).testTag("timeline_scrubber")
            )
        }

        // MULTI-TRACK TIMELINE VISUALIZER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0C0F15))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                editorState.clips.forEachIndexed { index, clip ->
                    val isSelected = index == editorState.selectedClipIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0x4DFFB703) else Color(0x2638BDF8))
                            .border(1.5.dp, if (isSelected) AccentGold else Color(0x3338BDF8), RoundedCornerShape(6.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${clip.title} (${clip.speed}x)",
                            color = if (isSelected) AccentGold else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TOOL TABS SELECTOR
        val tabScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScroll)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorTab.entries.forEach { tab ->
                val isTabSelected = editorState.activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTabSelected) Color(0x33FFB703) else Color(0x1AFFFFFF))
                        .border(1.dp, if (isTabSelected) AccentGold else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setEditorTab(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.displayName,
                        color = if (isTabSelected) AccentGold else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TAB CONTENT PANELS (Scrollable)
        val panelScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(panelScroll)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            when (editorState.activeTab) {
                EditorTab.TIMELINE -> {
                    // Trimming & Clip Actions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.splitClipAtCurrentPosition() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Split at Playhead", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.deleteSelectedClip() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Clip", color = TextPrimary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("TRANSITION TO NEXT CLIP", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TransitionType.entries.forEach { tr ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setTransition(tr) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(tr.displayName, color = AccentCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }

                EditorTab.FILTERS -> {
                    // Filter Presets & Color Grading
                    Text("PRESET COLOR PROFILES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterType.entries.forEach { f ->
                            val isSel = editorState.colorGrading.selectedFilter == f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0x4DFFB703) else Color(0x1AFFFFFF))
                                    .border(1.dp, if (isSel) AccentGold else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateColorGrading(editorState.colorGrading.copy(selectedFilter = f)) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(f.displayName, color = if (isSel) AccentGold else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grading Sliders
                    Text("Contrast: ${String.format("%.2f", editorState.colorGrading.contrast)}", color = TextPrimary, fontSize = 11.sp)
                    Slider(
                        value = editorState.colorGrading.contrast,
                        onValueChange = { viewModel.updateColorGrading(editorState.colorGrading.copy(contrast = it)) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold),
                        modifier = Modifier.height(28.dp)
                    )

                    Text("Saturation: ${String.format("%.2f", editorState.colorGrading.saturation)}", color = TextPrimary, fontSize = 11.sp)
                    Slider(
                        value = editorState.colorGrading.saturation,
                        onValueChange = { viewModel.updateColorGrading(editorState.colorGrading.copy(saturation = it)) },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan),
                        modifier = Modifier.height(28.dp)
                    )

                    Text("Temperature: ${String.format("%.2f", editorState.colorGrading.temperature)}", color = TextPrimary, fontSize = 11.sp)
                    Slider(
                        value = editorState.colorGrading.temperature,
                        onValueChange = { viewModel.updateColorGrading(editorState.colorGrading.copy(temperature = it)) },
                        valueRange = -1.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber),
                        modifier = Modifier.height(28.dp)
                    )
                }

                EditorTab.TEXT -> {
                    // Text Overlay Creation
                    OutlinedTextField(
                        value = newTextTitle,
                        onValueChange = { newTextTitle = it },
                        label = { Text("Title or Caption") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (newTextTitle.isNotBlank()) {
                                    viewModel.addTextOverlay(newTextTitle, "SansSerif", "#FFFFB703", selectedTextAnim)
                                    newTextTitle = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Animated Title to Video", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                EditorTab.STICKERS -> {
                    // Watermarks and Logos
                    Text("WATERMARKS & BRANDING", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.addImageOverlay("4K Studio Master", isWatermark = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text("Add 4K Watermark", color = AccentGold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.addImageOverlay("Moto G35 5G Logo", isWatermark = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text("Add Device Badge", color = AccentCyan, fontSize = 11.sp)
                        }
                    }
                }

                EditorTab.AUDIO -> {
                    // Audio Mixer & Background Music
                    Text("AUDIO TRACK MIXER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.setBackgroundAudio("Cinematic Ambient Horizon (Royalty Free)", "asset:///audio/ambient.mp3") },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Background Music", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }

                EditorTab.SPEED -> {
                    // Speed Control
                    Text("CLIP SPEED (0.25x to 4x)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.25f, 0.5f, 1.0f, 2.0f, 4.0f).forEach { spd ->
                            val isSel = editorState.selectedSpeed == spd
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0x3300F5D4) else Color(0x1AFFFFFF))
                                    .border(1.dp, if (isSel) AccentCyan else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setClipSpeed(spd) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${spd}x", color = if (isSel) AccentCyan else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                EditorTab.DRAW -> {
                    // Freehand Drawing Canvas Controls
                    Text("PEN & DRAWING TOOL", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.clearDrawings() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Drawings", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
