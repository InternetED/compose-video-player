package com.imherrera.videoplayer

import android.os.Build
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner


@JvmInline
value class ResizeMode private constructor(val value: Int) {
    companion object {
        val Fit = ResizeMode(0)
        val FixedWidth = ResizeMode(1)
        val FixedHeight = ResizeMode(2)
        val Fill = ResizeMode(3)
        val Zoom = ResizeMode(4)
    }
}

private fun Modifier.adaptiveLayout(
    aspectRatio: Float,
    resizeMode: ResizeMode = ResizeMode.Fit
) = layout { measurable, constraints ->
    val resizedConstraint = constraints.resizeForVideo(resizeMode, aspectRatio)
    val placeable = measurable.measure(resizedConstraint)
    layout(constraints.maxWidth, constraints.maxHeight) {
        // Center x and y axis relative to the layout
        placeable.placeRelative(
            x = (constraints.maxWidth - resizedConstraint.maxWidth) / 2,
            y = (constraints.maxHeight - resizedConstraint.maxHeight) / 2
        )
    }
}

private fun Modifier.defaultPlayerTapGestures(playerState: VideoPlayerState, centerX: Float) =
    pointerInput(centerX) {


        detectTapGestures(
            onDoubleTap = {
                if (it.x > centerX) {
                    playerState.control.forward()
                } else {
                    playerState.control.rewind()
                }
            },
            onTap = {
                if (playerState.isControlUiVisible.value) {
                    playerState.hideControlUi()
                } else {
                    playerState.showControlUi()
                }
            }
        )
    }

@Composable
private fun VideoPlayer(
    modifier: Modifier,
    playerState: VideoPlayerState,
    controller: @Composable () -> Unit
) {
    var centerX by remember {
        mutableStateOf(0F)
    }

    var currentProcess by remember {
        mutableStateOf(-1F)
    }

    var width by remember {
        mutableStateOf(0F)
    }

    Box(
        modifier = modifier
            .onSizeChanged {
                centerX = it.width / 2F
                width = it.width.toFloat()
            }
            .defaultPlayerTapGestures(playerState, centerX)
            .draggable(
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    if (playerState.player.duration > 0L) {
                        currentProcess =
                            (playerState.player.currentPosition.toFloat() / playerState.videoDurationMs.value)
                    }
                },
                state = rememberDraggableState {
                    if (currentProcess != -1F) {
                        val fl = it / (width)

                        currentProcess =
                            (currentProcess + fl).coerceIn(0F..1F)

                        playerState.dragVideoScreen(currentProcess)
                    }
                },

                onDragStopped = {
                    if (currentProcess != -1F) {
                        playerState.dragVideoScreenFinish(currentProcess)
                        currentProcess = -1F
                    }
                }
            )
    ) {

        BackHandler(enabled = playerState.isFullscreen.value) {
            playerState.control.setFullscreen(false)
        }

        AndroidView(
            modifier = Modifier.adaptiveLayout(
                aspectRatio = playerState.videoSize.value.aspectRatio(),
                resizeMode = playerState.videoResizeMode.value
            ),
            factory = { context ->
                SurfaceView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }.also {
                    playerState.player.setVideoSurfaceView(it)
                }
            }
        )

        AnimatedVisibility(
            visible = playerState.isControlUiVisible.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            controller()
        }
    }
}

/**
 * @param playerState state to attach to this composable.
 * @param lifecycleOwner required to manage the ExoPlayer instance.
 * @param controller you can use [VideoPlayerControl] or alternatively implement your own
 * */
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    playerState: VideoPlayerState,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    controller: @Composable () -> Unit,
) {
    VideoPlayer(
        modifier = modifier,
        playerState = playerState,
        controller = controller
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> playerState.showControlUi()
                    Lifecycle.Event.ON_STOP -> playerState.player.pause()
                    else -> Unit
                }
            } else {
                when (event) {
                    Lifecycle.Event.ON_START -> playerState.showControlUi()
                    Lifecycle.Event.ON_STOP -> playerState.player.pause()
                    else -> Unit
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerState.player.release()
        }
    }
}