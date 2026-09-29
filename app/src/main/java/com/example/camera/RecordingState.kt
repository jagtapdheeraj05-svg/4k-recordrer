package com.example.camera

enum class RecordingStatus {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    FINALIZING,
    ERROR
}

enum class CameraLens {
    REAR_MAIN, // 50MP 4K
    FRONT_SELFIE // 16MP 1080p
}

enum class AspectRatioMode(val displayName: String, val width: Int, val height: Int, val ratioFloat: Float) {
    RATIO_16_9("16:9 (3840×2160)", 3840, 2160, 16f / 9f),
    RATIO_20_9("20:9 (3840×1728)", 3840, 1728, 20f / 9f),
    RATIO_4_3("4:3 (2880×2160)", 2880, 2160, 4f / 3f),
    RATIO_1_1("1:1 (2160×2160)", 2160, 2160, 1f)
}

enum class WhiteBalanceMode(val displayName: String, val androidMode: Int) {
    AUTO("Auto WB", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("Daylight 5500K", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("Cloudy 6500K", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    TUNGSTEN("Tungsten 3200K", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("Fluorescent 4000K", android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT)
}

enum class SpecialMode(val displayName: String, val badge: String, val description: String) {
    VIDEO("Video", "4K 30FPS", "Standard high-bitrate 4K UHD 20Mbps recording"),
    PRO_MANUAL("Pro Manual", "MANUAL", "Full manual shutter, ISO, EV, WB & focus peaking"),
    TIMELAPSE("Timelapse", "TIME-LAPSE", "Time compressed recording with selectable intervals"),
    HYPERLAPSE("Hyperlapse", "HYPER-EIS", "Motion-stabilized smooth moving timelapse"),
    SLOW_MOTION("Slow Motion", "SLOW-MO", "High framerate capture with smooth slow-motion playback"),
    NIGHT_VIDEO("Night Cine", "NIGHT", "Longer per-frame integration & low-noise processing"),
    DUAL_PIP("Dual PiP", "DUAL CAM", "Front + Rear picture-in-picture recording")
}

data class LiveTelemetry(
    val recordedDurationMs: Long = 0L,
    val recordedSizeBytes: Long = 0L,
    val formattedDuration: String = "00:00:00",
    val formattedFileSize: String = "0.0 MB",
    val estimatedBitrateMbps: Float = 20.0f,
    val liveFps: Int = 30,
    val audioLevelDb: Float = -18f, // -60dB to 0dB
    val isAudioMuted: Boolean = false,
    val zebraClippingRatio: Float = 0.02f, // 0.0 to 1.0 highlight clipping
    val focusPeakingActive: Boolean = false,
    val gyroPitch: Float = 0f,
    val gyroRoll: Float = 0f,
    val gyroYaw: Float = 0f,
    val eisStabilizationActive: Boolean = true
)
