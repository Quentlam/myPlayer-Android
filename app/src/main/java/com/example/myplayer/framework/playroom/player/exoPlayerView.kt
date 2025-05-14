package com.example.myplayer.framework.playroom.player

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource



import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text


@OptIn(UnstableApi::class)
@Composable
fun exoPlayerView(
    context: Context,
    videoUrl: String,
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
) {
    // 用于触发重新加载的状态变量，每次+1时触发播放器重载
    var reloadTrigger by remember { mutableStateOf(0) }

    Column {
        // 重新加载按钮
        Button(onClick = { reloadTrigger++ }) {
            Text("重新加载视频")
        }

        // 播放器视图容器，监听 reloadTrigger 变化重载
        PlayerViewContainer(context = context, videoUrl = videoUrl, reloadTrigger = reloadTrigger, lifecycleOwner = lifecycleOwner)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerViewContainer(
    context: Context,
    videoUrl: String,
    reloadTrigger: Int,
    lifecycleOwner: LifecycleOwner
) {
    if (videoUrl.isEmpty()) {
        Toast.makeText(context, "无效的视频地址", Toast.LENGTH_SHORT).show()
        return
    }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, /* handleAudioFocus= */true)
                volume = 1.0f
                playWhenReady = false // 先不播放，准备好后再启动
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                Log.d("PlayerState", "播放器准备就绪并开始播放")
                                Log.d("Audio", "音频会话ID: $audioSessionId")
                                Log.d("Audio", "音频格式: $audioFormat")
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                                Toast.makeText(context, "找不到视频文件", Toast.LENGTH_SHORT).show()
                            PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                                Toast.makeText(context, "播放出错：IO错误", Toast.LENGTH_SHORT).show()
                            PlaybackException.ERROR_CODE_UNSPECIFIED ->
                                Toast.makeText(context, "播放出错：未知错误", Toast.LENGTH_SHORT).show()
                            else ->
                                Toast.makeText(context, "播放出错：${error.message}", Toast.LENGTH_SHORT).show()
                        }
                        Log.e("PlayerError", "Error code: ${error.errorCode}, message: ${error.message}")
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        Log.d("Tracks", "Tracks changed: $tracks")
                        for ((groupIndex, group) in tracks.groups.withIndex()) {
                            if (group.type == C.TRACK_TYPE_AUDIO && group.length > 0) {
                                val format = group.getTrackFormat(0)
                                Log.d("TrackSelection", "音频轨道: index=$groupIndex, language=${format.language}, mime=${format.sampleMimeType}")

                                val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(0))

                                val newParams = this@apply.trackSelectionParameters
                                    .buildUpon()
                                    .setOverrideForType(override)
                                    .build()

                                this@apply.trackSelectionParameters = newParams
                                Log.d("TrackSelection", "强制选择音轨 $groupIndex")
                                break
                            }
                        }
                    }
                })
            }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            exoPlayer.release()
        }
    }

    val playerView = remember {
        PlayerView(context).apply {
            player = exoPlayer
            useController = true
            setShowMultiWindowTimeBar(true)
            controllerShowTimeoutMs = 3000
            controllerHideOnTouch = true

            setOnClickListener {
                if (!isControllerFullyVisible()) {
                    showController()
                } else {
                    hideController()
                }
            }
        }
    }

    AndroidView(factory = { playerView })

    // 监听 videoUrl 和 reloadTrigger，触发播放资源重置和播放
    LaunchedEffect(videoUrl, reloadTrigger) {
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()

            if (!isSupportedFormat(videoUrl)) {
                Toast.makeText(context, "不支持的视频格式", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }

            val dataSourceFactory = DefaultDataSource.Factory(context)
            val mediaSource = when {
                videoUrl.endsWith(".m3u8", true) -> HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUrl))
                videoUrl.endsWith(".mpd", true) -> DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUrl))
                else -> ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUrl))
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            playerView.player = exoPlayer

        } catch (e: Exception) {
            Log.e("exoPlayerView", "视频加载失败", e)
            Toast.makeText(context, "视频加载失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun isSupportedFormat(url: String): Boolean {
    val supportedFormats = listOf(
        ".mp4", ".m4a", ".m4v",
        ".mp3", ".webm", ".mkv",
        ".flv", ".wav", ".ogg",
        ".ts", ".m3u8", ".mpd"
    )
    return supportedFormats.any { url.lowercase().endsWith(it) }
}
