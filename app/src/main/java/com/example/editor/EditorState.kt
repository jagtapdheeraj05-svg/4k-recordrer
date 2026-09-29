package com.example.editor

import com.example.data.model.AudioTrackItem
import com.example.data.model.DrawStroke
import com.example.data.model.ImageOverlayItem
import com.example.data.model.TextOverlayItem
import com.example.data.model.TimelineClip
import com.example.graphics.ColorGradingSettings
import com.example.graphics.FilterType

data class EditorState(
    val clips: List<TimelineClip> = emptyList(),
    val selectedClipIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val activeTab: EditorTab = EditorTab.TIMELINE,
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val selectedTextId: String? = null,
    val imageOverlays: List<ImageOverlayItem> = emptyList(),
    val drawStrokes: List<DrawStroke> = emptyList(),
    val backgroundAudio: AudioTrackItem? = null,
    val colorGrading: ColorGradingSettings = ColorGradingSettings(),
    val selectedSpeed: Float = 1.0f,
    val isDrawingMode: Boolean = false,
    val activeBrushColor: String = "#EF233C",
    val activeBrushWidth: Float = 6f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val selectedClip: TimelineClip?
        get() = if (clips.isNotEmpty() && selectedClipIndex in clips.indices) clips[selectedClipIndex] else null
}

enum class EditorTab(val displayName: String) {
    TIMELINE("Timeline & Trim"),
    FILTERS("Color & LUT"),
    TEXT("Text & Titles"),
    STICKERS("Overlays & Logo"),
    AUDIO("Music & Mixer"),
    SPEED("Speed & Ramping"),
    DRAW("Draw & Pen")
}
