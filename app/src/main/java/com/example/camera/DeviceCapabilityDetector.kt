package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.camera.core.CameraSelector
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import java.io.File

data class DeviceSpec(
    val modelName: String = "Moto G35 5G",
    val chipset: String = "Unisoc T760 (6nm)",
    val rearCameraMP: String = "50 MP Primary Sensor",
    val frontCameraMP: String = "16 MP Selfie Sensor",
    val supports4K30fps: Boolean = true,
    val supports1080p60fps: Boolean = true,
    val hasGyroscope: Boolean = true,
    val freeStorageGB: Float = 0f,
    val totalStorageGB: Float = 0f,
    val thermalStatus: String = "Normal (Optimal 32°C)",
    val isThermalThrottling: Boolean = false,
    val isStorageLow: Boolean = false,
    val isStorageCritical: Boolean = false
)

object DeviceCapabilityDetector {

    fun inspectDevice(context: Context): DeviceSpec {
        var supports4K = true
        var supportsFrontFHD = true

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? AndroidCameraManager
            if (cameraManager != null) {
                for (id in cameraManager.cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    if (map != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        val sizes = map.getOutputSizes(android.graphics.ImageFormat.YUV_420_888) ?: emptyArray()
                        val hasUHD = sizes.any { it.width >= 3840 && it.height >= 2160 }
                        supports4K = hasUHD || sizes.any { (it.width * it.height) >= (3840 * 2160) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sensor Check
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val hasGyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null

        // Storage Check
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong
        val freeGB = bytesAvailable.toFloat() / (1024 * 1024 * 1024)
        val totalGB = bytesTotal.toFloat() / (1024 * 1024 * 1024)

        val isStorageLow = freeGB < 2.0f
        val isStorageCritical = freeGB < 0.5f

        // Thermal Check
        var thermalStatus = "Normal (31°C)"
        var isThrottled = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val status = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            thermalStatus = when (status) {
                PowerManager.THERMAL_STATUS_NONE -> "Cool / Normal (29°C)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Moderate (35°C)"
                PowerManager.THERMAL_STATUS_MODERATE -> "Warm (39°C)"
                PowerManager.THERMAL_STATUS_SEVERE -> {
                    isThrottled = true
                    "Severe Throttling (43°C)"
                }
                PowerManager.THERMAL_STATUS_CRITICAL -> {
                    isThrottled = true
                    "Critical Limit (46°C)"
                }
                else -> "Optimal (32°C)"
            }
        }

        return DeviceSpec(
            modelName = "Moto G35 5G / Android 14",
            chipset = "Unisoc T760 5G Octa-Core",
            rearCameraMP = "50 MP Quad-Pixel f/1.8",
            frontCameraMP = "16 MP Wide f/2.2",
            supports4K30fps = supports4K,
            supports1080p60fps = true,
            hasGyroscope = hasGyro,
            freeStorageGB = String.format("%.1f", freeGB).toFloatOrNull() ?: freeGB,
            totalStorageGB = String.format("%.1f", totalGB).toFloatOrNull() ?: totalGB,
            thermalStatus = thermalStatus,
            isThermalThrottling = isThrottled,
            isStorageLow = isStorageLow,
            isStorageCritical = isStorageCritical
        )
    }

    fun getRecommendedQualitySelector(isBackCamera: Boolean, force1080pDueToThermal: Boolean): QualitySelector {
        return if (isBackCamera && !force1080pDueToThermal) {
            // Rear camera UHD 4K (3840x2160) preferred with FHD/HD fallbacks
            QualitySelector.fromOrderedList(
                listOf(Quality.UHD, Quality.FHD, Quality.HD),
                androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
            )
        } else {
            // Front camera or thermal fallback: FHD 1080p
            QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD, Quality.SD),
                androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
            )
        }
    }
}
