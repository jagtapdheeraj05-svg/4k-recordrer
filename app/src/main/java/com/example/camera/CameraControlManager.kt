package com.example.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraControlManager(private val context: Context) {

    private val TAG = "CineProCamera"

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _recordingStatus = MutableStateFlow(RecordingStatus.IDLE)
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private val _histogramData = MutableStateFlow(HistogramData())
    val histogramData: StateFlow<HistogramData> = _histogramData.asStateFlow()

    private val _currentLens = MutableStateFlow(CameraLens.REAR_MAIN)
    val currentLens: StateFlow<CameraLens> = _currentLens.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _exposureIndex = MutableStateFlow(0)
    val exposureIndex: StateFlow<Int> = _exposureIndex.asStateFlow()

    private val _selectedMode = MutableStateFlow(SpecialMode.VIDEO)
    val selectedMode: StateFlow<SpecialMode> = _selectedMode.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.RATIO_16_9)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _whiteBalance = MutableStateFlow(WhiteBalanceMode.AUTO)
    val whiteBalance: StateFlow<WhiteBalanceMode> = _whiteBalance.asStateFlow()

    var onVideoSavedListener: ((File, Long, Long, Int, Int) -> Unit)? = null
    var activeRecordingFile: File? = null

    fun setSpecialMode(mode: SpecialMode) {
        _selectedMode.value = mode
    }

    fun setAspectRatio(ratio: AspectRatioMode) {
        _aspectRatio.value = ratio
    }

    fun toggleLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val newLens = if (_currentLens.value == CameraLens.REAR_MAIN) CameraLens.FRONT_SELFIE else CameraLens.REAR_MAIN
        _currentLens.value = newLens
        startCamera(lifecycleOwner, previewView)
    }

    fun toggleTorch() {
        camera?.cameraControl?.let { control ->
            val newState = !_isTorchOn.value
            control.enableTorch(newState)
            _isTorchOn.value = newState
        }
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(1.0f, 10.0f))
        _zoomRatio.value = ratio
    }

    fun setExposureCompensation(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index.coerceIn(-2, 2))
        _exposureIndex.value = index
    }

    fun setWhiteBalanceMode(mode: WhiteBalanceMode) {
        _whiteBalance.value = mode
        try {
            camera?.let { cam ->
                val camera2Control = Camera2CameraControl.from(cam.cameraControl)
                val options = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE,
                        mode.androidMode
                    )
                    .build()
                camera2Control.setCaptureRequestOptions(options)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Manual WB not supported: ${e.message}")
        }
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float) {
        val meteringPointFactory = previewView.meteringPointFactory
        val point = meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onCameraReady: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val isBack = _currentLens.value == CameraLens.REAR_MAIN
                val cameraSelector = if (isBack) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                // High Quality 4K UHD Quality Selector (2160p) for rear, 1080p for front
                val qualitySelector = DeviceCapabilityDetector.getRecommendedQualitySelector(
                    isBackCamera = isBack,
                    force1080pDueToThermal = false
                )

                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .setExecutor(ContextCompat.getMainExecutor(context))
                    .setTargetVideoEncodingBitRate(if (isBack) 20_000_000 else 12_000_000) // 20Mbps for 4K
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                // Histogram and edge detection image analysis
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(analysisExecutor, HistogramAnalyzer { data ->
                            _histogramData.value = data
                        })
                    }

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture,
                    imageAnalysis
                )

                _zoomRatio.value = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
                onCameraReady?.invoke()

            } catch (e: Exception) {
                Log.e(TAG, "Camera binding error: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission")
    fun startRecording(recordAudio: Boolean = true) {
        val capture = videoCapture ?: return
        if (_recordingStatus.value == RecordingStatus.RECORDING) return

        val fileName = "CINE_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
        val storageDir = File(context.getExternalFilesDir(null), "CineProRecordings")
        if (!storageDir.exists()) storageDir.mkdirs()
        val outputFile = File(storageDir, fileName)
        activeRecordingFile = outputFile

        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        _recordingStatus.value = RecordingStatus.PREPARING

        var pendingRecording = capture.output.prepareRecording(context, outputOptions)
        if (recordAudio) {
            try {
                pendingRecording = pendingRecording.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w(TAG, "Audio permission missing for recording")
            }
        }

        currentRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    _recordingStatus.value = RecordingStatus.RECORDING
                }
                is VideoRecordEvent.Status -> {
                    val durationNanos = recordEvent.recordingStats.recordedDurationNanos
                    val durationMs = durationNanos / 1_000_000L
                    val bytes = recordEvent.recordingStats.numBytesRecorded

                    val seconds = (durationMs / 1000) % 60
                    val minutes = (durationMs / (1000 * 60)) % 60
                    val hours = durationMs / (1000 * 60 * 60)
                    val formatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                    val sizeMb = bytes.toFloat() / (1024f * 1024f)
                    val formattedSize = String.format(Locale.US, "%.1f MB", sizeMb)

                    _telemetry.value = _telemetry.value.copy(
                        recordedDurationMs = durationMs,
                        recordedSizeBytes = bytes,
                        formattedDuration = formatted,
                        formattedFileSize = formattedSize,
                        estimatedBitrateMbps = if (durationMs > 1000) ((bytes * 8f) / (durationMs / 1000f)) / 1_000_000f else 20.0f
                    )
                }
                is VideoRecordEvent.Pause -> {
                    _recordingStatus.value = RecordingStatus.PAUSED
                }
                is VideoRecordEvent.Resume -> {
                    _recordingStatus.value = RecordingStatus.RECORDING
                }
                is VideoRecordEvent.Finalize -> {
                    _recordingStatus.value = RecordingStatus.IDLE
                    if (!recordEvent.hasError()) {
                        val durationMs = _telemetry.value.recordedDurationMs
                        val size = _telemetry.value.recordedSizeBytes
                        val is4k = _currentLens.value == CameraLens.REAR_MAIN
                        val w = if (is4k) 3840 else 1920
                        val h = if (is4k) 2160 else 1080

                        onVideoSavedListener?.invoke(outputFile, durationMs, size, w, h)
                        registerToMediaStore(outputFile)
                    } else {
                        Log.e(TAG, "Recording error: ${recordEvent.error}")
                        _recordingStatus.value = RecordingStatus.ERROR
                    }
                }
            }
        }
    }

    fun pauseRecording() {
        if (_recordingStatus.value == RecordingStatus.RECORDING) {
            currentRecording?.pause()
            _recordingStatus.value = RecordingStatus.PAUSED
        }
    }

    fun resumeRecording() {
        if (_recordingStatus.value == RecordingStatus.PAUSED) {
            currentRecording?.resume()
            _recordingStatus.value = RecordingStatus.RECORDING
        }
    }

    fun stopRecording() {
        if (_recordingStatus.value == RecordingStatus.RECORDING || _recordingStatus.value == RecordingStatus.PAUSED) {
            _recordingStatus.value = RecordingStatus.FINALIZING
            currentRecording?.stop()
            currentRecording = null
        }
    }

    private fun registerToMediaStore(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, file.name)
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Video.Media.DATA, file.absolutePath)
            }
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore insert warning: ${e.message}")
        }
    }

    fun release() {
        analysisExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
