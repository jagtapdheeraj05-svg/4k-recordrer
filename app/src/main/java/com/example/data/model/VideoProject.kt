package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0L,
    val resolutionWidth: Int = 3840,
    val resolutionHeight: Int = 2160,
    val fps: Int = 30,
    val bitrateMbps: Int = 20,
    val fileSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val is4k: Boolean = true,
    val aspectRatio: String = "16:9", // "16:9", "20:9", "4:3", "1:1"
    val cameraLens: String = "REAR (50MP)",
    val filterUsed: String = "Normal",
    val tags: String = "4K UHD, H.264",
    val customNote: String = "",
    val isEditedProject: Boolean = false
)
