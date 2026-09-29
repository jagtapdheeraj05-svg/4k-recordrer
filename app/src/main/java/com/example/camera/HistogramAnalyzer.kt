package com.example.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

data class HistogramData(
    val bins: FloatArray = FloatArray(64) { 0f },
    val clippingHighlightRatio: Float = 0f,
    val averageLuminance: Float = 128f,
    val peakSharpness: Float = 0.5f
)

class HistogramAnalyzer(
    private val onDataUpdated: (HistogramData) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCounter = 0

    override fun analyze(image: ImageProxy) {
        // Sample every 2nd frame to keep CPU load low on Unisoc T760
        frameCounter++
        if (frameCounter % 2 != 0) {
            image.close()
            return
        }

        try {
            val planes = image.planes
            if (planes.isNotEmpty()) {
                val buffer: ByteBuffer = planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                val bins = IntArray(64)
                var clippedPixels = 0
                var totalLum = 0L
                val step = 8 // Subsample pixels for 60fps performance
                var sampledCount = 0

                var prevVal = 0
                var edgeSum = 0L

                for (i in 0 until data.size step step) {
                    val lum = data[i].toInt() and 0xFF
                    val binIndex = (lum * 64) / 256
                    bins[binIndex.coerceIn(0, 63)]++
                    totalLum += lum
                    sampledCount++

                    if (lum >= 245) {
                        clippedPixels++
                    }

                    // Simple high-pass gradient for focus peaking
                    val diff = Math.abs(lum - prevVal)
                    edgeSum += diff
                    prevVal = lum
                }

                if (sampledCount > 0) {
                    val maxBin = bins.maxOrNull() ?: 1
                    val normalizedBins = FloatArray(64) {
                        if (maxBin > 0) bins[it].toFloat() / maxBin.toFloat() else 0f
                    }
                    val clippingRatio = clippedPixels.toFloat() / sampledCount.toFloat()
                    val avgLum = totalLum.toFloat() / sampledCount.toFloat()
                    val sharpness = (edgeSum.toFloat() / sampledCount.toFloat()) / 64f

                    onDataUpdated(
                        HistogramData(
                            bins = normalizedBins,
                            clippingHighlightRatio = clippingRatio,
                            averageLuminance = avgLum,
                            peakSharpness = sharpness.coerceIn(0f, 1f)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore frame analysis exceptions
        } finally {
            image.close()
        }
    }
}
