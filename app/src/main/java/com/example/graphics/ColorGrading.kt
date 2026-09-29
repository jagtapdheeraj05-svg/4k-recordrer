package com.example.graphics

import androidx.compose.ui.graphics.ColorMatrix

data class ColorGradingSettings(
    val exposure: Float = 0.0f, // -2.0 to +2.0
    val contrast: Float = 1.0f, // 0.5 to 2.0
    val saturation: Float = 1.0f, // 0.0 to 2.0
    val temperature: Float = 0.0f, // -1.0 (cool blue) to +1.0 (warm amber)
    val tint: Float = 0.0f, // -1.0 (green) to +1.0 (magenta)
    val sharpness: Float = 0.0f, // 0.0 to 1.0
    val vignette: Float = 0.0f, // 0.0 to 1.0
    val lutStrength: Float = 1.0f, // 0.0 to 1.0
    val selectedFilter: FilterType = FilterType.NORMAL
) {
    fun toCombinedMatrix(): ColorMatrix {
        val base = FilterType.fromId(selectedFilter.id).colorMatrix.values.clone()

        // Apply contrast & exposure
        val c = contrast
        val expOffset = exposure * 30f
        val tempOffsetR = temperature * 25f
        val tempOffsetB = -temperature * 25f
        val tintOffsetG = -tint * 20f
        val tintOffsetM = tint * 20f

        val rRow0 = base[0] * c
        val rRow1 = base[1] * c
        val rRow2 = base[2] * c
        val rOffset = base[4] + expOffset + tempOffsetR + (tintOffsetM * 0.5f)

        val gRow0 = base[5] * c
        val gRow1 = base[6] * c
        val gRow2 = base[7] * c
        val gOffset = base[9] + expOffset + tintOffsetG

        val bRow0 = base[10] * c
        val bRow1 = base[11] * c
        val bRow2 = base[12] * c
        val bOffset = base[14] + expOffset + tempOffsetB + (tintOffsetM * 0.5f)

        val matrix = ColorMatrix(floatArrayOf(
            rRow0, rRow1, rRow2, 0f, rOffset,
            gRow0, gRow1, gRow2, 0f, gOffset,
            bRow0, bRow1, bRow2, 0f, bOffset,
            0f, 0f, 0f, 1f, 0f
        ))

        if (saturation != 1.0f) {
            val satMatrix = ColorMatrix().apply { setToSaturation(saturation) }
            matrix.timesAssign(satMatrix)
        }

        return matrix
    }
}
