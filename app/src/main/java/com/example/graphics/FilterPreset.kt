package com.example.graphics

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

enum class FilterType(
    val id: String,
    val displayName: String,
    val description: String,
    val colorMatrix: ColorMatrix
) {
    NORMAL(
        id = "normal",
        displayName = "Normal",
        description = "Natural Rec.709 color balance",
        colorMatrix = ColorMatrix()
    ),
    CINEMATIC(
        id = "cinematic",
        displayName = "Cinematic",
        description = "Teal shadows with warm orange skin tones",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.25f, -0.05f, -0.05f, 0f, 15f,
            -0.05f, 1.05f, -0.05f, 0f, 0f,
            -0.15f, -0.05f, 1.35f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        ))
    ),
    VIVID(
        id = "vivid",
        displayName = "Vivid",
        description = "High dynamic pop & enhanced saturation",
        colorMatrix = ColorMatrix().apply {
            setToSaturation(1.45f)
        }
    ),
    VINTAGE(
        id = "vintage",
        displayName = "Vintage 70s",
        description = "Warm golden nostalgic grain tone",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0.1f, 0.1f, 0f, 20f,
            0.1f, 1.0f, 0.1f, 0f, 10f,
            0.05f, 0.1f, 0.8f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ))
    ),
    BW_NOIR(
        id = "bw",
        displayName = "B&W Noir",
        description = "Deep contrast classic monochrome",
        colorMatrix = ColorMatrix().apply {
            setToSaturation(0f)
        }
    ),
    SEPIA(
        id = "sepia",
        displayName = "Sepia",
        description = "Retro copper antique finish",
        colorMatrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    ),
    COOL_ICE(
        id = "cool",
        displayName = "Cool Nordic",
        description = "Frosty steel cyan & blue highlights",
        colorMatrix = ColorMatrix(floatArrayOf(
            0.85f, 0f, 0.15f, 0f, -10f,
            0f, 0.95f, 0.05f, 0f, 0f,
            0f, 0.1f, 1.25f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
    ),
    WARM_GOLD(
        id = "warm",
        displayName = "Golden Hour",
        description = "Sun-kissed amber & warm daylight",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.25f, 0.05f, 0f, 0f, 20f,
            0.05f, 1.1f, 0f, 0f, 10f,
            0f, 0f, 0.85f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        ))
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Cyberpunk",
        description = "Electric neon magenta & cyan hues",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.3f, 0f, 0.3f, 0f, 25f,
            0f, 0.9f, 0.2f, 0f, -10f,
            0.3f, 0f, 1.4f, 0f, 35f,
            0f, 0f, 0f, 1f, 0f
        ))
    ),
    FLAT_LOG(
        id = "log",
        displayName = "Flat C-Log",
        description = "Wide dynamic range profile for color grading",
        colorMatrix = ColorMatrix(floatArrayOf(
            0.7f, 0.15f, 0.15f, 0f, 40f,
            0.15f, 0.7f, 0.15f, 0f, 40f,
            0.15f, 0.15f, 0.7f, 0f, 40f,
            0f, 0f, 0f, 1f, 0f
        ))
    );

    companion object {
        fun fromId(id: String): FilterType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: NORMAL
        }
    }
}
