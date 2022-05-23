package com.imherrera.videoplayer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imherrera.videoplayer.icons.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VideoPlayerControl(
    state: VideoPlayerState,
    title: String,
    subtitle: String? = null,
    background: Color = Color.Black.copy(0.30f),
    contentColor: Color = Color.White,
    progressLineColor: Color = MaterialTheme.colors.primaryVariant,
    onOptionsContent: (@Composable () -> Unit)? = null,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val onBackPressDispatcher =
                LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            BackHandler(enabled = state.isFullscreen.value) {
                state.control.setFullscreen(false)
            }

            ControlHeader(
                modifier = Modifier.fillMaxWidth(),
                title = title,
                subtitle = subtitle,
                onBackClick = {
                    onBackPressDispatcher?.onBackPressed()
                },
                onOptionsContent = onOptionsContent
            )
            PlaybackControl(
                isPlaying = state.isPlaying.value,
                control = state.control
            )
            TimelineControl(
                modifier = Modifier.fillMaxWidth(),
                progressLineColor = progressLineColor,
                isFullScreen = state.isFullscreen.value,
                videoDurationMs = state.videoDurationMs.value,
                videoPositionMs = state.videoPositionMs.value,
                onProgressChange = { progress ->
                    state.player.seekTo((state.videoDurationMs.value * progress).toLong())
                },
                onFullScreenToggle = {
                    state.control.setFullscreen(!state.isFullscreen.value)
                }
            )
        }
    }
}

@Composable
private fun ControlHeader(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    onBackClick: (() -> Unit)?,
    onOptionsContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveIconButton(onClick = { onBackClick?.invoke() }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
        }

        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = title,
                color = LocalContentColor.current,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = LocalContentColor.current.copy(0.80f),
                    style = MaterialTheme.typography.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        onOptionsContent?.invoke()
    }
}

@Composable
private fun PlaybackControl(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    control: VideoPlayerControl
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            modifier = Modifier
                .size(BigIconButtonSize)
                .padding(10.dp),
            onClick = control::rewind
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Rounded.Replay10,
                contentDescription = null
            )
        }
        IconButton(
            modifier = Modifier.size(BigIconButtonSize),
            onClick = { if (isPlaying) control.pause() else control.play() }
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null
            )
        }
        IconButton(
            modifier = Modifier
                .size(BigIconButtonSize)
                .padding(10.dp),
            onClick = control::forward
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Rounded.Forward10,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun TimelineControl(
    modifier: Modifier,
    progressLineColor: Color,
    isFullScreen: Boolean,
    videoDurationMs: Long,
    videoPositionMs: Long,
    onProgressChange: (Float) -> Unit,
    onFullScreenToggle: () -> Unit,
) {


    var progress by remember(videoPositionMs.milliseconds.inWholeSeconds) {
        mutableStateOf(
            if (videoDurationMs == 0L) {
                0F
            } else {
                1.0f - ((videoDurationMs - videoPositionMs) / videoDurationMs.toFloat())
            }
        )
    }

    val timestamp = remember(videoDurationMs, videoPositionMs.milliseconds.inWholeSeconds) {
        prettyVideoTimestamp(videoPositionMs.milliseconds, videoDurationMs.milliseconds)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = timestamp)
            Spacer(modifier = Modifier.weight(1.0f))
            AdaptiveIconButton(
                modifier = Modifier.size(SmallIconButtonSize),
                onClick = onFullScreenToggle
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = null
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            colors = SliderDefaults.colors(
                thumbColor = progressLineColor,
                activeTickColor = progressLineColor,
                inactiveTickColor = Color.LightGray
            ),
            value = progress,
            onValueChange = {
                progress = it
                onProgressChange(it)
            })

    }
}


/**
 * Allow the button to be any size instead of constraining it to 48dp
 * **/
@Composable
fun AdaptiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}

private val BigIconButtonSize = 52.dp
private val SmallIconButtonSize = 32.dp


