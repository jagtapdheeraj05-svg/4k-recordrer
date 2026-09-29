package com.example.editor

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class EditorPlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration.coerceAtLeast(0L)
                        } else if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startProgressPolling()
                        } else {
                            progressJob?.cancel()
                        }
                    }
                })
            }
        }
        return player!!
    }

    fun loadMedia(fileOrUri: String) {
        val p = getPlayer()
        val uri = if (fileOrUri.startsWith("content://") || fileOrUri.startsWith("file://") || fileOrUri.startsWith("http")) {
            Uri.parse(fileOrUri)
        } else {
            Uri.fromFile(File(fileOrUri))
        }
        val mediaItem = MediaItem.fromUri(uri)
        p.setMediaItem(mediaItem)
        p.prepare()
        _currentPosition.value = 0L
    }

    fun play() {
        getPlayer().play()
    }

    fun pause() {
        getPlayer().pause()
    }

    fun togglePlayPause() {
        val p = getPlayer()
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val p = getPlayer()
        p.seekTo(positionMs.coerceIn(0L, _duration.value.coerceAtLeast(1L)))
        _currentPosition.value = positionMs
    }

    fun setSpeed(speed: Float) {
        getPlayer().playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 4.0f))
    }

    fun setVolume(volume: Float) {
        getPlayer().volume = volume.coerceIn(0.0f, 2.0f)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && player != null) {
                _currentPosition.value = player?.currentPosition ?: 0L
                delay(33) // ~30fps UI update
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        player?.release()
        player = null
    }
}
