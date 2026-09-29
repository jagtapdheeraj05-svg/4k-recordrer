package com.example.export

enum class ExportResolution(
    val displayName: String,
    val width: Int,
    val height: Int,
    val description: String
) {
    UHD_4K("4K UHD (3840×2160)", 3840, 2160, "Ultra high resolution for 50MP rear camera master"),
    CINEMA_20_9("20:9 Cinema (3840×1728)", 3840, 1728, "Ultrawide cinematic aspect ratio for Moto G35"),
    FHD_1080P("1080p FHD (1920×1080)", 1920, 1080, "Crisp standard HD for social sharing"),
    HD_720P("720p HD (1280×720)", 1280, 720, "Compact fast export for messaging")
}

enum class ExportBitrate(val displayName: String, val bitrateBps: Int, val description: String) {
    HIGH("High Quality (50 Mbps)", 50_000_000, "Maximum fidelity, studio archive"),
    MEDIUM("Medium Balanced (20 Mbps)", 20_000_000, "Recommended for Moto G35 4K recording"),
    LOW("Low Compact (10 Mbps)", 10_000_000, "Smaller file size for quick uploads")
}

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    MP4_H264("MP4 (H.264 / AVC)", "mp4", "video/mp4"),
    WEBM("WebM (VP9)", "webm", "video/webm")
}

enum class ExportFramerate(val displayName: String, val fps: Int) {
    FPS_30("30 FPS (Standard)", 30),
    FPS_60("60 FPS (Smooth Motion)", 60)
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.UHD_4K,
    val bitrate: ExportBitrate = ExportBitrate.MEDIUM,
    val format: ExportFormat = ExportFormat.MP4_H264,
    val framerate: ExportFramerate = ExportFramerate.FPS_30,
    val includeAudio: Boolean = true,
    val applyColorGrading: Boolean = true,
    val burnOverlays: Boolean = true
)

data class ExportProgress(
    val isExporting: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentStage: String = "Idle",
    val outputPath: String? = null,
    val errorMessage: String? = null,
    val isFinished: Boolean = false
)
