package com.example.composeplayersample

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.composeplayersample.ui.theme.ComposePlayerSampleTheme
import com.google.android.exoplayer2.MediaItem
import com.imherrera.videoplayer.AdaptiveIconButton
import com.imherrera.videoplayer.VideoPlayer
import com.imherrera.videoplayer.VideoPlayerControl
import com.imherrera.videoplayer.rememberVideoPlayerState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposePlayerSampleTheme {
                val playerState = rememberVideoPlayerState()

                Column(modifier = Modifier.fillMaxSize()) {
                    VideoPlayer(
                        modifier = Modifier
                            .background(Color.Black)
                            .fillMaxWidth()
                            .adaptiveSize(playerState.isFullscreen.value, LocalView.current),
                        playerState = playerState,
                    ) {
                        var isShowVideoQuality by remember {
                            mutableStateOf(false)
                        }

                        if (isShowVideoQuality) {
                            Box(
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth(0.25F)
                            ) {
                                Popup(
                                    alignment = Alignment.CenterEnd
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = Color.White
                                    ) {
                                        Column(modifier = Modifier.fillMaxHeight()) {
                                            repeat(3) {
                                                Text(text = "$it - 720P")
                                            }
                                        }
                                    }
                                }
                            }

                        }


                        /**
                         * Use default control or implement your own
                         * */
                        VideoPlayerControl(
                            state = playerState,
                            title = "Elephant Dream",
                            subtitle = "By Blender Foundation",
                            onOptionsContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    AdaptiveIconButton(onClick = {
                                        isShowVideoQuality = !isShowVideoQuality
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )
                    }

                    LaunchedEffect(Unit) {
                        playerState.player.setMediaItem(MediaItem.fromUri("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"))
                        playerState.player.prepare()
                        playerState.player.playWhenReady = true
                    }

                    repeat(10){
                        Text(text = "Hello", color = Color.Gray)
                    }
                }
            }
        }
    }
}

private fun Modifier.adaptiveSize(fullscreen: Boolean, view: View): Modifier {
    return if (fullscreen) {
        hideSystemBars(view)
        fillMaxSize()
    } else {
        showSystemBars(view)
        fillMaxWidth().height(250.dp)
    }
}


private fun hideSystemBars(view: View) {
    val windowInsetsController = ViewCompat.getWindowInsetsController(view) ?: return
    // Configure the behavior of the hidden system bars
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    // Hide both the status bar and the navigation bar
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
}

private fun showSystemBars(view: View) {
    val windowInsetsController = ViewCompat.getWindowInsetsController(view) ?: return
    // Show both the status bar and the navigation bar
    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
}