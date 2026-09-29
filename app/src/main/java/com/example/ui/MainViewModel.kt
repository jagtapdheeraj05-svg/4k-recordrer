package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.AspectRatioMode
import com.example.camera.CameraControlManager
import com.example.camera.CameraLens
import com.example.camera.DeviceCapabilityDetector
import com.example.camera.DeviceSpec
import com.example.camera.RecordingStatus
import com.example.camera.SensorStabilizer
import com.example.camera.SpecialMode
import com.example.camera.WhiteBalanceMode
import com.example.data.db.CineDatabase
import com.example.data.model.AudioTrackItem
import com.example.data.model.DrawStroke
import com.example.data.model.ImageOverlayItem
import com.example.data.model.TextAnimationType
import com.example.data.model.TextOverlayItem
import com.example.data.model.TimelineClip
import com.example.data.model.TransitionType
import com.example.data.model.VideoProject
import com.example.data.repository.ProjectRepository
import com.example.editor.EditorPlayerManager
import com.example.editor.EditorState
import com.example.editor.EditorTab
import com.example.export.ExportConfig
import com.example.export.VideoExportManager
import com.example.graphics.ColorGradingSettings
import com.example.graphics.FilterType
import java.io.File
import java.util.Stack
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavigationScreen {
    RECORD,
    EDITOR,
    GALLERY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CineDatabase.getDatabase(application)
    val repository = ProjectRepository(db.videoProjectDao())

    val cameraManager = CameraControlManager(application)
    val sensorStabilizer = SensorStabilizer(application)
    val playerManager = EditorPlayerManager(application)
    val exportManager = VideoExportManager(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(AppNavigationScreen.RECORD)
    val currentScreen: StateFlow<AppNavigationScreen> = _currentScreen.asStateFlow()

    // Device Specs
    private val _deviceSpec = MutableStateFlow(DeviceCapabilityDetector.inspectDevice(application))
    val deviceSpec: StateFlow<DeviceSpec> = _deviceSpec.asStateFlow()

    // Database Projects
    val allProjects: StateFlow<List<VideoProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Editor State
    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    // Undo/Redo Stacks
    private val undoStack = Stack<EditorState>()
    private val redoStack = Stack<EditorState>()

    // Export Dialog State
    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    // Currently Playing Project in Gallery
    private val _previewProject = MutableStateFlow<VideoProject?>(null)
    val previewProject: StateFlow<VideoProject?> = _previewProject.asStateFlow()

    init {
        sensorStabilizer.start()

        cameraManager.onVideoSavedListener = { file, durationMs, sizeBytes, width, height ->
            saveRecordedProject(file, durationMs, sizeBytes, width, height)
        }

        // Add sample starter 4K projects if DB is empty for great first launch UX
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                if (list.isEmpty()) {
                    val starterProject1 = VideoProject(
                        title = "Cinematic 4K Sunset Master",
                        filePath = "",
                        durationMs = 14200L,
                        resolutionWidth = 3840,
                        resolutionHeight = 2160,
                        fps = 30,
                        bitrateMbps = 20,
                        fileSizeBytes = 35_500_000L,
                        is4k = true,
                        aspectRatio = "16:9",
                        cameraLens = "REAR 50MP",
                        filterUsed = "Cinematic",
                        tags = "4K UHD, H.264, 20Mbps, Moto G35",
                        customNote = "Recorded at 3840x2160 with Gyro EIS and Teal-Orange grading"
                    )
                    val starterProject2 = VideoProject(
                        title = "Ultrawide 20:9 Street Hyperlapse",
                        filePath = "",
                        durationMs = 8600L,
                        resolutionWidth = 3840,
                        resolutionHeight = 1728,
                        fps = 30,
                        bitrateMbps = 20,
                        fileSizeBytes = 21_500_000L,
                        is4k = true,
                        aspectRatio = "20:9",
                        cameraLens = "REAR 50MP",
                        filterUsed = "Cyberpunk",
                        tags = "20:9 Wide, Hyperlapse, Stabilized",
                        customNote = "Ultrawide format matching Moto G35 display"
                    )
                    repository.saveProject(starterProject1)
                    repository.saveProject(starterProject2)
                }
            }
        }
    }

    fun navigateTo(screen: AppNavigationScreen) {
        _currentScreen.value = screen
        if (screen != AppNavigationScreen.EDITOR) {
            playerManager.pause()
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getApplication<Application>().getSystemService(Application.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                val vibrator = getApplication<Application>().getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    fun onRecordButtonPressed() {
        triggerHapticFeedback()
        val status = cameraManager.recordingStatus.value
        when (status) {
            RecordingStatus.IDLE -> {
                if (_deviceSpec.value.isStorageCritical) {
                    Toast.makeText(getApplication(), "Storage critically low (<500MB). Free space to record.", Toast.LENGTH_LONG).show()
                    return
                }
                cameraManager.startRecording(recordAudio = true)
            }
            RecordingStatus.RECORDING -> {
                cameraManager.stopRecording()
            }
            RecordingStatus.PAUSED -> {
                cameraManager.stopRecording()
            }
            else -> {}
        }
    }

    fun onPauseResumePressed() {
        triggerHapticFeedback()
        val status = cameraManager.recordingStatus.value
        if (status == RecordingStatus.RECORDING) {
            cameraManager.pauseRecording()
        } else if (status == RecordingStatus.PAUSED) {
            cameraManager.resumeRecording()
        }
    }

    private fun saveRecordedProject(file: File, durationMs: Long, sizeBytes: Long, width: Int, height: Int) {
        viewModelScope.launch {
            val isBack = cameraManager.currentLens.value == CameraLens.REAR_MAIN
            val ratio = cameraManager.aspectRatio.value
            val currentMode = cameraManager.selectedMode.value

            val project = VideoProject(
                title = "${currentMode.displayName} ${if (isBack) "4K" else "1080p"} Take",
                filePath = file.absolutePath,
                durationMs = durationMs,
                resolutionWidth = if (ratio == AspectRatioMode.RATIO_20_9) 3840 else width,
                resolutionHeight = if (ratio == AspectRatioMode.RATIO_20_9) 1728 else height,
                fps = 30,
                bitrateMbps = if (isBack) 20 else 12,
                fileSizeBytes = sizeBytes,
                is4k = isBack,
                aspectRatio = if (ratio == AspectRatioMode.RATIO_20_9) "20:9" else "16:9",
                cameraLens = if (isBack) "REAR 50MP" else "FRONT 16MP",
                filterUsed = "Normal",
                tags = "Moto G35, ${if (isBack) "4K UHD" else "1080p"}, H.264",
                customNote = "Recorded in ${currentMode.displayName} mode with software EIS stabilization."
            )
            val newId = repository.saveProject(project)

            // Auto load into editor
            loadProjectIntoEditor(project.copy(id = newId))
            Toast.makeText(getApplication(), "4K Video saved! Ready in Editor.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadProjectIntoEditor(project: VideoProject) {
        saveUndoState()
        val clip = TimelineClip(
            id = UUID.randomUUID().toString(),
            sourcePath = project.filePath,
            title = project.title,
            startTrimMs = 0L,
            endTrimMs = project.durationMs,
            totalDurationMs = project.durationMs,
            speed = 1.0f,
            volume = 1.0f,
            orderIndex = 0
        )
        _editorState.value = EditorState(
            clips = listOf(clip),
            selectedClipIndex = 0,
            currentPositionMs = 0L,
            totalDurationMs = project.durationMs,
            activeTab = EditorTab.TIMELINE,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        if (project.filePath.isNotEmpty()) {
            playerManager.loadMedia(project.filePath)
        }
        _currentScreen.value = AppNavigationScreen.EDITOR
    }

    private fun saveUndoState() {
        undoStack.push(_editorState.value)
        redoStack.clear()
        updateUndoRedoStatus()
    }

    private fun updateUndoRedoStatus() {
        _editorState.value = _editorState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_editorState.value)
            _editorState.value = undoStack.pop().copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_editorState.value)
            _editorState.value = redoStack.pop().copy(
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun setEditorTab(tab: EditorTab) {
        _editorState.value = _editorState.value.copy(activeTab = tab)
    }

    fun updateClipTrim(startMs: Long, endMs: Long) {
        saveUndoState()
        val clips = _editorState.value.clips.toMutableList()
        val idx = _editorState.value.selectedClipIndex
        if (idx in clips.indices) {
            val clip = clips[idx]
            val updated = clip.copy(startTrimMs = startMs, endTrimMs = endMs)
            clips[idx] = updated
            val newTotal = clips.sumOf { (it.endTrimMs - it.startTrimMs).coerceAtLeast(0L) }
            _editorState.value = _editorState.value.copy(clips = clips, totalDurationMs = newTotal)
        }
    }

    fun splitClipAtCurrentPosition() {
        saveUndoState()
        val clips = _editorState.value.clips.toMutableList()
        val idx = _editorState.value.selectedClipIndex
        if (idx in clips.indices) {
            val clip = clips[idx]
            val splitPos = _editorState.value.currentPositionMs.coerceIn(clip.startTrimMs + 500, clip.endTrimMs - 500)
            if (splitPos > clip.startTrimMs && splitPos < clip.endTrimMs) {
                val clip1 = clip.copy(id = UUID.randomUUID().toString(), endTrimMs = splitPos)
                val clip2 = clip.copy(id = UUID.randomUUID().toString(), startTrimMs = splitPos, orderIndex = clip.orderIndex + 1)
                clips[idx] = clip1
                clips.add(idx + 1, clip2)
                _editorState.value = _editorState.value.copy(clips = clips, selectedClipIndex = idx + 1)
                Toast.makeText(getApplication(), "Clip split into two tracks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteSelectedClip() {
        saveUndoState()
        val clips = _editorState.value.clips.toMutableList()
        val idx = _editorState.value.selectedClipIndex
        if (idx in clips.indices && clips.size > 1) {
            clips.removeAt(idx)
            val newIdx = (idx - 1).coerceAtLeast(0)
            val newTotal = clips.sumOf { (it.endTrimMs - it.startTrimMs).coerceAtLeast(0L) }
            _editorState.value = _editorState.value.copy(clips = clips, selectedClipIndex = newIdx, totalDurationMs = newTotal)
            Toast.makeText(getApplication(), "Clip deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun setClipSpeed(speed: Float) {
        saveUndoState()
        val clips = _editorState.value.clips.toMutableList()
        val idx = _editorState.value.selectedClipIndex
        if (idx in clips.indices) {
            clips[idx] = clips[idx].copy(speed = speed)
            _editorState.value = _editorState.value.copy(clips = clips, selectedSpeed = speed)
            playerManager.setSpeed(speed)
        }
    }

    fun setTransition(transition: TransitionType) {
        saveUndoState()
        val clips = _editorState.value.clips.toMutableList()
        val idx = _editorState.value.selectedClipIndex
        if (idx in clips.indices) {
            clips[idx] = clips[idx].copy(transition = transition)
            _editorState.value = _editorState.value.copy(clips = clips)
            Toast.makeText(getApplication(), "Transition applied: ${transition.displayName}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateColorGrading(settings: ColorGradingSettings) {
        saveUndoState()
        _editorState.value = _editorState.value.copy(colorGrading = settings)
    }

    fun addTextOverlay(text: String, fontStyle: String, colorHex: String, animation: TextAnimationType) {
        saveUndoState()
        val newItem = TextOverlayItem(
            id = UUID.randomUUID().toString(),
            text = text,
            fontStyle = fontStyle,
            colorHex = colorHex,
            animation = animation,
            startMs = _editorState.value.currentPositionMs,
            durationMs = 4000L
        )
        val list = _editorState.value.textOverlays + newItem
        _editorState.value = _editorState.value.copy(textOverlays = list, selectedTextId = newItem.id)
    }

    fun updateTextOverlay(item: TextOverlayItem) {
        val list = _editorState.value.textOverlays.map { if (it.id == item.id) item else it }
        _editorState.value = _editorState.value.copy(textOverlays = list)
    }

    fun deleteTextOverlay(id: String) {
        saveUndoState()
        val list = _editorState.value.textOverlays.filterNot { it.id == id }
        _editorState.value = _editorState.value.copy(textOverlays = list, selectedTextId = null)
    }

    fun addImageOverlay(label: String, isWatermark: Boolean) {
        saveUndoState()
        val item = ImageOverlayItem(
            id = UUID.randomUUID().toString(),
            imageUri = "",
            label = label,
            isWatermark = isWatermark
        )
        _editorState.value = _editorState.value.copy(imageOverlays = _editorState.value.imageOverlays + item)
    }

    fun addDrawStroke(stroke: DrawStroke) {
        _editorState.value = _editorState.value.copy(drawStrokes = _editorState.value.drawStrokes + stroke)
    }

    fun clearDrawings() {
        saveUndoState()
        _editorState.value = _editorState.value.copy(drawStrokes = emptyList())
    }

    fun setBackgroundAudio(title: String, uriString: String) {
        saveUndoState()
        val track = AudioTrackItem(
            id = UUID.randomUUID().toString(),
            title = title,
            uriString = uriString,
            volume = 0.8f
        )
        _editorState.value = _editorState.value.copy(backgroundAudio = track)
        Toast.makeText(getApplication(), "Background audio added: $title", Toast.LENGTH_SHORT).show()
    }

    fun openExportDialog() {
        _showExportDialog.value = true
    }

    fun closeExportDialog() {
        _showExportDialog.value = false
    }

    fun updateExportConfig(config: ExportConfig) {
        _exportConfig.value = config
    }

    fun startExport() {
        _showExportDialog.value = false
        viewModelScope.launch {
            exportManager.exportProject(_editorState.value, _exportConfig.value) { newProject ->
                viewModelScope.launch {
                    repository.saveProject(newProject)
                    Toast.makeText(getApplication(), "Export saved to Gallery: ${newProject.title}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun toggleFavorite(project: VideoProject) {
        viewModelScope.launch {
            repository.toggleFavorite(project.id, !project.isFavorite)
        }
    }

    fun deleteProject(project: VideoProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (project.filePath.isNotEmpty()) {
                val f = File(project.filePath)
                if (f.exists()) f.delete()
            }
            if (_previewProject.value?.id == project.id) {
                _previewProject.value = null
            }
            Toast.makeText(getApplication(), "Video deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareProject(project: VideoProject) {
        val file = File(project.filePath)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.provider",
                    file
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(Intent.EXTRA_TEXT, "Check out my 4K Video: ${project.title} recorded on Moto G35 with CinePro 4K!")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(Intent.createChooser(shareIntent, "Share 4K Video via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun setPreviewProject(project: VideoProject?) {
        _previewProject.value = project
        if (project != null && project.filePath.isNotEmpty()) {
            playerManager.loadMedia(project.filePath)
        }
    }

    fun refreshDeviceHealth() {
        _deviceSpec.value = DeviceCapabilityDetector.inspectDevice(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        sensorStabilizer.stop()
        cameraManager.release()
        playerManager.release()
    }
}
