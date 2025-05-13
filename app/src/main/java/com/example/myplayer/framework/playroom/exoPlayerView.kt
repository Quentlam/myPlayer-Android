package com.example.myplayer.framework.playroom

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.MediaSource

@Composable
fun exoPlayerView(
    context: Context,
    videoUrl: String,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    if (videoUrl.isEmpty()) {
        Toast.makeText(context, "无效的视频地址", Toast.LENGTH_SHORT).show()
        return
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
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
                    }
                })
            }
    }

    DisposableEffect(lifecycleOwner, videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(factory = { ctx ->
        PlayerView(ctx).apply {
            player = exoPlayer
            useController = true
        }
    })

    LaunchedEffect(videoUrl) {
        try {
            exoPlayer.stop()
            // 创建 DataSource.Factory
            val dataSourceFactory = DefaultDataSource.Factory(context)

            // 根据URL创建适当的MediaSource
            val mediaSource: MediaSource = when {
                videoUrl.endsWith(".m3u8", ignoreCase = true) -> {
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUrl))
                }
                videoUrl.endsWith(".mpd", ignoreCase = true) -> {
                    DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUrl))
                }
                else -> {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoUrl))
                }
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

        } catch (e: Exception) {
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
