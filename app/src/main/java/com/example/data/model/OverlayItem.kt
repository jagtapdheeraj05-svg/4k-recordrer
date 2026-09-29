package com.example.data.model

data class TextOverlayItem(
    val id: String,
    val text: String,
    val colorHex: String = "#FFFFB703",
    val fontSizeSp: Float = 28f,
    val xPosNorm: Float = 0.5f, // 0.0 to 1.0 normalized screen position
    val yPosNorm: Float = 0.5f,
    val rotationDeg: Float = 0f,
    val scale: Float = 1.0f,
    val opacity: Float = 1.0f,
    val fontStyle: String = "SansSerif", // "SansSerif", "Serif", "Monospace", "Cursive"
    val animation: TextAnimationType = TextAnimationType.FADE_IN,
    val startMs: Long = 0L,
    val durationMs: Long = 3000L
)

enum class TextAnimationType(val displayName: String) {
    NONE("Static"),
    FADE_IN("Fade In"),
    SLIDE("Slide In"),
    TYPEWRITER("Typewriter"),
    BOUNCE("Bounce"),
    GLOW("Neon Glow")
}

data class ImageOverlayItem(
    val id: String,
    val imageUri: String,
    val label: String = "Sticker",
    val xPosNorm: Float = 0.85f,
    val yPosNorm: Float = 0.15f,
    val scale: Float = 0.8f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 0.9f,
    val isWatermark: Boolean = false,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.TOP_RIGHT,
    val startMs: Long = 0L,
    val durationMs: Long = 0L // 0 = entire video
)

enum class WatermarkPosition(val displayName: String) {
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right"),
    CENTER("Center")
}

data class DrawStroke(
    val points: List<Pair<Float, Float>>, // normalized x, y
    val colorHex: String = "#EF233C",
    val strokeWidthDp: Float = 5f
)
