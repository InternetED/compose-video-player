package com.imherrera.videoplayer

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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    previewOnScrollProgressSize: DpSize = DpSize(80.dp, 45.dp),
    onPreviewOnScrollProgressContent: (@Composable BoxScope.(progress: Float) -> Unit)? = null
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(16.dp),

            ) {

            var target by remember {
                mutableStateOf<LayoutCoordinates?>(null)
            }

            if (onPreviewOnScrollProgressContent != null
                && state.videoProgressScrolling.value
                && target != null
            ) {
                val targetRect = target!!.boundsInParent()

                Box(
                    modifier = Modifier
                        .zIndex(1F)
                        .align(Alignment.BottomStart)
                        .size(previewOnScrollProgressSize)
                        .absoluteOffset {
                            val progress = state.videoProgress.value
                            val height = previewOnScrollProgressSize.height

                            IntOffset(
                                x = ((targetRect.width) * progress).toInt(),
                                y = -(height + 0.dp)
                                    .toPx()
                                    .toInt()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    onPreviewOnScrollProgressContent(state.videoProgress.value)
                }

            }


            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                val onBackPressDispatcher =
                    LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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
                    videoPlayerState = state,
                    videoDurationMs = state.videoDurationMs.value,
                    videoPositionMs = state.videoPositionMs.value,
                    onFullScreenToggle = {
                        state.control.setFullscreen(!state.isFullscreen.value)
                    },
                    onGloballyPositioned = {
                        target = it
                    }
                )
            }
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
    videoPlayerState: VideoPlayerState,
    videoDurationMs: Long,
    videoPositionMs: Long,
    onFullScreenToggle: () -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {


            Text(text = prettyVideoTimestamp(videoPositionMs.milliseconds))

            Spacer(modifier = Modifier.height(4.dp))




            Slider(
                modifier = Modifier
                    .weight(1F)
                    .height(2.dp)
                    .onGloballyPositioned(onGloballyPositioned),
                colors = SliderDefaults.colors(
                    thumbColor = progressLineColor,
                    activeTickColor = progressLineColor,
                    inactiveTickColor = Color.LightGray
                ),
                value = videoPlayerState.videoProgress.value,
                onValueChange = {
                    videoPlayerState.dragVideoScreen(it)
                },
                onValueChangeFinished = {
                    videoPlayerState.dragVideoScreenFinish(videoPlayerState.videoProgress.value)
                },
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = prettyVideoTimestamp(videoDurationMs.milliseconds))

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

private val BigIconButtonSize = 48.dp
private val SmallIconButtonSize = 32.dp


