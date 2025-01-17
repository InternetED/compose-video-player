package com.imherrera.videoplayer

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Build and remember default implementation of [VideoPlayerState]
 *
 * @param hideControllerAfterMs time after which [VideoPlayerState.isControlUiVisible] will be set to false.
 * interactions with [VideoPlayerState.control] will reset the internal counter.
 * if null is provided the controller wont be hidden until [VideoPlayerState.hideControlUi] is called again
 * @param videoPositionPollInterval interval on which the [VideoPlayerState.videoPositionMs] will be updated,
 * you can set a lower number to update the ui faster though it will consume more cpu resources.
 * Take in consideration that this value is updated only when [VideoPlayerState.isControlUiVisible] is set to true,
 * if you need to get the last value use [ExoPlayer.getCurrentPosition].
 * @param coroutineScope this scope will be used to poll for [VideoPlayerState.videoPositionMs] updates
 * @param context used to build an [ExoPlayer] instance
 * @param config you can use this to configure [ExoPlayer]
 * */
@Composable
fun rememberVideoPlayerState(
    key: Any? = null,
    hideControllerAfterMs: Long? = 3000,
    videoPositionPollInterval: Long = 500,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
    config: ExoPlayer.Builder.() -> Unit = {
        setSeekBackIncrementMs(10 * 1000)
        setSeekForwardIncrementMs(10 * 1000)
    },
): VideoPlayerState = remember(key) {
    VideoPlayerStateImpl(
        player = ExoPlayer.Builder(context)
            .setTrackSelector(DefaultTrackSelector(context))
            .setRenderersFactory(DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            })
            .setLoadControl(DefaultLoadControl.Builder().build())
            .apply(config).build(),
        coroutineScope = coroutineScope,
        hideControllerAfterMs = hideControllerAfterMs,
        videoPositionPollInterval = videoPositionPollInterval,
    ).also {
        it.player.addListener(it)
    }
}

class VideoPlayerStateImpl(
    override val player: ExoPlayer,
    private val coroutineScope: CoroutineScope,
    private val hideControllerAfterMs: Long?,
    private val videoPositionPollInterval: Long,
) : VideoPlayerState, Player.Listener {
    override val videoSize = mutableStateOf(player.videoSize)
    override val videoResizeMode = mutableStateOf(ResizeMode.Fit)
    override val videoPositionMs = mutableStateOf(0L)
    override val videoDurationMs = mutableStateOf(0L)
    override val videoProgressScrolling = mutableStateOf(false)
    override val videoProgress = mutableStateOf(0F)

    override val dragVideoScreen: (Float) -> Unit
        get() = {
            videoProgressScrolling.value = true
            if (!isControlUiVisible.value) {
                showControlUi()
            }
            this.videoProgress.value = it
            extendHiddenControlWindowTime()
        }
    override val dragVideoScreenFinish: (endDragProcess: Float) -> Unit
        get() = {
            videoProgressScrolling.value = false

            player.seekTo((player.duration * videoProgress.value).toLong())
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
        }

    override val isFullscreen = mutableStateOf(false)
    override val isPlaying = mutableStateOf(player.isPlaying)
    override val playerState = mutableStateOf(player.playbackState)

    override val isControlUiVisible = mutableStateOf(false)
    override val control = object : VideoPlayerControl {
        override fun play() {
            controlUiLastInteractionMs = 0
            val state = player.playbackState
            if (state == Player.STATE_IDLE) {
                player.prepare()
            } else if (state == Player.STATE_ENDED) {
                player.seekTo(player.currentMediaItemIndex, C.TIME_UNSET)
            }
            player.play()
        }

        override fun pause() {
            controlUiLastInteractionMs = 0
            player.pause()
        }

        override fun forward() {
            controlUiLastInteractionMs = 0
            player.seekForward()
        }

        override fun rewind() {
            controlUiLastInteractionMs = 0
            player.seekBack()
        }

        override fun setVideoResize(mode: ResizeMode) {
            controlUiLastInteractionMs = 0
            videoResizeMode.value = mode
        }

        override fun setFullscreen(value: Boolean) {
            controlUiLastInteractionMs = 0
            isFullscreen.value = value
        }
    }


    private var pollVideoPositionJob: Job? = null
    private var controlUiLastInteractionMs = 0L

    override fun hideControlUi() {
        controlUiLastInteractionMs = 0
        isControlUiVisible.value = false
        pollVideoPositionJob?.cancel()
        pollVideoPositionJob = null
    }

    override fun showControlUi() {
        isControlUiVisible.value = true
        pollVideoPositionJob?.cancel()
        pollVideoPositionJob = coroutineScope.launch {
            if (hideControllerAfterMs != null) {
                while (true) {
                    videoPositionMs.value = player.currentPosition
                    delay(videoPositionPollInterval)
                    controlUiLastInteractionMs += videoPositionPollInterval
                    if (controlUiLastInteractionMs >= hideControllerAfterMs) {
                        hideControlUi()
                        break
                    }
                }
            } else {
                while (true) {
                    videoPositionMs.value = player.currentPosition
                    delay(videoPositionPollInterval)
                }
            }
        }
    }

    override fun extendHiddenControlWindowTime() {
        controlUiLastInteractionMs = 0L
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying.value = isPlaying
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) videoDurationMs.value = player.duration
        this.playerState.value = playbackState
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        this.videoSize.value = videoSize
    }
}


interface VideoPlayerState {
    val player: ExoPlayer

    val videoSize: State<VideoSize>
    val videoResizeMode: State<ResizeMode>
    val videoPositionMs: State<Long>
    val videoDurationMs: State<Long>

    val videoProgressScrolling: State<Boolean>
    val videoProgress: State<Float>

    val dragVideoScreen: (dragProcess: Float) -> Unit
    val dragVideoScreenFinish: (endDragProcess: Float) -> Unit

    val isFullscreen: State<Boolean>
    val isPlaying: State<Boolean>
    val playerState: State<Int>

    val isControlUiVisible: State<Boolean>
    val control: VideoPlayerControl

    fun hideControlUi()
    fun showControlUi()

    fun extendHiddenControlWindowTime()
}

interface VideoPlayerControl {
    fun play()
    fun pause()

    fun forward()
    fun rewind()

    fun setFullscreen(value: Boolean)
    fun setVideoResize(mode: ResizeMode)
}