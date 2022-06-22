package com.example.composeplayersample

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.composeplayersample.ui.theme.ComposePlayerSampleTheme
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.imherrera.videoplayer.AdaptiveIconButton
import com.imherrera.videoplayer.VideoPlayer
import com.imherrera.videoplayer.VideoPlayerControl
import com.imherrera.videoplayer.rememberVideoPlayerState
import okhttp3.OkHttpClient
import okhttp3.Request

val LocalActivity = staticCompositionLocalOf<ComponentActivity> { error("") }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {


            ComposePlayerSampleTheme {

                Column(modifier = Modifier.fillMaxSize()) {


                    var data by remember {
                        mutableStateOf("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                    }


                    CompositionLocalProvider(
                        LocalActivity provides this@MainActivity,
                    ) {
                        VideoPlayer(data = data)
                    }




                    TextButton(onClick = {
                        data =
                            "https://video-hw.xvideos-cdn.com/videos_new/mp4/3/9/f/xvideos.com_39f4301baa8f337af8599cab46fdb79b-1.mp4?e=1654688717&ri=1024&rs=85&h=38c64b97a7ac80b2d85734161f1c8c8e"
                    }) {
                        Text(text = "Hello", color = Color.Gray)
                    }
                    TextButton(onClick = {
                        data =
                            "https://video-hw.xvideos-cdn.com/videos_new/mp4/c/6/4/xvideos.com_c6499dec6c7eb0b78358595d37cb0e16.mp4?e=1654688799&ri=1024&rs=85&h=b716949352b014f3889e73344b0bb7bb"
                    }) {
                        Text(text = "Hello", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(data: String) {
    val playerState = rememberVideoPlayerState(
        hideControllerAfterMs = 30000,
    )

    VideoPlayer(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxWidth()
            .adaptiveSize(playerState.isFullscreen.value, LocalView.current),
        playerState = playerState,
    ) {

        /**
         * Use default control or implement your own
         * */
        VideoPlayerControl(
            state = playerState,
            title = "Elephant Dreamasjbafhvbaskdnaudyvasdquiehqiuwehiqwe",
            subtitle = "By Blender Foundation",
            isTitleMarquee = true,
            onOptionsContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AdaptiveIconButton(onClick = {

                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }
                    var isShowVideoQuality by remember {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(key1 = isShowVideoQuality, block = {
                        playerState.extendHiddenControlWindowTime()
                    })

                    AdaptiveIconButton(onClick = {
                        isShowVideoQuality = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = isShowVideoQuality,
                        onDismissRequest = {
                            isShowVideoQuality = false
                        }) {
                        repeat(10) {
                            DropdownMenuItem(onClick = {
                            }) {
                                Text(text = "Hello -$it")
                            }
                        }
                    }
                }

            },
            onPreviewOnScrollProgressContent = {
                Image(imageVector = Icons.Default.MoreVert, contentDescription = "")
            }
        )


    }


    val dataSourceFactory = remember {
        OkHttpDataSource.Factory(
            object : okhttp3.Call.Factory {
                val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                    .build()

                override fun newCall(request: Request): okhttp3.Call {
                    return okHttpClient.newCall(request)
                }
            }
        )

    }
    val defaultMediaSourceFactory = remember {
        DefaultMediaSourceFactory(dataSourceFactory)
    }

    LaunchedEffect(data) {
        playerState.player.stop()
//        playerState.player.setMediaItem(MediaItem.fromUri("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"))
//        playerState.player.setMediaItem(MediaItem.fromUri(data))
//        MediaItem.Builder()
//            .setUri(data)
//            .setDrmConfiguration(MediaItem.DrmConfiguration.Builder(UUID.randomUUID())
//                .forceSessionsForAudioAndVideoTracks(true)
//                .build())


        playerState.player.setMediaSource(
            defaultMediaSourceFactory.createMediaSource(
                MediaItem.Builder()
                    .setUri(data)
//                    .setDrmConfiguration(
//                        MediaItem.DrmConfiguration.Builder(UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"))
//                            .setLicenseUri("https://proxy.uat.widevine.com/proxy?provider=widevine_test")
//                            .setMultiSession(false)
//                            .setForceDefaultLicenseUri(false)
//                            .setLicenseRequestHeaders(mapOf())
////                            .forceSessionsForAudioAndVideoTracks(false)
//                            .build()
//                    )
                    .build()
//                MediaItem.fromUri(data)
            )
        )
        playerState.player.prepare()
        playerState.player.playWhenReady = true
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