package com.example.data.model

data class TimelineClip(
    val id: String,
    val sourcePath: String,
    val title: String,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 0L, // 0 = use full length
    val totalDurationMs: Long = 0L,
    val speed: Float = 1.0f, // 0.25f to 4.0f
    val volume: Float = 1.0f, // 0.0f to 2.0f
    val orderIndex: Int = 0,
    val transition: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 500L,
    val filterName: String = "Normal",
    val filterIntensity: Float = 1.0f,
    val isMuted: Boolean = false
)

enum class TransitionType(val displayName: String) {
    NONE("None"),
    CROSSFADE("Crossfade"),
    DISSOLVE("Dissolve"),
    SLIDE_LEFT("Slide Left"),
    SLIDE_RIGHT("Slide Right"),
    WIPE("Wipe"),
    ZOOM_IN("Zoom In"),
    GLITCH("Glitch Effect")
}

data class AudioTrackItem(
    val id: String,
    val title: String,
    val uriString: String,
    val startOffsetMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.8f,
    val isVoiceTrack: Boolean = false,
    val echoEnabled: Boolean = false,
    val reverbEnabled: Boolean = false,
    val pitchShift: Float = 1.0f
)
