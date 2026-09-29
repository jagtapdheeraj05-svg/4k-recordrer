package com.example.graphics

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class LutPreset(
    val name: String,
    val description: String,
    val size: Int = 33,
    val data: FloatArray? = null
)

object LutManager {
    val builtInLuts = listOf(
        LutPreset("Kodak 2383", "Classic Hollywood cinematic film stock emulation"),
        LutPreset("Fuji F-125", "Rich greens, pastel skin tones, soft contrast"),
        LutPreset("Alexa 709", "Arri standard broadcast high dynamic curve"),
        LutPreset("Bleach Bypass", "High contrast desaturated gritty look"),
        LutPreset("Golden Hour 3D", "Warm sunset glow LUT curve"),
        LutPreset("Moody Teal", "Moody Scandinavian teal shadows")
    )

    fun parseCubeFile(inputStream: InputStream): LutPreset? {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            var title = "Custom LUT"
            var lutSize = 33
            val table = ArrayList<Float>()

            reader.forEachLine { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("TITLE", ignoreCase = true) -> {
                        title = trimmed.removePrefix("TITLE").trim().replace("\"", "")
                    }
                    trimmed.startsWith("LUT_3D_SIZE", ignoreCase = true) -> {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            lutSize = parts[1].toIntOrNull() ?: 33
                        }
                    }
                    trimmed.isEmpty() || trimmed.startsWith("#") -> {
                        // ignore comment or blank
                    }
                    else -> {
                        val rgb = trimmed.split("\\s+".toRegex()).mapNotNull { it.toFloatOrNull() }
                        if (rgb.size == 3) {
                            table.add(rgb[0])
                            table.add(rgb[1])
                            table.add(rgb[2])
                        }
                    }
                }
            }
            return LutPreset(name = title, description = "Custom 3D LUT", size = lutSize, data = table.toFloatArray())
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
